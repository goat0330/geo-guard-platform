param(
    [string]$KitArchive
)

$ErrorActionPreference = 'Stop'
$kitRoot = Join-Path $PSScriptRoot '.offline-build-kit'
$maven = Join-Path $kitRoot 'maven\bin\mvn.cmd'
$repository = Join-Path $kitRoot 'repository'
$jdk = Join-Path $kitRoot 'jdk'
$java = Join-Path $jdk 'bin\java.exe'
$javac = Join-Path $jdk 'bin\javac.exe'

if ((-not (Test-Path $maven) -or -not (Test-Path $repository -PathType Container) -or -not (Test-Path $java) -or -not (Test-Path $javac)) -and $KitArchive) {
    if (-not (Test-Path -LiteralPath $KitArchive -PathType Leaf)) {
        throw "Offline kit archive was not found: $KitArchive"
    }
    New-Item -ItemType Directory -Path $kitRoot -Force | Out-Null
    Expand-Archive -LiteralPath $KitArchive -DestinationPath $kitRoot -Force
}

if (-not (Test-Path $maven) -or -not (Test-Path $repository -PathType Container) -or -not (Test-Path $java) -or -not (Test-Path $javac)) {
    throw 'Offline build kit is missing. Download geo-guard-offline-build-kit.zip from the GitHub Releases page and pass it with -KitArchive.'
}

$javaVersion = (& $java -version 2>&1 | Out-String)
if ($javaVersion -notmatch 'version "21(?:\.|"|\+)') {
    throw "JDK 21 is required. Detected: $($javaVersion.Trim())"
}

$previousJavaHome = $env:JAVA_HOME
$env:JAVA_HOME = $jdk
Push-Location $PSScriptRoot
try {
    & $maven '-o' '-B' "-Dmaven.repo.local=$repository" '-DskipTests=false' 'verify'
    if ($LASTEXITCODE -ne 0) {
        throw "Offline Maven verify failed with exit code $LASTEXITCODE"
    }
} finally {
    Pop-Location
    if ($null -eq $previousJavaHome) {
        Remove-Item Env:JAVA_HOME -ErrorAction SilentlyContinue
    } else {
        $env:JAVA_HOME = $previousJavaHome
    }
}
