$ErrorActionPreference = 'Stop'
$maven = Get-Command mvn.cmd -ErrorAction SilentlyContinue
if (-not $maven) {
    throw 'Maven was not found. Install Maven 3.6.3+ and add it to PATH.'
}
Push-Location $PSScriptRoot
try {
    & $maven.Source '-DskipTests' 'package'
    if ($LASTEXITCODE -ne 0) {
        throw "Maven build failed with exit code $LASTEXITCODE"
    }
} finally {
    Pop-Location
}
