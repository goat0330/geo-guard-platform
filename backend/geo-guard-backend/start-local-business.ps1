param([int]$Port = 8008)

$ErrorActionPreference = 'Stop'
$java = Join-Path $PSScriptRoot '.offline-build-kit\jdk\bin\java.exe'
$jar = Join-Path $PSScriptRoot 'chongqing-geological-disaster-start\target\chongqing-geological-disaster-start.jar'
$config = Join-Path $PSScriptRoot 'config\geo-local.yml'
$runtime = Join-Path $PSScriptRoot '.runtime'
foreach ($path in @($java, $jar, $config)) {
    if (!(Test-Path -LiteralPath $path)) { throw "Required file missing: $path. Run build-offline.ps1 first." }
}
if (Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue) {
    throw "Port $Port already has a listener; existing service preserved."
}
New-Item -ItemType Directory -Path $runtime -Force | Out-Null
$previousPassword = $env:GEO_DB_PASSWORD
$previousPort = $env:GEO_BUSINESS_PORT
$previousJwtSecret = $env:GEO_JWT_SECRET
try {
    if (!$env:GEO_DB_PASSWORD) {
        $passwordFile = Join-Path $runtime 'postgres-password.txt'
        if (!(Test-Path -LiteralPath $passwordFile)) { throw 'Initialize the local database or set GEO_DB_PASSWORD first.' }
        $env:GEO_DB_PASSWORD = [IO.File]::ReadAllText($passwordFile).Trim()
    }
    $env:GEO_BUSINESS_PORT = "$Port"
    if (!$env:GEO_JWT_SECRET) {
        $secretFile = Join-Path $runtime 'jwt-secret.txt'
        if (!(Test-Path -LiteralPath $secretFile)) {
            $secretBytes = New-Object byte[] 32
            [Security.Cryptography.RandomNumberGenerator]::Fill($secretBytes)
            [IO.File]::WriteAllText($secretFile, [Convert]::ToBase64String($secretBytes))
        }
        $env:GEO_JWT_SECRET = [IO.File]::ReadAllText($secretFile).Trim()
    }
    $process = Start-Process -FilePath $java -WorkingDirectory $PSScriptRoot -WindowStyle Hidden `
        -ArgumentList @('-jar', 'chongqing-geological-disaster-start/target/chongqing-geological-disaster-start.jar',
            '--spring.config.additional-location=file:./config/geo-local.yml') `
        -RedirectStandardOutput (Join-Path $runtime 'business.out.log') `
        -RedirectStandardError (Join-Path $runtime 'business.err.log') -PassThru
    Write-Output "Java process launched: $($process.Id). Check .runtime/business.out.log for startup result; target port: $Port."
} finally {
    if ($null -eq $previousPassword) { Remove-Item Env:GEO_DB_PASSWORD -ErrorAction SilentlyContinue }
    else { $env:GEO_DB_PASSWORD = $previousPassword }
    if ($null -eq $previousPort) { Remove-Item Env:GEO_BUSINESS_PORT -ErrorAction SilentlyContinue }
    else { $env:GEO_BUSINESS_PORT = $previousPort }
    if ($null -eq $previousJwtSecret) { Remove-Item Env:GEO_JWT_SECRET -ErrorAction SilentlyContinue }
    else { $env:GEO_JWT_SECRET = $previousJwtSecret }
}
