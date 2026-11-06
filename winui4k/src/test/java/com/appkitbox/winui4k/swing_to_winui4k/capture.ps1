# Runs <component>/*.java in this folder (jbutton / jtextfield / jlist) one by one, captures the window that appears with PrintWindow, and
# saves it to doc/images/swing_to_winui4k/<JButton|JTextField|JList>/<file name>.png (the images of doc/swing_to_winui4k.md).
# The samples are launched one at a time in java's source-file mode (they are not run as tests).
#
# Usage (at the repository root):
#   .\gradlew :winui4k-all:shadowJar
#   pwsh winui4k\src\test\java\com\appkitbox\winui4k\swing_to_winui4k\capture.ps1                        # all
#   pwsh winui4k\src\test\java\com\appkitbox\winui4k\swing_to_winui4k\capture.ps1 -Component jbutton     # only one component
#   pwsh winui4k\src\test\java\com\appkitbox\winui4k\swing_to_winui4k\capture.ps1 -Component jbutton -Only ClickSwing,ClickWinUI4K
#
# No mouse or key input is sent (so as not to operate another window in front by mistake). The screen is in the state right after launch.
param(
    [string[]]$Only = @(),
    [string]$Component = "*",
    [int]$SettleMillis = 3000,
    [int]$TimeoutSeconds = 40
)
$ErrorActionPreference = "Stop"
# With pwsh -File, -Only a,b arrives as one string, so split it here
$Only = @($Only | ForEach-Object { $_ -split ',' } | Where-Object { $_ })
$Repo = (Resolve-Path "$PSScriptRoot\..\..\..\..\..\..\..\..").Path
$Jar = Get-ChildItem "$Repo\winui4k-all\build\libs\winui4k-*-all.jar" | Sort-Object LastWriteTime | Select-Object -Last 1
if (-not $Jar) { throw "The winui4k-all fat jar does not exist. Run .\gradlew :winui4k-all:shadowJar first" }
$OutRoot = "$Repo\doc\images\swing_to_winui4k"
# From the package name (lower case) to the image folder name (the Swing class name)
$ImageDirs = @{ jbutton = "JButton"; jtextfield = "JTextField"; jlist = "JList" }

Add-Type -AssemblyName System.Drawing
Add-Type -TypeDefinition @"
using System;
using System.Runtime.InteropServices;
public static class SampleShot {
    [DllImport("user32.dll")] public static extern bool SetProcessDPIAware();
    [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr h);
    [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr h, out RECT r);
    [DllImport("user32.dll")] public static extern bool PrintWindow(IntPtr h, IntPtr hdc, uint flags);
    [DllImport("dwmapi.dll")] public static extern int DwmGetWindowAttribute(IntPtr h, int attr, out RECT r, int size);
    [StructLayout(LayoutKind.Sequential)] public struct RECT { public int Left, Top, Right, Bottom; }
}
"@
[SampleShot]::SetProcessDPIAware() | Out-Null

function Save-Window([IntPtr]$h, [string]$path) {
    $window = New-Object SampleShot+RECT
    [void][SampleShot]::GetWindowRect($h, [ref]$window)
    # The window rectangle on Windows 11 includes a transparent shadow frame, so crop to the visible frame (DWMWA_EXTENDED_FRAME_BOUNDS)
    $frame = New-Object SampleShot+RECT
    if ([SampleShot]::DwmGetWindowAttribute($h, 9, [ref]$frame, 16) -ne 0) { $frame = $window }
    $full = New-Object System.Drawing.Bitmap ($window.Right - $window.Left), ($window.Bottom - $window.Top)
    $g = [System.Drawing.Graphics]::FromImage($full)
    $dc = $g.GetHdc()
    [void][SampleShot]::PrintWindow($h, $dc, 2) # PW_RENDERFULLCONTENT (also captures the WinUI content)
    $g.ReleaseHdc($dc); $g.Dispose()
    $crop = New-Object System.Drawing.Rectangle ($frame.Left - $window.Left), ($frame.Top - $window.Top), ($frame.Right - $frame.Left), ($frame.Bottom - $frame.Top)
    $image = $full.Clone($crop, $full.PixelFormat)
    New-Item -ItemType Directory -Force (Split-Path $path) | Out-Null
    $image.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $image.Dispose(); $full.Dispose()
}

$sources = Get-ChildItem "$PSScriptRoot\$Component\*.java" | Where-Object { $Only.Count -eq 0 -or $Only -contains $_.BaseName } | Sort-Object FullName
$failed = @()
foreach ($source in $sources) {
    $component = $source.Directory.Name
    $out = "$OutRoot\$($ImageDirs[$component])\$($source.BaseName).png"
    Write-Host "$component/$($source.Name)"
    # The working directory is the sample's folder (relative paths such as icons/save.png are read from there)
    $process = Start-Process -FilePath "java" -PassThru -WindowStyle Normal -WorkingDirectory $source.DirectoryName -ArgumentList @(
        "--enable-native-access=ALL-UNNAMED", "-cp", "`"$($Jar.FullName)`"", "`"$($source.FullName)`""
    )
    try {
        $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
        $handle = [IntPtr]::Zero
        while ((Get-Date) -lt $deadline -and -not $process.HasExited) {
            $process.Refresh()
            if ($process.MainWindowHandle -ne [IntPtr]::Zero -and [SampleShot]::IsWindowVisible($process.MainWindowHandle)) {
                $handle = $process.MainWindowHandle
                break
            }
            Start-Sleep -Milliseconds 200
        }
        if ($handle -eq [IntPtr]::Zero) {
            Write-Host "  No window appeared" -ForegroundColor Red
            $failed += $source.Name
            continue
        }
        Start-Sleep -Milliseconds $SettleMillis
        Save-Window $handle $out
        Write-Host "  -> $out"
    } finally {
        if (-not $process.HasExited) { Stop-Process -Id $process.Id -Force }
    }
}
if ($failed.Count -gt 0) { throw "Samples that could not be captured: $($failed -join ', ')" }
