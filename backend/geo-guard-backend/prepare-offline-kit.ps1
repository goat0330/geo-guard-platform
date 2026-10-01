param(
    [Parameter(Mandatory = $true)]
    [string]$MavenHome,
    [Parameter(Mandatory = $true)]
    [string]$MavenRepository,
    [Parameter(Mandatory = $true)]
    [string]$JdkHome,
    [Parameter(Mandatory = $true)]
    [string]$OutputPath
)

$ErrorActionPreference = 'Stop'
$mvn = Join-Path $MavenHome 'bin\mvn.cmd'
if (-not (Test-Path $mvn -PathType Leaf)) { throw "Maven distribution is invalid: $MavenHome" }
if (-not (Test-Path $MavenRepository -PathType Container)) { throw "Maven repository is missing: $MavenRepository" }
$java = Join-Path $JdkHome 'bin\java.exe'
$javac = Join-Path $JdkHome 'bin\javac.exe'
if (-not (Test-Path $java -PathType Leaf) -or -not (Test-Path $javac -PathType Leaf) -or -not (Test-Path (Join-Path $JdkHome 'legal') -PathType Container)) {
    throw "JDK 21 distribution is invalid: $JdkHome"
}
$javaVersion = (& $java -version 2>&1 | Out-String)
if ($javaVersion -notmatch 'version "21(?:\.|"|\+)') { throw "JDK 21 is required: $($javaVersion.Trim())" }

$output = [System.IO.Path]::GetFullPath($OutputPath)
$staging = Join-Path ([System.IO.Path]::GetTempPath()) ("geo-guard-offline-kit-" + [guid]::NewGuid().ToString('N'))
$kit = Join-Path $staging 'geo-guard-offline-build-kit'
$kitMaven = Join-Path $kit 'maven'
$kitRepository = Join-Path $kit 'repository'
$kitJdk = Join-Path $kit 'jdk'
New-Item -ItemType Directory -Path $kitMaven, $kitRepository, $kitJdk -Force | Out-Null

try {
    Copy-Item -Path (Join-Path $MavenHome '*') -Destination $kitMaven -Recurse -Force
    Copy-Item -Path (Join-Path $MavenRepository '*') -Destination $kitRepository -Recurse -Force
    Copy-Item -Path (Join-Path $JdkHome '*') -Destination $kitJdk -Recurse -Force
    Set-Content -LiteralPath (Join-Path $kit 'README.txt') -Encoding UTF8 -Value @(
        'GeoGuard offline Java build kit (Windows x64)',
        'Includes Eclipse Temurin JDK 21, Apache Maven 3.9.16, and the isolated Maven repository used for the verified Java build.',
        'The repository contains public dependency artifacts and Maven plugins for offline mode.',
        'Original JDK component license notices are under jdk/legal; Maven license and notice files are under maven.'
    )
    New-Item -ItemType Directory -Path (Split-Path -Parent $output) -Force | Out-Null
    $temporaryArchive = "$output.partial.zip"
    if (Test-Path -LiteralPath $temporaryArchive) { Remove-Item -LiteralPath $temporaryArchive -Force }
    Compress-Archive -Path (Join-Path $kit '*') -DestinationPath $temporaryArchive -CompressionLevel Optimal
    Move-Item -LiteralPath $temporaryArchive -Destination $output -Force
} finally {
    $resolvedStaging = [System.IO.Path]::GetFullPath($staging)
    $tempRoot = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath())
    if ($resolvedStaging.StartsWith($tempRoot, [System.StringComparison]::OrdinalIgnoreCase) -and (Test-Path -LiteralPath $resolvedStaging)) {
        Remove-Item -LiteralPath $resolvedStaging -Recurse -Force
    }
}

Get-Item -LiteralPath $output | Select-Object FullName, Length
