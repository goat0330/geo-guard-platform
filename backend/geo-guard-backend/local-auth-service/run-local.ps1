$ErrorActionPreference = 'Stop'
$jar = Join-Path $PSScriptRoot 'target\geo-guard-local-auth-0.1.0.jar'
if (-not (Test-Path -LiteralPath $jar)) {
    throw '尚未构建服务。请先运行 .\build-local.ps1'
}
if (Get-NetTCPConnection -State Listen -LocalPort 8007 -ErrorAction SilentlyContinue) {
    throw '本机 8007 端口已有服务监听；请先确认该服务归属，不要自动终止它。'
}

$username = (Read-Host '设置本地开发登录账号').Trim()
if ([string]::IsNullOrWhiteSpace($username)) {
    throw '账号不能为空。'
}
$securePassword = Read-Host '设置本地开发登录密码（至少 8 位）' -AsSecureString
if ($securePassword.Length -lt 8) {
    $securePassword.Dispose()
    throw '本地开发密码至少需要 8 位。'
}

$pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
try {
    $env:GEO_LOCAL_AUTH_USERNAME = $username
    $env:GEO_LOCAL_AUTH_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    & java -jar $jar --server.address=127.0.0.1 --server.port=8007 --spring.servlet.multipart.enabled=false
    if ($LASTEXITCODE -ne 0) {
        throw "Java 服务退出，代码 $LASTEXITCODE"
    }
} finally {
    Remove-Item Env:GEO_LOCAL_AUTH_USERNAME -ErrorAction SilentlyContinue
    Remove-Item Env:GEO_LOCAL_AUTH_PASSWORD -ErrorAction SilentlyContinue
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    $securePassword.Dispose()
}
