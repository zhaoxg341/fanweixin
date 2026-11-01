param([int]$Port = 8000)
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $repoRoot
$venvPython = Join-Path $repoRoot 'backend\.venv\Scripts\python.exe'
if (-not (Test-Path -LiteralPath $venvPython)) {
    python -m venv backend\.venv
    if ($LASTEXITCODE -ne 0) { throw '创建 Python 虚拟环境失败' }
}
& $venvPython -m pip install --disable-pip-version-check --cache-dir (Join-Path $repoRoot '.tools\pip-cache') -r backend\requirements.txt
if ($LASTEXITCODE -ne 0) { throw '安装后端依赖失败' }
& $venvPython -m uvicorn backend.main:app --host 0.0.0.0 --port $Port
