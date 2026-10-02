param(
    [Parameter(Mandatory = $true)][string]$PostgreSqlBin,
    [int]$Port = 15432,
    [switch]$ApplyLocalViews
)
$ErrorActionPreference = 'Stop'
$backendRoot = $PSScriptRoot
$runtimeRoot = Join-Path $backendRoot '.runtime'
$dataRoot = Join-Path $runtimeRoot 'postgres-data'
$passwordFile = Join-Path $runtimeRoot 'postgres-password.txt'
$pgCtl = Join-Path $PostgreSqlBin 'pg_ctl.exe'
$psql = Join-Path $PostgreSqlBin 'psql.exe'
foreach ($binary in @('initdb.exe', 'pg_ctl.exe', 'createdb.exe', 'psql.exe')) {
    if (-not (Test-Path -LiteralPath (Join-Path $PostgreSqlBin $binary))) { throw "Missing PostgreSQL binary: $binary" }
}
if (-not (Test-Path -LiteralPath (Join-Path $PostgreSqlBin '..\share\extension\postgis.control'))) {
    throw 'Install PostGIS for this PostgreSQL version before initializing the business database.'
}
New-Item -ItemType Directory -Path $runtimeRoot -Force | Out-Null
if (-not (Test-Path -LiteralPath (Join-Path $dataRoot 'PG_VERSION'))) {
    if (Test-Path -LiteralPath $dataRoot) { throw 'Existing data directory has no PG_VERSION; inspect it before continuing.' }
    if (-not (Test-Path -LiteralPath $passwordFile)) {
        $random = [byte[]]::new(32)
        $generator = [Security.Cryptography.RandomNumberGenerator]::Create()
        try { $generator.GetBytes($random) } finally { $generator.Dispose() }
        [IO.File]::WriteAllText($passwordFile, [Convert]::ToBase64String($random))
    }
    & (Join-Path $PostgreSqlBin 'initdb.exe') -D $dataRoot -U geo_guard --auth-local=scram-sha-256 --auth-host=scram-sha-256 --encoding=UTF8 "--pwfile=$passwordFile"
    if ($LASTEXITCODE -ne 0) { throw 'initdb failed; existing files have been retained for diagnosis.' }
}
if (-not (Test-Path -LiteralPath $passwordFile)) { throw 'The project cluster password file is missing; existing data was not changed.' }
& $pgCtl -D $dataRoot status *> $null
if ($LASTEXITCODE -ne 0) {
    $pgProcess = Start-Process -FilePath $pgCtl -WindowStyle Hidden -PassThru -ArgumentList @(
        '-D', ('"' + $dataRoot + '"'), '-l', ('"' + (Join-Path $runtimeRoot 'postgres.log') + '"'),
        '-o', ('"-h 127.0.0.1 -p ' + $Port + '"'), 'start', '-w', '-t', '30'
    )
    # Wait for pg_ctl itself; PowerShell -Wait also waits for its long-lived server child.
    $pgProcess.WaitForExit()
    if ($pgProcess.ExitCode -ne 0) { throw 'PostgreSQL did not start. Inspect .runtime/postgres.log.' }
}
$previousPassword = $env:PGPASSWORD
try {
    $env:PGPASSWORD = [IO.File]::ReadAllText($passwordFile).Trim()
    $databaseExists = & $psql -h 127.0.0.1 -p $Port -U geo_guard -d postgres -At -c "SELECT EXISTS(SELECT 1 FROM pg_database WHERE datname='geo_guard');"
    if ($LASTEXITCODE -ne 0) { throw 'Could not authenticate to the local project cluster.' }
    if ($databaseExists -eq 'f') {
        & (Join-Path $PostgreSqlBin 'createdb.exe') -h 127.0.0.1 -p $Port -U geo_guard geo_guard
        if ($LASTEXITCODE -ne 0) { throw 'Could not create geo_guard database.' }
    }
    $tableCount = & $psql -h 127.0.0.1 -p $Port -U geo_guard -d geo_guard -At -c "SELECT count(*) FROM pg_tables WHERE schemaname='public' AND tablename <> 'spatial_ref_sys';"
    if ($LASTEXITCODE -ne 0) { throw 'Could not inspect the local database.' }
    if ([int]$tableCount -eq 0) {
        $schemaArgs = @('-h', '127.0.0.1', '-p', $Port, '-U', 'geo_guard', '-d', 'geo_guard', '-q', '-v', 'ON_ERROR_STOP=1', '--single-transaction')
        foreach ($schema in (Get-ChildItem -LiteralPath (Join-Path $backendRoot 'database') -Filter '*.sql' | Sort-Object Name)) {
            $schemaArgs += @('-f', $schema.FullName)
        }
        & $psql @schemaArgs
        if ($LASTEXITCODE -ne 0) { throw 'Schema transaction failed and was rolled back.' }
        Write-Host 'Local empty business schema initialized. No accounts or business records were imported.'
    } else {
        Write-Host "Existing database preserved ($tableCount tables); schema was not reapplied."
        if ($ApplyLocalViews) {
            & $psql -h 127.0.0.1 -p $Port -U geo_guard -d geo_guard -q -v ON_ERROR_STOP=1 --single-transaction `
                -f (Join-Path $backendRoot 'database\003-local-business-views.sql')
            if ($LASTEXITCODE -ne 0) { throw 'Local view migration failed and was rolled back.' }
            Write-Host 'Local storage tables and live business views installed; existing records preserved.'
        }
    }
    & $psql -h 127.0.0.1 -p $Port -U geo_guard -d geo_guard -c "SELECT current_database(), postgis_version();"
    if ($LASTEXITCODE -ne 0) { throw 'PostGIS verification failed.' }
} finally {
    $env:PGPASSWORD = $previousPassword
}
