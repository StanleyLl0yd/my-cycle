param(
    [Parameter(Mandatory = $true)][string]$PepkJar,
    [Parameter(Mandatory = $true)][string]$Keystore,
    [Parameter(Mandatory = $true)][string]$Alias,
    [Parameter(Mandatory = $true)][string]$EncryptionKey,
    [string]$OutputDir = "rustore-signing"
)

$ErrorActionPreference = "Stop"

foreach ($path in @($PepkJar, $Keystore)) {
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "File not found: $path"
    }
}

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
$pepkKeystore = $Keystore
$tempKeystore = $null

try {
    if ([IO.Path]::GetExtension($Keystore).Equals(".jks", [StringComparison]::OrdinalIgnoreCase)) {
        $tempKeystore = Join-Path ([IO.Path]::GetTempPath()) ("my-cycle-rustore-{0}.keystore" -f [guid]::NewGuid())
        & keytool -importkeystore -srckeystore $Keystore -destkeystore $tempKeystore
        if ($LASTEXITCODE -ne 0) { throw "keytool import failed" }
        $pepkKeystore = $tempKeystore
    }

    & java -jar $PepkJar "--keystore=$pepkKeystore" "--alias=$Alias" "--output=$OutputDir/pepk_out.zip" "--encryptionkey=$EncryptionKey" --include-cert
    if ($LASTEXITCODE -ne 0) { throw "PEPK export failed" }

    & keytool -exportcert -alias $Alias -keystore $Keystore -rfc -file "$OutputDir/uploadcert.pem"
    if ($LASTEXITCODE -ne 0) { throw "Certificate export failed" }

    Write-Host "Created:"
    Write-Host "  $OutputDir/pepk_out.zip"
    Write-Host "  $OutputDir/uploadcert.pem"
}
finally {
    if ($tempKeystore -and (Test-Path -LiteralPath $tempKeystore)) {
        Remove-Item -LiteralPath $tempKeystore -Force
    }
}
