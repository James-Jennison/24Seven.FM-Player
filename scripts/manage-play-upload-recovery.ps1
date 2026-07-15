<#
.SYNOPSIS
Exports, verifies, or restores an authenticated off-PC Play upload-key recovery package.

.EXAMPLE
pwsh.exe -NoProfile -File .\scripts\manage-play-upload-recovery.ps1 -Action Export -PackagePath E:\Backups\24seven-upload.24seven-recovery

.EXAMPLE
pwsh.exe -NoProfile -File .\scripts\manage-play-upload-recovery.ps1 -Action Verify -PackagePath E:\Backups\24seven-upload.24seven-recovery

.EXAMPLE
pwsh.exe -NoProfile -File .\scripts\manage-play-upload-recovery.ps1 -Action Restore -PackagePath E:\Backups\24seven-upload.24seven-recovery
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [ValidateSet('Export', 'Verify', 'Restore')]
    [string]$Action,

    [Parameter(Mandatory)]
    [string]$PackagePath,

    [Security.SecureString]$Passphrase,

    [string]$SecretPath = (Join-Path ([Environment]::GetFolderPath('UserProfile')) '.codex\secrets\24seven-play-upload.dpapi'),
    [string]$TargetKeystorePath = (Join-Path ([Environment]::GetFolderPath('UserProfile')) '.android\24seven-fm-player-upload.jks'),
    [string]$TargetSecretPath = (Join-Path ([Environment]::GetFolderPath('UserProfile')) '.codex\secrets\24seven-play-upload.dpapi')
)

$ErrorActionPreference = 'Stop'
$recoveryFormat = '24seven.fm-play-upload-recovery'
$recoveryVersion = 1
$kdfIterations = 600000
$aadBytes = [Text.Encoding]::UTF8.GetBytes("$recoveryFormat-v$recoveryVersion")

if ([Environment]::OSVersion.Platform -ne [PlatformID]::Win32NT) {
    throw 'Play upload recovery requires Windows for DPAPI restore support.'
}
if ($PSVersionTable.PSVersion.Major -lt 7) {
    throw 'Play upload recovery requires PowerShell 7 or newer for AES-GCM. Run this script with pwsh.exe.'
}
Add-Type -AssemblyName System.Security

function Clear-Bytes {
    param([byte[]]$Value)
    if ($Value) { [Array]::Clear($Value, 0, $Value.Length) }
}

