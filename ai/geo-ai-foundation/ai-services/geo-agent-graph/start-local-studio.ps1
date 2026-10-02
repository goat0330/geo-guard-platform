param([int]$Port = 2024)
$ErrorActionPreference = 'Stop'
$cli = Join-Path $PSScriptRoot '.studio-venv\Scripts\langgraph.exe'
if (!(Test-Path -LiteralPath $cli)) { $cli = Join-Path $PSScriptRoot '.venv\Scripts\langgraph.exe' }
if (!(Test-Path -LiteralPath $cli)) { throw 'Install the studio extra in a local virtual environment first; see README.' }
if (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue) { throw "Port $Port is already in use; existing service preserved." }
$runtime = Join-Path $PSScriptRoot '.runtime'
New-Item -ItemType Directory -Path $runtime -Force | Out-Null
$previousEncoding = $env:PYTHONIOENCODING
$previousUtf8 = $env:PYTHONUTF8
$previousColor = $env:LOG_COLOR
try {
    # Redirected Windows logs need UTF-8 and no ANSI color renderer dependency.
    $env:PYTHONIOENCODING = 'utf-8'
    $env:PYTHONUTF8 = '1'
    $env:LOG_COLOR = 'false'
    $process = Start-Process -FilePath $cli -WorkingDirectory $PSScriptRoot -WindowStyle Hidden `
        -ArgumentList @('dev', '--host', '127.0.0.1', '--port', $Port, '--no-browser', '--no-reload') `
        -RedirectStandardOutput (Join-Path $runtime 'studio.out.log') `
        -RedirectStandardError (Join-Path $runtime 'studio.err.log') -PassThru
    Write-Output "Official local Studio API launched: $($process.Id). Check http://127.0.0.1:$Port/ok. The Studio UI is hosted by LangChain."
} finally {
    $env:PYTHONIOENCODING = $previousEncoding
    $env:PYTHONUTF8 = $previousUtf8
    $env:LOG_COLOR = $previousColor
}
