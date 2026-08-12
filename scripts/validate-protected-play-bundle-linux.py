#!/usr/bin/env python3
"""Build and verify a Play bundle from the encrypted upload-key recovery package."""

from __future__ import annotations

import argparse
import base64
import binascii
import getpass
import hashlib
import json
import os
from pathlib import Path
import re
import secrets
import shutil
import subprocess
import sys
import tempfile
from contextlib import contextmanager
from dataclasses import dataclass
from typing import Iterator, Mapping

try:
    from cryptography.exceptions import InvalidTag
    from cryptography.hazmat.primitives.ciphers.aead import AESGCM
except ImportError as exc:  # pragma: no cover - exercised only on an incomplete host
    raise SystemExit(
        "python3-cryptography is required. Install the Ubuntu package before signing."
    ) from exc


RECOVERY_FORMAT = "24seven.fm-play-upload-recovery"
RECOVERY_VERSION = 1
MINIMUM_KDF_ITERATIONS = 600_000
RECOVERY_AAD = f"{RECOVERY_FORMAT}-v{RECOVERY_VERSION}".encode()
EXPECTED_UPLOAD_CERTIFICATE_SHA256 = (
    "F6E8E81271964FFC3F8A0D548B49B4DB93AEFC48CCB74B8744512670F4279E3F"
)
REPLACEMENT_UPLOAD_ALIAS = "24seven-upload"
REPLACEMENT_UPLOAD_DNAME = (
    "CN=24Seven.FM Player Upload, OU=Mobile, O=24Seven.FM Community, C=US"
)


class SigningError(RuntimeError):
    """A safe, user-facing signing validation failure."""


@dataclass
class RecoveryPayload:
    store_bytes: bytearray
    store_sha256: str
    store_password: str
    key_alias: str
    key_password: str
    certificate_sha256: str

    def clear(self) -> None:
        self.store_bytes[:] = b"\x00" * len(self.store_bytes)
        self.store_password = ""
        self.key_alias = ""
        self.key_password = ""


def _decode_base64(value: object, field: str) -> bytes:
    if not isinstance(value, str) or not value:
        raise SigningError(f"The recovery package is missing '{field}'.")
    try:
        return base64.b64decode(value, validate=True)
    except (binascii.Error, ValueError) as exc:
        raise SigningError(f"The recovery package contains invalid '{field}' data.") from exc


def _normalize_sha256(value: object, field: str) -> str:
    if not isinstance(value, str) or not re.fullmatch(r"[0-9A-Fa-f]{64}", value):
        raise SigningError(f"The recovery package contains an invalid '{field}'.")
    return value.upper()


def decrypt_recovery_envelope(envelope: object, passphrase: str) -> bytearray:
    if not isinstance(envelope, dict):
        raise SigningError("The recovery package is not a JSON object.")
    if envelope.get("format") != RECOVERY_FORMAT or envelope.get("version") != RECOVERY_VERSION:
        raise SigningError("The file is not a supported 24Seven.FM Play upload recovery package.")
    if envelope.get("encryption") != "AES-256-GCM" or envelope.get("kdf") != "PBKDF2-HMAC-SHA256":
        raise SigningError("The recovery package uses an unsupported encryption profile.")
    try:
        iterations = int(envelope.get("iterations"))
    except (TypeError, ValueError) as exc:
        raise SigningError("The recovery package contains an invalid key-derivation cost.") from exc
    if iterations < MINIMUM_KDF_ITERATIONS:
        raise SigningError("The recovery package uses an unsafe key-derivation cost.")

    salt = _decode_base64(envelope.get("salt"), "salt")
    nonce = _decode_base64(envelope.get("nonce"), "nonce")
    tag = _decode_base64(envelope.get("tag"), "tag")
    ciphertext = _decode_base64(envelope.get("ciphertext"), "ciphertext")
    if len(salt) != 32 or len(nonce) != 12 or len(tag) != 16:
        raise SigningError("The recovery package uses invalid cryptographic parameter lengths.")

    key = bytearray(
        hashlib.pbkdf2_hmac("sha256", passphrase.encode("utf-8"), salt, iterations, dklen=32)
    )
    try:
        plaintext = AESGCM(bytes(key)).decrypt(nonce, ciphertext + tag, RECOVERY_AAD)
        return bytearray(plaintext)
    except InvalidTag as exc:
        raise SigningError(
            "Recovery package authentication failed. The passphrase is incorrect "
            "or the package was modified."
        ) from exc
    finally:
        key[:] = b"\x00" * len(key)


