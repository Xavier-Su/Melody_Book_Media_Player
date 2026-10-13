<#
  Build the Melody Book APK from the command line, without Android Studio.

  First run downloads and installs (into one folder, default <drive>:\AndroidBuildTools):
    - JDK 17 (Microsoft Build of OpenJDK)
    - Android command-line tools + platform-tools + Android 35 platform + build-tools
    - Gradle 8.10.2
    - Gradle dependency cache (~1 GB)
  Later runs reuse everything and only rebuild.

  Usage (run from the AndroidCompose folder, or just double-click build-apk.bat):
    .\build-apk.bat                    build the debug APK
    .\build-apk.bat -Install           also install it to a USB-connected phone (adb)
    .\build-apk.bat -SkipTests         skip unit tests
    .\build-apk.bat -NoMirror          do not use Aliyun / Tencent mirrors
    .\build-apk.bat -ToolsDir D:\abt   choose where the toolchain goes

  To uninstall the toolchain completely, delete the ToolsDir folder.
#>
param(
    [string]$ToolsDir = "",
    [switch]$Install,
    [switch]$SkipTests,
    [switch]$NoMirror
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'   # Invoke-WebRequest is ~10x faster without the progress bar
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$GradleVersion   = '8.10.2'
$CmdlineToolsZip = 'commandlinetools-win-11076708_latest.zip'
$SdkPackages     = @('platform-tools', 'platforms;android-35', 'build-tools;34.0.0')

$ProjectDir = $PSScriptRoot
if (-not $ToolsDir) {
    # Default to the same drive as the project (keeps C: free)
    $ToolsDir = Join-Path (Split-Path -Qualifier $ProjectDir) 'AndroidBuildTools'
}
$DownloadDir = Join-Path $ToolsDir 'downloads'
New-Item -ItemType Directory -Force -Path $DownloadDir | Out-Null

function Step([string]$msg) { Write-Host "`n==> $msg" -ForegroundColor Cyan }

function Download([string[]]$urls, [string]$dest) {
    if (Test-Path $dest) { return }
    foreach ($u in $urls) {
        try {
            Write-Host "    downloading $u"
            Invoke-WebRequest -Uri $u -OutFile "$dest.part" -UseBasicParsing
            Move-Item "$dest.part" $dest -Force
            return
        } catch {
            Write-Host "    failed: $($_.Exception.Message)" -ForegroundColor Yellow
            Remove-Item "$dest.part" -ErrorAction SilentlyContinue
        }
    }
    throw "All download sources failed for $(Split-Path -Leaf $dest). Check your network and run again."
}

function Unzip([string]$zip, [string]$dest) {
    New-Item -ItemType Directory -Force -Path $dest | Out-Null
    $tar = Get-Command tar.exe -ErrorAction SilentlyContinue
    if ($tar) {
        & tar.exe -xf $zip -C $dest
        if ($LASTEXITCODE -eq 0) { return }
    }
    Expand-Archive -Path $zip -DestinationPath $dest -Force
}

# Runs a .bat through cmd.exe with an exact command line.
# (PowerShell 5.1 mangles quotes/semicolons when calling .bat files directly.)
function Invoke-Bat([string]$commandLine, [string]$stdinFile = $null, [string]$stdoutFile = $null) {
    $p = @{
        FilePath     = 'cmd.exe'
        ArgumentList = "/s /c `"$commandLine`""
        NoNewWindow  = $true
        Wait         = $true
        PassThru     = $true
    }
    if ($stdinFile)  { $p.RedirectStandardInput  = $stdinFile }
    if ($stdoutFile) { $p.RedirectStandardOutput = $stdoutFile }
    return (Start-Process @p).ExitCode
}

try {
    Write-Host "Project : $ProjectDir"
    Write-Host "Tools   : $ToolsDir"

    $drive = Get-PSDrive -Name (Split-Path -Qualifier $ToolsDir).TrimEnd(':') -ErrorAction SilentlyContinue
    if ($drive -and $drive.Free -lt 4GB) {
        Write-Warning ("Only {0:N1} GB free on {1}:. The first build needs about 3 GB." -f ($drive.Free / 1GB), $drive.Name)
    }

    # ------------------------------------------------------------------ JDK 17
    $JdkRoot = Join-Path $ToolsDir 'jdk'
    $javaExe = Get-ChildItem $JdkRoot -Filter java.exe -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $javaExe) {
        Step 'Installing JDK 17'
        $zip = Join-Path $DownloadDir 'jdk17.zip'
        Download @('https://aka.ms/download-jdk/microsoft-jdk-17-windows-x64.zip') $zip
        Unzip $zip $JdkRoot
        $javaExe = Get-ChildItem $JdkRoot -Filter java.exe -Recurse | Select-Object -First 1
    }
    $env:JAVA_HOME = $javaExe.Directory.Parent.FullName
    $env:Path = "$env:JAVA_HOME\bin;$env:Path"
    Write-Host "JDK     : $env:JAVA_HOME"

    # ------------------------------------------------------------------ Android SDK
    $SdkDir = Join-Path $ToolsDir 'android-sdk'
    $sdkmanager = Join-Path $SdkDir 'cmdline-tools\latest\bin\sdkmanager.bat'
    if (-not (Test-Path $sdkmanager)) {
        Step 'Installing Android command-line tools'
        $zip = Join-Path $DownloadDir $CmdlineToolsZip
        Download @("https://dl.google.com/android/repository/$CmdlineToolsZip") $zip
        $tmp = Join-Path $DownloadDir 'cmdline-tmp'
        Remove-Item $tmp -Recurse -Force -ErrorAction SilentlyContinue
        Unzip $zip $tmp
        $latest = Join-Path $SdkDir 'cmdline-tools\latest'
        New-Item -ItemType Directory -Force -Path (Split-Path $latest) | Out-Null
        Move-Item (Join-Path $tmp 'cmdline-tools') $latest
        Remove-Item $tmp -Recurse -Force -ErrorAction SilentlyContinue
    }
    $env:ANDROID_HOME = $SdkDir
    $env:ANDROID_SDK_ROOT = $SdkDir

    $missing = $SdkPackages | Where-Object { -not (Test-Path (Join-Path $SdkDir ($_ -replace ';', '\'))) }
    if ($missing) {
        Step "Installing SDK packages: $($missing -join ', ')"
        $yesFile = Join-Path $DownloadDir 'yes.txt'
        Set-Content -Path $yesFile -Value (@('y') * 50) -Encoding ASCII
        $licLog = Join-Path $DownloadDir 'licenses.log'
        Invoke-Bat "`"$sdkmanager`" --sdk_root=`"$SdkDir`" --licenses" $yesFile $licLog | Out-Null

        $pkgFile = Join-Path $DownloadDir 'packages.txt'
        Set-Content -Path $pkgFile -Value $missing -Encoding ASCII
        $code = Invoke-Bat "`"$sdkmanager`" --sdk_root=`"$SdkDir`" --package_file=`"$pkgFile`""
        if ($code -ne 0) { throw "sdkmanager failed (exit $code)." }
    }

    # ------------------------------------------------------------------ Gradle
    $GradleHome = Join-Path $ToolsDir "gradle-$GradleVersion"
    $gradleBat = Join-Path $GradleHome 'bin\gradle.bat'
    if (-not (Test-Path $gradleBat)) {
        Step "Installing Gradle $GradleVersion"
        $zip = Join-Path $DownloadDir "gradle-$GradleVersion-bin.zip"
        $urls = @("https://services.gradle.org/distributions/gradle-$GradleVersion-bin.zip")
        if (-not $NoMirror) { $urls = @("https://mirrors.cloud.tencent.com/gradle/gradle-$GradleVersion-bin.zip") + $urls }
        Download $urls $zip
        Unzip $zip $ToolsDir
    }
    # Keep the ~1 GB dependency cache next to the tools instead of C:\Users\...\.gradle
    $env:GRADLE_USER_HOME = Join-Path $ToolsDir 'gradle-home'

    $gradleArgs = @('-p', $ProjectDir, '--no-daemon', '--console=plain')
    if (-not $NoMirror) {
        # Put Aliyun mirrors in front of the official repositories; official ones remain as fallback.
        $initFile = Join-Path $ToolsDir 'mirror.init.gradle'
        @'
def mirrors = [
    'https://maven.aliyun.com/repository/google',
    'https://maven.aliyun.com/repository/public',
    'https://maven.aliyun.com/repository/gradle-plugin',
]
beforeSettings { settings ->
    settings.pluginManagement.repositories {
        mirrors.each { u -> maven { url u } }
    }
    settings.dependencyResolutionManagement.repositories {
        mirrors.each { u -> maven { url u } }
    }
}
'@ | Set-Content -Path $initFile -Encoding ASCII
        $gradleArgs = @('--init-script', $initFile) + $gradleArgs
    }

    # ------------------------------------------------------------------ Build
    Step 'Building debug APK (the first build downloads dependencies and can take 5-15 minutes)'
    & $gradleBat @gradleArgs assembleDebug
    if ($LASTEXITCODE -ne 0) { throw 'Gradle build failed. Scroll up to the first "e:" or "What went wrong" line.' }

    $apk = Join-Path $ProjectDir 'app\build\outputs\apk\debug\app-debug.apk'
    $out = Join-Path $ProjectDir 'MelodyBook-debug.apk'
    Copy-Item $apk $out -Force

    if (-not $SkipTests) {
        Step 'Running unit tests'
        & $gradleBat @gradleArgs testDebugUnitTest
        if ($LASTEXITCODE -ne 0) {
            Write-Warning 'Some unit tests failed (the APK was still built). Report: app\build\reports\tests\testDebugUnitTest\index.html'
        } else {
            Write-Host '    all unit tests passed' -ForegroundColor Green
        }
    }

    if ($Install) {
        Step 'Installing to phone via adb'
        $adb = Join-Path $SdkDir 'platform-tools\adb.exe'
        & $adb devices
        & $adb install -r $out
        if ($LASTEXITCODE -ne 0) { Write-Warning 'adb install failed. Is USB debugging enabled and the phone authorized?' }
    }

    Write-Host "`nDone. APK: $out" -ForegroundColor Green
    Write-Host 'Copy it to your phone and open it to install (allow "install unknown apps" when asked).'
    exit 0
}
catch {
    Write-Host "`nBUILD FAILED: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
