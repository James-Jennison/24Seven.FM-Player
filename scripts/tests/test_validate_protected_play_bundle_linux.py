from __future__ import annotations

import base64
import hashlib
import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
import zipfile

from cryptography.hazmat.primitives.ciphers.aead import AESGCM


SCRIPT = Path(__file__).resolve().parents[1] / "validate-protected-play-bundle-linux.py"
SPEC = importlib.util.spec_from_file_location("linux_play_signing", SCRIPT)
assert SPEC and SPEC.loader
signing = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = signing
SPEC.loader.exec_module(signing)


class LinuxPlaySigningTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.temporary = tempfile.TemporaryDirectory(prefix="24seven-signing-test-")
        cls.root = Path(cls.temporary.name)
        cls.keystore = cls.root / "synthetic-upload.p12"
        cls.password = "synthetic-test-password"
        cls.alias = "synthetic-upload"
        subprocess.run(
            [
                "keytool",
                "-genkeypair",
                "-storetype",
                "PKCS12",
                "-keystore",
                str(cls.keystore),
                "-storepass",
                cls.password,
                "-keypass",
                cls.password,
                "-alias",
                cls.alias,
                "-keyalg",
                "RSA",
                "-keysize",
                "2048",
                "-validity",
                "1",
                "-dname",
                "CN=Synthetic Test",
            ],
            check=True,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )
        pem = subprocess.run(
            [
                "keytool",
                "-exportcert",
                "-rfc",
                "-keystore",
                str(cls.keystore),
                "-storepass",
                cls.password,
                "-alias",
                cls.alias,
            ],
            check=True,
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.DEVNULL,
        ).stdout
        cls.certificate_sha256 = signing._certificate_sha256_from_pem(pem)
        cls.passphrase = "synthetic recovery passphrase"

    @classmethod
    def tearDownClass(cls) -> None:
        cls.temporary.cleanup()

    def create_package(self, *, store_sha256: str | None = None) -> Path:
        store_bytes = self.keystore.read_bytes()
        payload = {
            "version": 1,
            "storeBytes": base64.b64encode(store_bytes).decode(),
            "storeSha256": store_sha256 or hashlib.sha256(store_bytes).hexdigest().upper(),
            "storePassword": self.password,
            "keyAlias": self.alias,
            "keyPassword": self.password,
            "certificateSha256": self.certificate_sha256,
        }
        plaintext = json.dumps(payload, separators=(",", ":")).encode()
        salt = bytes(range(32))
        nonce = bytes(range(12))
        key = hashlib.pbkdf2_hmac(
            "sha256", self.passphrase.encode(), salt, signing.MINIMUM_KDF_ITERATIONS, dklen=32
        )
        encrypted = AESGCM(key).encrypt(nonce, plaintext, signing.RECOVERY_AAD)
        envelope = {
            "format": signing.RECOVERY_FORMAT,
            "version": signing.RECOVERY_VERSION,
            "encryption": "AES-256-GCM",
            "kdf": "PBKDF2-HMAC-SHA256",
            "iterations": signing.MINIMUM_KDF_ITERATIONS,
            "salt": base64.b64encode(salt).decode(),
            "nonce": base64.b64encode(nonce).decode(),
            "tag": base64.b64encode(encrypted[-16:]).decode(),
            "ciphertext": base64.b64encode(encrypted[:-16]).decode(),
        }
        package = self.root / f"recovery-{len(list(self.root.glob('recovery-*')))}.json"
        package.write_text(json.dumps(envelope), encoding="utf-8")
        return package

    def test_authenticated_package_materializes_and_matches_its_certificate(self) -> None:
        payload = signing.load_recovery_package(
            self.create_package(), self.passphrase, self.certificate_sha256
        )
        try:
            with signing.materialized_keystore(payload, self.root) as keystore:
                self.assertEqual(0o600, keystore.stat().st_mode & 0o777)
                self.assertEqual(
                    self.certificate_sha256,
                    signing.verify_keystore_certificate(keystore, payload),
                )
            self.assertFalse(any(self.root.glob("24seven-play-signing-*")))
        finally:
            payload.clear()

    def test_wrong_passphrase_is_rejected(self) -> None:
        with self.assertRaisesRegex(signing.SigningError, "authentication failed"):
            signing.load_recovery_package(
                self.create_package(), "wrong passphrase", self.certificate_sha256
            )

    def test_wrong_registered_certificate_is_rejected(self) -> None:
        with self.assertRaisesRegex(signing.SigningError, "registered Play upload identity"):
            signing.load_recovery_package(self.create_package(), self.passphrase, "0" * 64)

    def test_mismatched_keystore_hash_is_rejected(self) -> None:
        with self.assertRaisesRegex(signing.SigningError, "keystore hash"):
            signing.load_recovery_package(
                self.create_package(store_sha256="0" * 64),
                self.passphrase,
                self.certificate_sha256,
            )

    def test_repository_local_recovery_package_is_rejected(self) -> None:
        repository = self.root / "repository"
        repository.mkdir()
        with self.assertRaisesRegex(signing.SigningError, "outside the repository"):
            signing._assert_external_package(repository / "upload.24seven-recovery", repository)

    def test_android_sdk_falls_back_to_the_standard_home_location(self) -> None:
        home = self.root / "synthetic-home"
        sdk = home / "Android/Sdk"
        platform = sdk / "platforms/android-36"
        build_tools = sdk / "build-tools/36.1.0"
        platform.mkdir(parents=True)
        build_tools.mkdir(parents=True)
        (platform / "android.jar").touch()

        self.assertEqual(
            sdk.resolve(),
            signing.resolve_android_sdk(environment={}, home=home),
        )

    def test_incomplete_android_sdk_is_rejected(self) -> None:
        with self.assertRaisesRegex(signing.SigningError, "could not be located"):
            signing.resolve_android_sdk(environment={}, home=self.root / "missing-home")

    def test_self_signed_upload_certificate_passes_jar_verification(self) -> None:
        bundle = self.root / "self-signed-test.aab"
        with zipfile.ZipFile(bundle, "w") as archive:
            archive.writestr("base/manifest/AndroidManifest.xml", "synthetic")
        subprocess.run(
            [
                "jarsigner",
                "-keystore",
                str(self.keystore),
                "-storepass",
                self.password,
                "-keypass",
                self.password,
                "-storetype",
                "PKCS12",
                str(bundle),
                self.alias,
            ],
            check=True,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )

        verification = signing._verify_jar_signature(bundle)

        self.assertRegex(verification, r"(?im)^\s*jar verified\.?\s*$")


if __name__ == "__main__":
    unittest.main()