def _require_replacement_passphrase() -> str:
    first = getpass.getpass("New recovery passphrase: ")
    second = getpass.getpass("Confirm recovery passphrase: ")
    if len(first) < 16:
        raise SigningError("The recovery passphrase must contain at least 16 characters.")
    if not secrets.compare_digest(first, second):
        raise SigningError("The recovery passphrases do not match.")
    return first


def _encrypt_recovery_payload(payload: bytes, passphrase: str) -> dict[str, object]:
    salt = secrets.token_bytes(32)
    nonce = secrets.token_bytes(12)
    key = bytearray(
        hashlib.pbkdf2_hmac(
            "sha256", passphrase.encode("utf-8"), salt, MINIMUM_KDF_ITERATIONS, dklen=32
        )
    )
    try:
        encrypted = AESGCM(bytes(key)).encrypt(nonce, payload, RECOVERY_AAD)
        return {
            "format": RECOVERY_FORMAT,
            "version": RECOVERY_VERSION,
            "encryption": "AES-256-GCM",
            "kdf": "PBKDF2-HMAC-SHA256",
            "iterations": MINIMUM_KDF_ITERATIONS,
            "salt": base64.b64encode(salt).decode("ascii"),
            "nonce": base64.b64encode(nonce).decode("ascii"),
            "tag": base64.b64encode(encrypted[-16:]).decode("ascii"),
            "ciphertext": base64.b64encode(encrypted[:-16]).decode("ascii"),
        }
    finally:
        key[:] = b"\x00" * len(key)


def _write_new_recovery_package(package_path: Path, envelope: Mapping[str, object]) -> None:
    if package_path.exists():
        raise SigningError("Refusing to replace an existing encrypted recovery package.")
    package_path.parent.mkdir(parents=True, exist_ok=True)
    temporary_path: Path | None = None
    try:
        with tempfile.NamedTemporaryFile(
            mode="x", encoding="utf-8", dir=package_path.parent, prefix=".24seven-recovery-", delete=False
        ) as temporary:
            temporary_path = Path(temporary.name)
            temporary_path.chmod(0o600)
            temporary.write(json.dumps(envelope, separators=(",", ":")))
            temporary.write("\n")
            temporary.flush()
            os.fsync(temporary.fileno())
        os.replace(temporary_path, package_path)
        temporary_path = None
    finally:
        if temporary_path is not None:
            temporary_path.unlink(missing_ok=True)


