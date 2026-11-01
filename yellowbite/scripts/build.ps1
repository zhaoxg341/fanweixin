$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $repoRoot
$env:GRADLE_USER_HOME = Join-Path $repoRoot '.tools\gradle-home'
$env:ANDROID_USER_HOME = Join-Path $repoRoot '.tools\android-user'
if (Test-Path -LiteralPath (Join-Path $repoRoot '.tools\sdk')) {
    $env:ANDROID_HOME = Join-Path $repoRoot '.tools\sdk'
}
$localGradle = Join-Path $repoRoot '.tools\gradle\bin\gradle.bat'
$gradleArgs = @(':app:assembleDebug', ':app:testDebugUnitTest', '--max-workers=4')
if (Test-Path -LiteralPath $localGradle) {
    $javaFile = Join-Path $repoRoot '.tools\java-home.txt'
    if (Test-Path -LiteralPath $javaFile) { $env:JAVA_HOME = (Get-Content -LiteralPath $javaFile -Raw).Trim() }
    $proxyScript = Join-Path $repoRoot '.tools\maven_proxy.py'
    $proxyInit = Join-Path $repoRoot '.tools\maven.init.gradle'
    if ((Test-Path -LiteralPath $proxyScript) -and (Test-Path -LiteralPath $proxyInit)) {
        try { $null = Invoke-WebRequest 'http://127.0.0.1:8765/central/junit/junit/4.13.2/junit-4.13.2.pom' -TimeoutSec 2 }
        catch {
            Start-Process -FilePath 'python' -ArgumentList @('.tools\maven_proxy.py') -WorkingDirectory $repoRoot -WindowStyle Hidden
            Start-Sleep -Seconds 1
        }
        $gradleArgs = @('-I', $proxyInit) + $gradleArgs
    }
    & $localGradle @gradleArgs
} else {
    & .\gradlew.bat @gradleArgs
}
if ($LASTEXITCODE -ne 0) { throw '构建失败，请检查 JDK、Android SDK 和网络环境' }
New-Item -ItemType Directory -Force -Path artifacts | Out-Null
Copy-Item -LiteralPath app\build\outputs\apk\debug\app-debug.apk -Destination artifacts\yellowbite-demo.apk
Write-Host 'APK 已生成：artifacts\yellowbite-demo.apk'
