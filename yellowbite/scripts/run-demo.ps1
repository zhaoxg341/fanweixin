param([switch]$CheckOnly)
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$sdkRoot = Join-Path $repoRoot '.tools\sdk'
$avdRoot = Join-Path $repoRoot '.tools\avd'
$apk = Join-Path $repoRoot 'artifacts\yellowbite-demo.apk'
foreach ($required in @((Join-Path $sdkRoot 'emulator\emulator.exe'), (Join-Path $avdRoot 'YellowBite.avd\config.ini'), $apk)) {
    if (-not (Test-Path -LiteralPath $required)) { throw "缺少本机演示文件：$required。本启动器使用当前电脑已准备好的 .tools 环境。" }
}
# Use an ASCII alias because the native emulator has trouble with Chinese paths.
$aliasRoot = Join-Path $env:TEMP 'yellowbite-android-tools'
New-Item -ItemType Directory -Force -Path $aliasRoot | Out-Null
foreach ($entry in @(@('sdk', $sdkRoot), @('avd', $avdRoot))) {
    $link = Join-Path $aliasRoot $entry[0]
    if (Test-Path -LiteralPath $link) {
        $existing = Get-Item -LiteralPath $link -Force
        if ($existing.LinkType -ne 'Junction' -or [string]$existing.Target -ne $entry[1]) {
            throw "临时入口已被其他目录占用：$link。请不要删除项目文件，将此输出发给我检查即可。"
        }
    } else { New-Item -ItemType Junction -Path $link -Target $entry[1] | Out-Null }
}
$prefs = Join-Path $env:TEMP 'yellowbite-emulator-prefs'
New-Item -ItemType Directory -Force -Path $prefs | Out-Null
$env:ANDROID_HOME = Join-Path $aliasRoot 'sdk'
$env:ANDROID_AVD_HOME = Join-Path $aliasRoot 'avd'
$env:ANDROID_USER_HOME = $prefs
$env:ANDROID_EMULATOR_HOME = $prefs
"avd.ini.encoding=UTF-8`npath=$aliasRoot\avd\YellowBite.avd`ntarget=android-35" | Set-Content -LiteralPath (Join-Path $avdRoot 'YellowBite.ini') -Encoding ascii
$emulator = Join-Path $env:ANDROID_HOME 'emulator\emulator.exe'
$adb = Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe'
if ($CheckOnly) {
    $names = @(& $emulator -list-avds)
    if ($LASTEXITCODE -ne 0 -or $names -notcontains 'YellowBite') { throw '没有找到 YellowBite 虚拟手机配置' }
    Write-Host "模拟器：$sdkRoot\emulator\emulator.exe"
    Write-Host "虚拟手机：$avdRoot\YellowBite.avd"
    Write-Host "安装包：$apk"
    Write-Host '配置检查通过。'
    exit 0
}
& $adb start-server
if ($LASTEXITCODE -ne 0) { throw 'ADB 启动失败' }
$devices = @(& $adb devices)
$serial = $null
foreach ($line in $devices) {
    if ($line -match '^(emulator-\d+)\s+device$') {
        $candidate = $Matches[1]
        $name = (& $adb -s $candidate shell getprop ro.boot.qemu.avd_name 2>$null | Out-String).Trim()
        if ($name -eq 'YellowBite') { $serial = $candidate; break }
    }
}
if (-not $serial) {
    $used = @($devices | ForEach-Object { if ($_ -match '^emulator-(\d+)\s+') { [int]$Matches[1] } })
    $port = 5554
    while ($used -contains $port) { $port += 2 }
    $serial = "emulator-$port"
    Write-Host '正在打开安卓手机窗口，首次开机可能需要1至3分钟……'
    # This is the interactive phone window requested by the user running the launcher.
    Start-Process -FilePath $emulator -ArgumentList @('-avd', 'YellowBite', '-port', "$port", '-gpu', 'swiftshader', '-no-snapshot', '-no-boot-anim', '-no-audio', '-memory', '2048') -WindowStyle Normal
} else { Write-Host '已找到正在运行的 YellowBite 手机，正在打开应用……' }
$deadline = (Get-Date).AddMinutes(4)
$booted = $false
while ((Get-Date) -lt $deadline) {
    try { $boot = (& $adb -s $serial shell getprop sys.boot_completed 2>$null | Out-String).Trim() }
    catch { $boot = '' }
    if ($boot -eq '1') { $booted = $true; break }
    Start-Sleep -Seconds 2
}
if (-not $booted) { throw '等待安卓开机超时。请保留手机窗口和终端输出，以便检查。' }
& $adb -s $serial shell input keyevent 224
& $adb -s $serial shell input keyevent 82
Write-Host '正在安装并打开小黄外卖……'
& $adb -s $serial install -r $apk
if ($LASTEXITCODE -ne 0) { throw 'APK 安装失败。请保留终端输出；启动器不会自动清除已有数据。' }
& $adb -s $serial shell am start -n com.yellowbite.app/.MainActivity
if ($LASTEXITCODE -ne 0) { throw '应用启动失败' }
Write-Host '小黄外卖已打开。保持离线演示即可使用，无需启动后端。'