def create_replacement_recovery_package(repository_root: Path, package_path: Path) -> str:
    """Create and independently verify a new upload identity held only in encrypted recovery."""
    _assert_external_package(package_path, repository_root)
    if package_path.exists():
        raise SigningError("Refusing to replace an existing encrypted recovery package.")
    if not Path("/dev/shm").is_dir():
        raise SigningError("Memory-backed /dev/shm is required to create a replacement upload identity.")

    passphrase = _require_replacement_passphrase()
    payload: RecoveryPayload | None = None
    try:
        with tempfile.TemporaryDirectory(prefix="24seven-replacement-upload-", dir="/dev/shm") as directory:
            keystore_path = Path(directory) / "24seven-upload.p12"
            password = secrets.token_urlsafe(48)
            environment = os.environ.copy()
            environment["TWENTYFOURSEVEN_REPLACEMENT_KEY_PASSWORD"] = password
            result = subprocess.run(
                [
                    shutil.which("keytool") or "keytool",
                    "-genkeypair",
                    "-keystore",
                    str(keystore_path),
                    "-storetype",
                    "PKCS12",
                    "-alias",
                    REPLACEMENT_UPLOAD_ALIAS,
                    "-keyalg",
                    "RSA",
                    "-keysize",
                    "4096",
                    "-validity",
                    "10000",
                    "-dname",
                    REPLACEMENT_UPLOAD_DNAME,
                    "-storepass:env",
                    "TWENTYFOURSEVEN_REPLACEMENT_KEY_PASSWORD",
                    "-keypass:env",
                    "TWENTYFOURSEVEN_REPLACEMENT_KEY_PASSWORD",
                    "-noprompt",
                ],
                env=environment,
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL,
                check=False,
            )
            if result.returncode != 0 or not keystore_path.is_file():
                raise SigningError("keytool could not create the replacement Play upload identity.")

            store_bytes = bytearray(keystore_path.read_bytes())
            temporary_payload = RecoveryPayload(
                store_bytes=store_bytes,
                store_sha256=hashlib.sha256(store_bytes).hexdigest().upper(),
                store_password=password,
                key_alias=REPLACEMENT_UPLOAD_ALIAS,
                key_password=password,
                certificate_sha256="",
            )
            try:
                temporary_payload.certificate_sha256 = _keystore_certificate_sha256(
                    keystore_path, temporary_payload.store_password, temporary_payload.key_alias
                )
                plaintext = bytearray(
                    json.dumps(
                        {
                            "version": 1,
                            "storeFileName": keystore_path.name,
                            "storeBytes": base64.b64encode(store_bytes).decode("ascii"),
                            "storeSha256": temporary_payload.store_sha256,
                            "storePassword": password,
                            "keyAlias": REPLACEMENT_UPLOAD_ALIAS,
                            "keyPassword": password,
                            "certificateSha256": temporary_payload.certificate_sha256,
                        },
                        separators=(",", ":"),
                    ).encode("utf-8")
                )
                try:
                    _write_new_recovery_package(
                        package_path, _encrypt_recovery_payload(plaintext, passphrase)
                    )
                finally:
                    plaintext[:] = b"\x00" * len(plaintext)
            finally:
                temporary_payload.clear()

        payload = load_recovery_package(package_path, passphrase, temporary_payload.certificate_sha256)
        with materialized_keystore(payload) as restored_keystore:
            certificate_sha256 = verify_keystore_certificate(restored_keystore, payload)
        if certificate_sha256 != temporary_payload.certificate_sha256:
            raise SigningError("The replacement recovery package did not verify its upload certificate.")
        return certificate_sha256
    except Exception:
        package_path.unlink(missing_ok=True)
        raise
    finally:
        passphrase = ""
        if payload is not None:
            payload.clear()