function ConvertFrom-RecoverySecureString {
    param([Parameter(Mandatory)][Security.SecureString]$Value)

    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($Value)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

function Resolve-RecoveryPassphrase {
    param(
        [Security.SecureString]$Supplied,
        [bool]$Confirm
    )

    if ($Supplied) {
        if ($Confirm) {
            $suppliedPlain = ConvertFrom-RecoverySecureString -Value $Supplied
            try {
                if ($suppliedPlain.Length -lt 16) {
                    throw 'The recovery passphrase must contain at least 16 characters.'
                }
            } finally {
                $suppliedPlain = $null
            }
        }
        return $Supplied
    }

    $first = Read-Host 'Recovery passphrase' -AsSecureString
    if (-not $Confirm) { return $first }

    $second = Read-Host 'Confirm recovery passphrase' -AsSecureString
    $firstPlain = ConvertFrom-RecoverySecureString -Value $first
    $secondPlain = ConvertFrom-RecoverySecureString -Value $second
    try {
        if ($firstPlain.Length -lt 16) {
            throw 'The recovery passphrase must contain at least 16 characters.'
        }
        if ($firstPlain -cne $secondPlain) {
            throw 'The recovery passphrases do not match.'
        }
    } finally {
        $firstPlain = $null
        $secondPlain = $null
    }
    return $first
}

function Get-Sha256Hex {
    param([Parameter(Mandatory)][byte[]]$Value)

    $sha256 = [Security.Cryptography.SHA256]::Create()
    try {
        return ([BitConverter]::ToString($sha256.ComputeHash($Value))).Replace('-', '')
    } finally {
        $sha256.Dispose()
    }
}

function Get-CertificateSha256FromPem {
    param([Parameter(Mandatory)][string]$Pem)

    $match = [regex]::Match(
        $Pem,
        '-----BEGIN CERTIFICATE-----\s*(?<body>[A-Za-z0-9+/=\s]+?)\s*-----END CERTIFICATE-----',
        [Text.RegularExpressions.RegexOptions]::Singleline
    )
    if (-not $match.Success) {
        throw 'Unable to read a certificate from keytool output.'
    }
    $certificateBytes = [Convert]::FromBase64String(($match.Groups['body'].Value -replace '\s', ''))
    try {
        return Get-Sha256Hex -Value $certificateBytes
    } finally {
        Clear-Bytes -Value $certificateBytes
    }
}

function Get-KeystoreCertificateSha256 {
    param(
        [Parameter(Mandatory)][byte[]]$KeystoreBytes,
        [Parameter(Mandatory)][string]$StorePassword,
        [Parameter(Mandatory)][string]$KeyAlias
    )

    $temporaryKeystore = Join-Path ([IO.Path]::GetTempPath()) ("24seven-recovery-{0}.p12" -f [Guid]::NewGuid().ToString('N'))
    $previousPassword = [Environment]::GetEnvironmentVariable('TWENTYFOURSEVEN_RECOVERY_STORE_PASSWORD', 'Process')
    try {
        [IO.File]::WriteAllBytes($temporaryKeystore, $KeystoreBytes)
        $env:TWENTYFOURSEVEN_RECOVERY_STORE_PASSWORD = $StorePassword
        $keytool = (Get-Command keytool.exe -ErrorAction Stop).Source
        $certificatePem = (& $keytool -exportcert -rfc `
            -keystore $temporaryKeystore `
            -storetype PKCS12 `
            -alias $KeyAlias `
            -storepass:env TWENTYFOURSEVEN_RECOVERY_STORE_PASSWORD 2>&1 | Out-String)
        if ($LASTEXITCODE -ne 0) {
            throw 'Unable to verify the keystore contained in the recovery data.'
        }
        return Get-CertificateSha256FromPem -Pem $certificatePem
    } finally {
        [Environment]::SetEnvironmentVariable('TWENTYFOURSEVEN_RECOVERY_STORE_PASSWORD', $previousPassword, 'Process')
        if (Test-Path -LiteralPath $temporaryKeystore -PathType Leaf) {
            Remove-Item -LiteralPath $temporaryKeystore -Force
        }
    }
}

function Protect-RecoveryPayload {
    param(
        [Parameter(Mandatory)][byte[]]$PlainBytes,
        [Parameter(Mandatory)][Security.SecureString]$SecurePassphrase
    )

    $salt = [Security.Cryptography.RandomNumberGenerator]::GetBytes(32)
    $nonce = [Security.Cryptography.RandomNumberGenerator]::GetBytes(12)
    $ciphertext = [byte[]]::new($PlainBytes.Length)
    $tag = [byte[]]::new(16)
    $key = $null
    $plainPassphrase = ConvertFrom-RecoverySecureString -Value $SecurePassphrase
    try {
        $deriver = [Security.Cryptography.Rfc2898DeriveBytes]::new(
            $plainPassphrase,
            $salt,
            $kdfIterations,
            [Security.Cryptography.HashAlgorithmName]::SHA256
        )
        try { $key = $deriver.GetBytes(32) } finally { $deriver.Dispose() }
        $aes = [Security.Cryptography.AesGcm]::new($key, 16)
        try { $aes.Encrypt($nonce, $PlainBytes, $ciphertext, $tag, $aadBytes) } finally { $aes.Dispose() }

        return [ordered]@{
            format = $recoveryFormat
            version = $recoveryVersion
            createdAtUtc = [DateTimeOffset]::UtcNow.ToString('o')
            encryption = 'AES-256-GCM'
            kdf = 'PBKDF2-HMAC-SHA256'
            iterations = $kdfIterations
            salt = [Convert]::ToBase64String($salt)
            nonce = [Convert]::ToBase64String($nonce)
            tag = [Convert]::ToBase64String($tag)
            ciphertext = [Convert]::ToBase64String($ciphertext)
        }
    } finally {
        $plainPassphrase = $null
        Clear-Bytes -Value $key
        Clear-Bytes -Value $salt
        Clear-Bytes -Value $nonce
        Clear-Bytes -Value $tag
        Clear-Bytes -Value $ciphertext
    }
}

function Unprotect-RecoveryPayload {
    param(
        [Parameter(Mandatory)]$Envelope,
        [Parameter(Mandatory)][Security.SecureString]$SecurePassphrase
    )

    if ($Envelope.format -ne $recoveryFormat -or [int]$Envelope.version -ne $recoveryVersion) {
        throw 'The file is not a supported 24Seven.FM Play upload recovery package.'
    }
    if ($Envelope.encryption -ne 'AES-256-GCM' -or $Envelope.kdf -ne 'PBKDF2-HMAC-SHA256') {
        throw 'The recovery package uses an unsupported encryption profile.'
    }

    $iterations = [int]$Envelope.iterations
    if ($iterations -lt $kdfIterations) {
        throw 'The recovery package uses an unsafe key-derivation cost.'
    }
    $salt = [Convert]::FromBase64String($Envelope.salt)
    $nonce = [Convert]::FromBase64String($Envelope.nonce)
    $tag = [Convert]::FromBase64String($Envelope.tag)
    $ciphertext = [Convert]::FromBase64String($Envelope.ciphertext)
    $plaintext = [byte[]]::new($ciphertext.Length)
    $key = $null
    $plainPassphrase = ConvertFrom-RecoverySecureString -Value $SecurePassphrase
    try {
        $deriver = [Security.Cryptography.Rfc2898DeriveBytes]::new(
            $plainPassphrase,
            $salt,
            $iterations,
            [Security.Cryptography.HashAlgorithmName]::SHA256
        )
        try { $key = $deriver.GetBytes(32) } finally { $deriver.Dispose() }
        $aes = [Security.Cryptography.AesGcm]::new($key, 16)
        try {
            $aes.Decrypt($nonce, $ciphertext, $tag, $plaintext, $aadBytes)
        } catch [Security.Cryptography.AuthenticationTagMismatchException] {
            throw 'Recovery package authentication failed. The passphrase is incorrect or the package was modified.'
        } finally {
            $aes.Dispose()
        }
        return $plaintext
    } catch {
        Clear-Bytes -Value $plaintext
        throw
    } finally {
        $plainPassphrase = $null
        Clear-Bytes -Value $key
        Clear-Bytes -Value $salt
        Clear-Bytes -Value $nonce
        Clear-Bytes -Value $tag
        Clear-Bytes -Value $ciphertext
    }
}

function Assert-OutsideRepository {
    param(
        [Parameter(Mandatory)][string]$Candidate,
        [Parameter(Mandatory)][string]$RepositoryRoot
    )

    $prefix = $RepositoryRoot.TrimEnd('\') + '\'
    if ($Candidate.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase)) {
        throw 'Recovery packages and restored signing material must be stored outside the repository.'
    }
}

$repoRoot = (Resolve-Path -LiteralPath (Split-Path -Parent $PSScriptRoot)).Path
$resolvedPackage = [IO.Path]::GetFullPath($PackagePath)
$resolvedTargetKeystore = [IO.Path]::GetFullPath($TargetKeystorePath)
$resolvedTargetSecret = [IO.Path]::GetFullPath($TargetSecretPath)
$effectivePassphrase = Resolve-RecoveryPassphrase -Supplied $Passphrase -Confirm ($Action -eq 'Export')
$dpapiCipherBytes = $null
$dpapiPlainBytes = $null
$payloadBytes = $null
$keystoreBytes = $null
$restoredSecretPlainBytes = $null
$restoredSecretCipherBytes = $null
$signing = $null
$payload = $null
$payloadObject = $null
$restoredSecret = $null
$envelope = $null

try {
    if ($Action -eq 'Export') {
        Assert-OutsideRepository -Candidate $resolvedPackage -RepositoryRoot $repoRoot
        if (Test-Path -LiteralPath $resolvedPackage) {
            throw "Refusing to replace the existing recovery package: $resolvedPackage"
        }

        $resolvedSecret = (Resolve-Path -LiteralPath $SecretPath -ErrorAction Stop).Path
        $dpapiCipherBytes = [IO.File]::ReadAllBytes($resolvedSecret)
        $dpapiPlainBytes = [Security.Cryptography.ProtectedData]::Unprotect(
            $dpapiCipherBytes,
            $null,
            [Security.Cryptography.DataProtectionScope]::CurrentUser
        )
        $signing = [Text.Encoding]::UTF8.GetString($dpapiPlainBytes) | ConvertFrom-Json
        foreach ($property in @('storeFile', 'storePassword', 'keyAlias', 'keyPassword')) {
            if ([string]::IsNullOrWhiteSpace($signing.$property)) {
                throw "The protected signing envelope is missing '$property'."
            }
        }
        $keystoreBytes = [IO.File]::ReadAllBytes((Resolve-Path -LiteralPath $signing.storeFile -ErrorAction Stop).Path)
        $keystoreSha256 = Get-Sha256Hex -Value $keystoreBytes
        $certificateSha256 = Get-KeystoreCertificateSha256 `
            -KeystoreBytes $keystoreBytes `
            -StorePassword $signing.storePassword `
            -KeyAlias $signing.keyAlias
        if ($signing.certificateSha256 -and $signing.certificateSha256 -ne $certificateSha256) {
            throw 'The protected signing envelope certificate does not match its keystore.'
        }

        $payloadObject = [ordered]@{
            version = 1
            createdAtUtc = [DateTimeOffset]::UtcNow.ToString('o')
            storeFileName = [IO.Path]::GetFileName($signing.storeFile)
            storeBytes = [Convert]::ToBase64String($keystoreBytes)
            storeSha256 = $keystoreSha256
            storePassword = $signing.storePassword
            keyAlias = $signing.keyAlias
            keyPassword = $signing.keyPassword
            certificateSha256 = $certificateSha256
        }
        $payloadBytes = [Text.Encoding]::UTF8.GetBytes(($payloadObject | ConvertTo-Json -Compress))
        $envelope = Protect-RecoveryPayload -PlainBytes $payloadBytes -SecurePassphrase $effectivePassphrase
        $packageDirectory = Split-Path -Parent $resolvedPackage
        New-Item -ItemType Directory -Path $packageDirectory -Force | Out-Null
        $temporaryPackage = Join-Path $packageDirectory ('.{0}.{1}.tmp' -f ([IO.Path]::GetFileName($resolvedPackage)), [Guid]::NewGuid().ToString('N'))
        try {
            [IO.File]::WriteAllText(
                $temporaryPackage,
                ($envelope | ConvertTo-Json -Compress),
                [Text.UTF8Encoding]::new($false)
            )
            Move-Item -LiteralPath $temporaryPackage -Destination $resolvedPackage
        } finally {
            if (Test-Path -LiteralPath $temporaryPackage -PathType Leaf) {
                Remove-Item -LiteralPath $temporaryPackage -Force
            }
        }
        Write-Output "Encrypted Play upload recovery package created: $resolvedPackage"
        Write-Output "Package SHA-256: $((Get-FileHash -LiteralPath $resolvedPackage -Algorithm SHA256).Hash)"
        Write-Output "Upload certificate SHA-256: $certificateSha256"
        return
    }

    $resolvedPackage = (Resolve-Path -LiteralPath $resolvedPackage -ErrorAction Stop).Path
    $envelope = Get-Content -LiteralPath $resolvedPackage -Raw | ConvertFrom-Json
    $payloadBytes = Unprotect-RecoveryPayload -Envelope $envelope -SecurePassphrase $effectivePassphrase
    $payload = [Text.Encoding]::UTF8.GetString($payloadBytes) | ConvertFrom-Json
    foreach ($property in @('storeBytes', 'storeSha256', 'storePassword', 'keyAlias', 'keyPassword', 'certificateSha256')) {
        if ([string]::IsNullOrWhiteSpace($payload.$property)) {
            throw "The recovery payload is missing '$property'."
        }
    }
    $keystoreBytes = [Convert]::FromBase64String($payload.storeBytes)
    $keystoreSha256 = Get-Sha256Hex -Value $keystoreBytes
    if ($keystoreSha256 -ne $payload.storeSha256) {
        throw 'The recovered keystore hash does not match the authenticated payload.'
    }
    $certificateSha256 = Get-KeystoreCertificateSha256 `
        -KeystoreBytes $keystoreBytes `
        -StorePassword $payload.storePassword `
        -KeyAlias $payload.keyAlias
    if ($certificateSha256 -ne $payload.certificateSha256) {
        throw 'The recovered keystore certificate does not match the authenticated payload.'
    }

    if ($Action -eq 'Verify') {
        Write-Output "Recovery package verified: $resolvedPackage"
        Write-Output "Package SHA-256: $((Get-FileHash -LiteralPath $resolvedPackage -Algorithm SHA256).Hash)"
        Write-Output "Upload certificate SHA-256: $certificateSha256"
        return
    }

    Assert-OutsideRepository -Candidate $resolvedTargetKeystore -RepositoryRoot $repoRoot
    Assert-OutsideRepository -Candidate $resolvedTargetSecret -RepositoryRoot $repoRoot
    if (Test-Path -LiteralPath $resolvedTargetKeystore) {
        throw "Refusing to replace the existing upload keystore: $resolvedTargetKeystore"
    }
    if (Test-Path -LiteralPath $resolvedTargetSecret) {
        throw "Refusing to replace the existing protected signing envelope: $resolvedTargetSecret"
    }

    $restoredSecret = [ordered]@{
        version = 1
        recoveredAtUtc = [DateTimeOffset]::UtcNow.ToString('o')
        storeFile = $resolvedTargetKeystore
        storePassword = $payload.storePassword
        keyAlias = $payload.keyAlias
        keyPassword = $payload.keyPassword
        certificateSha256 = $certificateSha256
    }
    $restoredSecretPlainBytes = [Text.Encoding]::UTF8.GetBytes(($restoredSecret | ConvertTo-Json -Compress))
    $restoredSecretCipherBytes = [Security.Cryptography.ProtectedData]::Protect(
        $restoredSecretPlainBytes,
        $null,
        [Security.Cryptography.DataProtectionScope]::CurrentUser
    )

    New-Item -ItemType Directory -Path (Split-Path -Parent $resolvedTargetKeystore) -Force | Out-Null
    New-Item -ItemType Directory -Path (Split-Path -Parent $resolvedTargetSecret) -Force | Out-Null
    try {
        [IO.File]::WriteAllBytes($resolvedTargetKeystore, $keystoreBytes)
        [IO.File]::WriteAllBytes($resolvedTargetSecret, $restoredSecretCipherBytes)
    } catch {
        if (Test-Path -LiteralPath $resolvedTargetSecret -PathType Leaf) {
            Remove-Item -LiteralPath $resolvedTargetSecret -Force
        }
        if (Test-Path -LiteralPath $resolvedTargetKeystore -PathType Leaf) {
            Remove-Item -LiteralPath $resolvedTargetKeystore -Force
        }
        throw
    }
    Write-Output "Play upload keystore restored: $resolvedTargetKeystore"
    Write-Output "Current-user DPAPI signing envelope restored: $resolvedTargetSecret"
    Write-Output "Upload certificate SHA-256: $certificateSha256"
} finally {
    Clear-Bytes -Value $aadBytes
    Clear-Bytes -Value $dpapiCipherBytes
    Clear-Bytes -Value $dpapiPlainBytes
    Clear-Bytes -Value $payloadBytes
    Clear-Bytes -Value $keystoreBytes
    Clear-Bytes -Value $restoredSecretPlainBytes
    Clear-Bytes -Value $restoredSecretCipherBytes
    $signing = $null
    $payload = $null
    $payloadObject = $null
    $restoredSecret = $null
    $envelope = $null
    $effectivePassphrase = $null
}
