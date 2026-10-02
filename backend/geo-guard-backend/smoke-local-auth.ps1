param(
    [string]$BaseUrl = 'http://127.0.0.1:8008',
    [string]$DockerExecutable = 'docker',
    [string]$Username = 'admin'
)

# Real HTTP + real Redis verification. Never changes the server's captcha response.
$ErrorActionPreference = 'Stop'
$password = $env:GEO_ADMIN_PASSWORD
if (!$password) { $password = [IO.File]::ReadAllText((Join-Path $PSScriptRoot '.runtime\admin-password.txt')).Trim() }
function Assert-Result($Condition, $Description) {
    if (!$Condition) { throw "FAIL: $Description" }
    Write-Host "PASS: $Description"
}
function New-RealCaptcha {
    $response = Invoke-RestMethod "$BaseUrl/auth/code" -TimeoutSec 15
    Assert-Result ($response.code -eq 200 -and $response.data.captchaEnabled) 'Real captcha endpoint enabled'
    $bytes = [Convert]::FromBase64String($response.data.img)
    Assert-Result ([Convert]::ToHexString($bytes[0..7]) -eq '89504E470D0A1A0A') 'Captcha contains an actual PNG image'
    $key = "geo-guard:global:captcha_codes:$($response.data.uuid)"
    $ttl = & $DockerExecutable exec geo-guard-business-redis redis-cli TTL $key
    Assert-Result ($LASTEXITCODE -eq 0 -and [int]$ttl -gt 0 -and [int]$ttl -le 120) 'Captcha exists in Redis with expiry'
    # White-box test reads the answer from the real cache; production clients never get it.
    $answerJson = & $DockerExecutable exec geo-guard-business-redis redis-cli --raw GET $key
    if ($LASTEXITCODE -ne 0) { throw 'Cannot read issued captcha from Redis.' }
    return @{Uuid = $response.data.uuid; Key = $key; Answer = [string]($answerJson | ConvertFrom-Json)}
}
function Invoke-Login($Captcha, $Password, $Code) {
    $body = @{username=$Username; password=$Password; clientId='geo-local'; grantType='password'; uuid=$Captcha.Uuid; code=$Code}
    return Invoke-RestMethod "$BaseUrl/auth/login" -Method Post -ContentType 'application/json' `
        -Body ($body | ConvertTo-Json -Compress) -TimeoutSec 15
}

$captcha = New-RealCaptcha
$result = Invoke-Login $captcha $password 'wrong'
Assert-Result ($result.code -ne 200 -and $result.msg -match 'jcaptcha|验证码') 'Wrong captcha rejected'
$exists = & $DockerExecutable exec geo-guard-business-redis redis-cli EXISTS $captcha.Key
Assert-Result ($exists -eq '0') 'Captcha consumed after validation'
$result = Invoke-Login $captcha $password $captcha.Answer
Assert-Result ($result.code -ne 200 -and $result.msg -match 'expire|失效|过期') 'Consumed captcha cannot be reused'

$captcha = New-RealCaptcha
$result = Invoke-Login $captcha 'wrong-password' $captcha.Answer
Assert-Result ($result.code -ne 200 -and $result.msg -match 'password|密码') 'Wrong password rejected'

$captcha = New-RealCaptcha
$result = Invoke-Login $captcha $password $captcha.Answer
Assert-Result ($result.code -eq 200 -and !!$result.data.access_token -and $result.data.userId -eq 1) 'Real administrator login succeeded'
$headers = @{'bwy-token'=$result.data.access_token; clientid='geo-local'}
try {
    $info = Invoke-RestMethod "$BaseUrl/system/user/getInfo" -Headers $headers -TimeoutSec 15
    Assert-Result ($info.code -eq 200 -and $info.data.user.userName -eq $Username) 'User information loaded from local PostgreSQL'
    $routes = Invoke-RestMethod "$BaseUrl/system/menu/getRouters" -Headers $headers -TimeoutSec 15
    Assert-Result ($routes.code -eq 200) 'Menu query completed against local PostgreSQL'
    $wrongClientHeaders = @{'bwy-token'=$result.data.access_token; clientid='other-client'}
    $denied = Invoke-RestMethod "$BaseUrl/system/user/getInfo" -Headers $wrongClientHeaders -TimeoutSec 15
    Assert-Result ($denied.code -eq 401) 'Token rejected for a different client ID'
} finally {
    $logout = Invoke-RestMethod "$BaseUrl/auth/logout" -Method Post -ContentType 'application/json' -Body '{}' -Headers $headers -TimeoutSec 15
    Assert-Result ($logout.code -eq 200) 'Logout completed'
}
$expired = Invoke-RestMethod "$BaseUrl/system/user/getInfo" -Headers $headers -TimeoutSec 15
Assert-Result ($expired.code -eq 401) 'Logged-out token rejected'
