param(
    [Parameter(Mandatory = $true)][string]$PostgreSqlBin,
    [int]$Port = 15432,
    [ValidatePattern('^[A-Za-z][A-Za-z0-9_-]{1,29}$')][string]$Username = 'admin'
)

$ErrorActionPreference = 'Stop'
$psql = Join-Path $PostgreSqlBin 'psql.exe'
$java = Join-Path $PSScriptRoot '.offline-build-kit\jdk\bin\java.exe'
$bcrypt = Join-Path $PSScriptRoot '.offline-build-kit\repository\cn\dev33\sa-token-core\1.45.0\sa-token-core-1.45.0.jar'
$helper = Join-Path $PSScriptRoot 'tools\HashLocalPassword.java'
$runtime = Join-Path $PSScriptRoot '.runtime'
foreach ($path in @($psql, $java, $bcrypt, $helper)) {
    if (!(Test-Path -LiteralPath $path)) { throw "Required file missing: $path" }
}
$previousPgPassword = $env:PGPASSWORD
$previousAdminPassword = $env:GEO_ADMIN_PASSWORD
try {
    $env:PGPASSWORD = $env:GEO_DB_PASSWORD
    if (!$env:PGPASSWORD) {
        $env:PGPASSWORD = [IO.File]::ReadAllText((Join-Path $runtime 'postgres-password.txt')).Trim()
    }
    $connection = @('-h', '127.0.0.1', '-p', "$Port", '-U', 'geo_guard', '-d', 'geo_guard', '-X', '-v', 'ON_ERROR_STOP=1')
    $count = & $psql @connection -t -A -c 'SELECT count(*) FROM public.sys_user;'
    if ($LASTEXITCODE -ne 0) { throw 'Cannot query local users.' }
    if ([int]$count -gt 0) {
        Write-Output 'Existing users preserved; bootstrap is only for an empty user table.'
        exit 0
    }
    $generated = !$env:GEO_ADMIN_PASSWORD
    if ($generated) {
        $bytes = New-Object byte[] 15
        [Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
        $env:GEO_ADMIN_PASSWORD = [Convert]::ToBase64String($bytes)
    }
    $hash = & $java --class-path $bcrypt $helper
    if ($LASTEXITCODE -ne 0 -or $hash -notmatch '^\$2[aby]\$\d{2}\$[./A-Za-z0-9]{53}$') { throw 'Offline BCrypt generation failed.' }
    $secretBytes = New-Object byte[] 24
    [Security.Cryptography.RandomNumberGenerator]::Fill($secretBytes)
    $clientSecret = [Convert]::ToBase64String($secretBytes)
    $sql = @"
BEGIN;
LOCK TABLE public.sys_user, public.sys_client, public.sys_role, public.sys_dept IN SHARE ROW EXCLUSIVE MODE;
DO `$`$ BEGIN
  IF EXISTS (SELECT 1 FROM public.sys_user) THEN
    RAISE EXCEPTION 'Users were created concurrently; bootstrap cancelled';
  END IF;
END `$`$;
INSERT INTO public.sys_dept (dept_id, parent_id, ancestors, dept_name, status, create_time)
VALUES (1, 0, '0', '本地项目', '0', CURRENT_TIMESTAMP);
INSERT INTO public.sys_role (role_id, role_name, role_key, role_sort, status, create_time)
VALUES (1, '本地管理员', 'superadmin', 1, '0', CURRENT_TIMESTAMP);
INSERT INTO public.sys_user (user_id, dept_id, user_name, nick_name, password, status, create_time)
VALUES (1, 1, '$Username', '本地管理员', '$hash', '0', CURRENT_TIMESTAMP);
INSERT INTO public.sys_user_role (user_id, role_id) VALUES (1, 1);
INSERT INTO public.sys_client (id, client_id, client_key, client_secret, grant_type, device_type, status, create_time)
VALUES (1, 'geo-local', 'geo-guard-web', '$clientSecret', 'password', 'pc', '0', CURRENT_TIMESTAMP);
COMMIT;
"@
    $sql | & $psql @connection | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Local account initialization failed and transaction rolled back.' }
    if ($generated) {
        $passwordFile = Join-Path $runtime 'admin-password.txt'
        [IO.File]::WriteAllText($passwordFile, $env:GEO_ADMIN_PASSWORD)
        Write-Output "Generated password saved locally: $passwordFile (Git ignored)."
    }
    Write-Output "Local administrator created: $Username. Frontend VITE_APP_CLIENT_ID=geo-local."
} finally {
    if ($null -eq $previousPgPassword) { Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue }
    else { $env:PGPASSWORD = $previousPgPassword }
    if ($null -eq $previousAdminPassword) { Remove-Item Env:GEO_ADMIN_PASSWORD -ErrorAction SilentlyContinue }
    else { $env:GEO_ADMIN_PASSWORD = $previousAdminPassword }
}
