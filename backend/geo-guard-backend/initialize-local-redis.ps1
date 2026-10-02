param(
    [string]$DockerExecutable = 'docker',
    [int]$Port = 26379
)

$ErrorActionPreference = 'Stop'
$container = 'geo-guard-business-redis'
$image = 'redis:7.4.10-alpine'
$data = Join-Path $PSScriptRoot '.runtime\redis-data'

& $DockerExecutable info --format '{{.ServerVersion}}' | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Start Docker Engine before initializing Redis.' }

$existing = & $DockerExecutable ps -a --filter "name=^/$container$" --format '{{.Names}}'
if ($LASTEXITCODE -ne 0) { throw 'Cannot inspect existing Redis container.' }
if ($existing) {
    $details = (& $DockerExecutable inspect $container | ConvertFrom-Json)[0]
    if ($LASTEXITCODE -ne 0) { throw 'Cannot inspect Redis configuration.' }
    $binding = $details.HostConfig.PortBindings.'6379/tcp'
    $mount = @($details.Mounts | Where-Object { $_.Destination -eq '/data' })
    if ($details.Config.Labels.'geo-guard.component' -ne 'business-redis' -or
        $details.Config.Image -ne $image -or $binding.Count -ne 1 -or
        $binding[0].HostIp -ne '127.0.0.1' -or $binding[0].HostPort -ne "$Port" -or
        $mount.Count -ne 1 -or $mount[0].Source -ne $data) {
        throw 'Existing container differs from this project configuration; preserved without changes.'
    }
    & $DockerExecutable start $container | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Redis container failed to start.' }
} else {
    & $DockerExecutable image inspect $image 2>$null | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Local image $image is missing. Import an offline image archive with docker load, or explicitly download it before running this script."
    }
    New-Item -ItemType Directory -Path $data -Force | Out-Null
    & $DockerExecutable run -d --name $container --label geo-guard.component=business-redis `
        --restart unless-stopped --publish "127.0.0.1:${Port}:6379" `
        --mount "type=bind,source=$data,target=/data" $image redis-server --appendonly yes | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Redis container creation failed; inspect docker logs and host port availability.' }
}

for ($attempt = 0; $attempt -lt 10; $attempt++) {
    $reply = & $DockerExecutable exec $container redis-cli ping 2>$null
    if ($LASTEXITCODE -eq 0 -and $reply -eq 'PONG') {
        Write-Output "Redis ready at 127.0.0.1:$Port; persistent data: $data"
        exit 0
    }
    Start-Sleep -Seconds 1
}
throw 'Redis did not return PONG; inspect docker logs geo-guard-business-redis.'
