$ErrorActionPreference = 'Stop'
$kitRoot = Join-Path (Split-Path -Parent $PSScriptRoot) '.offline-build-kit'
$maven = Join-Path $kitRoot 'maven\bin\mvn.cmd'
$repository = Join-Path $kitRoot 'repository'
$jdk = Join-Path $kitRoot 'jdk'
if (-not (Test-Path $maven) -or -not (Test-Path $repository -PathType Container) -or -not (Test-Path (Join-Path $jdk 'bin\java.exe'))) {
    throw 'Offline build kit is missing. Download and extract geo-guard-offline-build-kit.zip using ..\build-offline.ps1 -KitArchive <path>.'
}
$previousJavaHome = $env:JAVA_HOME
$env:JAVA_HOME = $jdk
Push-Location $PSScriptRoot
try {
    & $maven '-o' '-B' "-Dmaven.repo.local=$repository" '-DskipTests' 'package'
    if ($LASTEXITCODE -ne 0) {
        throw "Maven build failed with exit code $LASTEXITCODE"
    }
} finally {
    Pop-Location
    if ($null -eq $previousJavaHome) {
        Remove-Item Env:JAVA_HOME -ErrorAction SilentlyContinue
    } else {
        $env:JAVA_HOME = $previousJavaHome
    }
}