def parse_recovery_payload(
    plaintext: bytearray, expected_certificate_sha256: str
) -> RecoveryPayload:
    try:
        value = json.loads(plaintext.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise SigningError("The authenticated recovery payload is not valid JSON.") from exc
    if not isinstance(value, dict):
        raise SigningError("The authenticated recovery payload is not a JSON object.")

    required_text = ("storePassword", "keyAlias", "keyPassword")
    for field in required_text:
        if not isinstance(value.get(field), str) or not value[field]:
            raise SigningError(f"The recovery payload is missing '{field}'.")

    store_bytes = bytearray(_decode_base64(value.get("storeBytes"), "storeBytes"))
    store_sha256 = _normalize_sha256(value.get("storeSha256"), "storeSha256")
    certificate_sha256 = _normalize_sha256(value.get("certificateSha256"), "certificateSha256")
    actual_store_sha256 = hashlib.sha256(store_bytes).hexdigest().upper()
    if actual_store_sha256 != store_sha256:
        store_bytes[:] = b"\x00" * len(store_bytes)
        raise SigningError("The recovered keystore hash does not match the authenticated payload.")
    if certificate_sha256 != expected_certificate_sha256.upper():
        store_bytes[:] = b"\x00" * len(store_bytes)
        raise SigningError(
            "The recovery package does not contain the registered Play upload identity."
        )

    return RecoveryPayload(
        store_bytes=store_bytes,
        store_sha256=store_sha256,
        store_password=value["storePassword"],
        key_alias=value["keyAlias"],
        key_password=value["keyPassword"],
        certificate_sha256=certificate_sha256,
    )


def load_recovery_package(
    package_path: Path,
    passphrase: str,
    expected_certificate_sha256: str | None = EXPECTED_UPLOAD_CERTIFICATE_SHA256,
) -> RecoveryPayload:
    try:
        envelope = json.loads(package_path.read_text(encoding="utf-8"))
    except FileNotFoundError as exc:
        raise SigningError("The encrypted recovery package does not exist.") from exc
    except (OSError, UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise SigningError("The encrypted recovery package could not be read as JSON.") from exc

    plaintext = decrypt_recovery_envelope(envelope, passphrase)
    try:
        if expected_certificate_sha256 is None:
            return _parse_recovery_payload_without_registered_identity(plaintext)
        return parse_recovery_payload(plaintext, expected_certificate_sha256)
    finally:
        plaintext[:] = b"\x00" * len(plaintext)


def _parse_recovery_payload_without_registered_identity(plaintext: bytearray) -> RecoveryPayload:
    """Read a self-authenticated first-upload package before Play has an upload identity."""
    try:
        value = json.loads(plaintext.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise SigningError("The authenticated recovery payload is not valid JSON.") from exc
    if not isinstance(value, dict):
        raise SigningError("The authenticated recovery payload is not a JSON object.")
    certificate_sha256 = _normalize_sha256(value.get("certificateSha256"), "certificateSha256")
    return parse_recovery_payload(plaintext, certificate_sha256)


def _certificate_sha256_from_pem(pem: str) -> str:
    match = re.search(
        r"-----BEGIN CERTIFICATE-----\s*(?P<body>[A-Za-z0-9+/=\s]+?)\s*-----END CERTIFICATE-----",
        pem,
        re.DOTALL,
    )
    if not match:
        raise SigningError("Unable to read a signer certificate from keytool output.")
    try:
        certificate = base64.b64decode(re.sub(r"\s", "", match.group("body")), validate=True)
    except (binascii.Error, ValueError) as exc:
        raise SigningError("The signer certificate contains invalid PEM data.") from exc
    return hashlib.sha256(certificate).hexdigest().upper()


def _run_captured(command: list[str], environment: dict[str, str] | None = None) -> str:
    result = subprocess.run(
        command,
        env=environment,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        check=False,
    )
    if result.returncode != 0:
        raise SigningError(f"{Path(command[0]).name} failed without exposing its captured output.")
    return result.stdout


def _keystore_certificate_sha256(keystore_path: Path, store_password: str, key_alias: str) -> str:
    environment = os.environ.copy()
    environment["TWENTYFOURSEVEN_RECOVERY_STORE_PASSWORD"] = store_password
    pem = _run_captured(
        [
            shutil.which("keytool") or "keytool",
            "-exportcert",
            "-rfc",
            "-keystore",
            str(keystore_path),
            "-storetype",
            "PKCS12",
            "-alias",
            key_alias,
            "-storepass:env",
            "TWENTYFOURSEVEN_RECOVERY_STORE_PASSWORD",
        ],
        environment,
    )
    return _certificate_sha256_from_pem(pem)


def verify_keystore_certificate(keystore_path: Path, payload: RecoveryPayload) -> str:
    certificate_sha256 = _keystore_certificate_sha256(
        keystore_path, payload.store_password, payload.key_alias
    )
    if certificate_sha256 != payload.certificate_sha256:
        raise SigningError(
            "The recovered keystore certificate does not match the authenticated payload."
        )
    return certificate_sha256


@contextmanager
def materialized_keystore(
    payload: RecoveryPayload, memory_root: Path = Path("/dev/shm")
) -> Iterator[Path]:
    if not memory_root.is_dir() or not os.access(memory_root, os.W_OK | os.X_OK):
        raise SigningError(
            "Memory-backed /dev/shm storage is required for protected Linux signing."
        )
    previous_umask = os.umask(0o077)
    temporary_directory: Path | None = None
    try:
        temporary_directory = Path(
            tempfile.mkdtemp(prefix="24seven-play-signing-", dir=memory_root)
        )
        keystore_path = temporary_directory / "upload.p12"
        keystore_path.write_bytes(payload.store_bytes)
        keystore_path.chmod(0o600)
        yield keystore_path
    finally:
        os.umask(previous_umask)
        if temporary_directory is not None:
            shutil.rmtree(temporary_directory, ignore_errors=True)


def _assert_external_package(package_path: Path, repository_root: Path) -> None:
    lexical_package = package_path.expanduser().absolute()
    resolved_package = package_path.expanduser().resolve(strict=False)
    resolved_repository = repository_root.resolve()
    if (
        lexical_package == resolved_repository
        or resolved_repository in lexical_package.parents
        or resolved_package == resolved_repository
        or resolved_repository in resolved_package.parents
    ):
        raise SigningError("The encrypted recovery package must remain outside the repository.")


def resolve_android_sdk(
    environment: Mapping[str, str] | None = None,
    home: Path | None = None,
) -> Path:
    values = environment if environment is not None else os.environ
    home_directory = home if home is not None else Path.home()
    candidates = [
        values.get("ANDROID_HOME"),
        values.get("ANDROID_SDK_ROOT"),
        str(home_directory / "Android/Sdk"),
    ]
    for candidate in candidates:
        if not candidate:
            continue
        sdk = Path(candidate).expanduser()
        if (
            (sdk / "platforms/android-36/android.jar").is_file()
            and (sdk / "build-tools/36.1.0").is_dir()
        ):
            return sdk.resolve()
    raise SigningError(
        "Android SDK Platform 36 and Build Tools 36.1.0 could not be located."
    )


def check_environment(repository_root: Path) -> Path:
    if sys.platform != "linux":
        raise SigningError("This protected signing helper supports Linux only.")
    if not (repository_root / "gradlew").is_file():
        raise SigningError("Run the helper from the 24Seven.FM Player repository checkout.")
    if not Path("/dev/shm").is_dir():
        raise SigningError("Memory-backed /dev/shm storage is unavailable.")
    for command in ("findmnt", "java", "keytool", "jarsigner"):
        if shutil.which(command) is None:
            raise SigningError(f"Required command '{command}' is unavailable.")
    memory_filesystem = _run_captured(["findmnt", "-n", "-o", "FSTYPE", "/dev/shm"]).strip()
    if memory_filesystem != "tmpfs":
        raise SigningError("Protected signing requires /dev/shm to be mounted as tmpfs.")
    return resolve_android_sdk()


def _build_signed_artifacts(
    repository_root: Path,
    android_sdk: Path,
    keystore_path: Path,
    payload: RecoveryPayload,
    build_apk: bool,
) -> None:
    environment = os.environ.copy()
    environment.update(
        {
            "ANDROID_HOME": str(android_sdk),
            "ANDROID_SDK_ROOT": str(android_sdk),
            "TWENTYFOURSEVEN_UPLOAD_STORE_FILE": str(keystore_path),
            "TWENTYFOURSEVEN_UPLOAD_STORE_PASSWORD": payload.store_password,
            "TWENTYFOURSEVEN_UPLOAD_KEY_ALIAS": payload.key_alias,
            "TWENTYFOURSEVEN_UPLOAD_KEY_PASSWORD": payload.key_password,
        }
    )
    tasks = [":app:bundleRelease"]
    if build_apk:
        tasks.append(":app:assembleRelease")
    result = subprocess.run(
        [str(repository_root / "gradlew"), *tasks, "--no-daemon", "--console=plain"],
        cwd=repository_root,
        env=environment,
        check=False,
    )
    if result.returncode != 0:
        raise SigningError(f"Protected release build failed with exit code {result.returncode}.")


def _verify_signed_bundle(
    repository_root: Path, expected_certificate_sha256: str
) -> tuple[str, str]:
    bundle = repository_root / "app/build/outputs/bundle/release/app-release.aab"
    if not bundle.is_file():
        raise SigningError("The release bundle was not produced at the expected path.")
    _verify_jar_signature(bundle)
    certificate_pem = _run_captured(
        [shutil.which("keytool") or "keytool", "-printcert", "-rfc", "-jarfile", str(bundle)]
    )
    certificate_sha256 = _certificate_sha256_from_pem(certificate_pem)
    if certificate_sha256 != expected_certificate_sha256.upper():
        raise SigningError(
            "The release bundle signer does not match the registered Play upload certificate."
        )
    return hashlib.sha256(bundle.read_bytes()).hexdigest().upper(), certificate_sha256


def _verify_jar_signature(bundle: Path) -> str:
    # Play upload certificates are normally self-signed. Avoid jarsigner's strict
    # public-trust-chain check and pin the exact expected certificate separately.
    verification = _run_captured([
        shutil.which("jarsigner") or "jarsigner",
        "-verify",
        "-verbose",
        "-certs",
        str(bundle),
    ])
    if not re.search(r"(?im)^\s*jar verified\.?\s*$", verification):
        raise SigningError("The release bundle does not contain a verified JAR signature.")
    return verification


def parse_arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Build and verify a signed Play bundle without persisting plaintext signing material."
        )
    )
    parser.add_argument(
        "--recovery-package",
        type=Path,
        help="Encrypted recovery package outside the repository",
    )
    parser.add_argument(
        "--build-apk", action="store_true", help="Also build the signed release APK"
    )
    parser.add_argument(
        "--check-environment",
        action="store_true",
        help="Check Linux prerequisites without reading signing material",
    )
    parser.add_argument(
        "--create-replacement-recovery-package",
        type=Path,
        help=(
            "Create and verify a replacement upload identity as an encrypted recovery package "
            "outside the repository"
        ),
    )
    parser.add_argument(
        "--accept-first-upload-identity",
        action="store_true",
        help=(
            "Accept and print the self-authenticated certificate from a replacement package "
            "only for Play's first bundle upload"
        ),
    )
    return parser.parse_args()


def main() -> int:
    arguments = parse_arguments()
    repository_root = Path(__file__).resolve().parent.parent
    try:
        android_sdk = check_environment(repository_root)
        if arguments.check_environment:
            print("Protected Linux signing environment is ready.")
            return 0
        if arguments.create_replacement_recovery_package is not None:
            certificate_sha256 = create_replacement_recovery_package(
                repository_root, arguments.create_replacement_recovery_package.expanduser()
            )
            print("Replacement upload identity created and recovery package independently verified.")
            print(f"Upload certificate SHA-256: {certificate_sha256}")
            return 0
        if arguments.recovery_package is None:
            raise SigningError(
                "--recovery-package is required unless --check-environment or "
                "--create-replacement-recovery-package is used."
            )
        _assert_external_package(arguments.recovery_package, repository_root)
        if arguments.accept_first_upload_identity and arguments.recovery_package.name != (
            "24seven-upload-replacement-20260811.24seven-recovery"
        ):
            raise SigningError(
                "First-upload identity acceptance is restricted to the approved replacement recovery package."
            )
        passphrase = getpass.getpass("Recovery passphrase: ")
        payload: RecoveryPayload | None = None
        try:
            payload = load_recovery_package(
                arguments.recovery_package.expanduser(),
                passphrase,
                None if arguments.accept_first_upload_identity else EXPECTED_UPLOAD_CERTIFICATE_SHA256,
            )
            passphrase = ""
            with materialized_keystore(payload) as keystore_path:
                certificate_sha256 = verify_keystore_certificate(keystore_path, payload)
                _build_signed_artifacts(
                    repository_root,
                    android_sdk,
                    keystore_path,
                    payload,
                    arguments.build_apk,
                )
            bundle_sha256, bundle_certificate_sha256 = _verify_signed_bundle(
                repository_root, certificate_sha256
            )
        finally:
            passphrase = ""
            if payload is not None:
                payload.clear()
        print("Play bundle verified from memory-backed Linux signing material.")
        print(f"SHA-256: {bundle_sha256}")
        print(f"Upload certificate SHA-256: {bundle_certificate_sha256}")
        return 0
    except SigningError as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
