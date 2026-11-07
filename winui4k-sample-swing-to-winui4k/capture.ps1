# Runs the samples in src/main/java/com/appkitbox/winui4k/swing_to_winui4k/<component>/<use case>/*.java one at a time,
# captures the window that appears with PrintWindow and saves it to doc/images/swing_to_winui4k/<JButton etc.>/<file name>.png
# (the images of doc/swing_to_winui4k.md).
# Samples are launched in java's source-file mode with <component>/ as the working directory (relative paths such as icons/save.png are read from there).
#
# Usage (from the repository root; uses the latest winui4k-all fat jar):
#   .\gradlew :winui4k-all:shadowJar
#   pwsh winui4k-sample-swing-to-winui4k\capture.ps1                        # all
#   pwsh winui4k-sample-swing-to-winui4k\capture.ps1 -Component jbutton     # one component only (wildcards allowed: -Component j[a-m]*)
#   pwsh winui4k-sample-swing-to-winui4k\capture.ps1 -Component jbutton -Only ClickSwing,ClickWinUI4K
#
# Dialogs, menus, tooltips and file dialogs appear in separate top-level windows, so all visible windows of the process
# are captured and composited into one image at their on-screen positions. With a single window, the result is that window cropped to its visible frame.
# Transparent parts of WinUI popups (menus, tooltips) come out black with PrintWindow, so pure black pixels are made transparent when compositing,
# and the overflow beyond the frame is determined by the extent of non-black pixels (shadow margins do not enlarge the image).
#
# The screen is captured right after launch. Samples that need an action after launch are listed in $ActionTable below. Keys are posted with PostMessageW to WinUI's
# InputSiteWindowClass (or the window if there is none) (mouse and SendInput are not used, so that another window in front is never operated by mistake).
param(
    [string[]]$Only = @(),
    [string]$Component = "*",
    [int]$SettleMillis = 3500,
    [int]$TimeoutSeconds = 40
)
$ErrorActionPreference = "Stop"
# With pwsh -File, -Only a,b arrives as a single string, so split it here
$Only = @($Only | ForEach-Object { $_ -split ',' } | Where-Object { $_ })
$Repo = (Resolve-Path "$PSScriptRoot\..").Path
$Samples = "$PSScriptRoot\src\main\java\com\appkitbox\winui4k\swing_to_winui4k"
$Jar = Get-ChildItem "$Repo\winui4k-all\build\libs\winui4k-*-all.jar" | Sort-Object LastWriteTime | Select-Object -Last 1
if (-not $Jar) { throw "The winui4k-all fat jar was not found. Run .\gradlew :winui4k-all:shadowJar first" }
$OutRoot = "$Repo\doc\images\swing_to_winui4k"
# From a component folder name (lower case) to the image folder name (the Swing class name)
$ImageDirs = @{
    jbutton = "JButton"
    jtextfield = "JTextField"
    jlist = "JList"
    jlabel = "JLabel"
    jcheckbox = "JCheckBox"
    jradiobutton = "JRadioButton"
    jtogglebutton = "JToggleButton"
    jcombobox = "JComboBox"
    jspinner = "JSpinner"
    jslider = "JSlider"
    jprogressbar = "JProgressBar"
    jtextarea = "JTextArea"
    jformattedtextfield = "JFormattedTextField"
    jtextpane = "JTextPane"
    jpanel = "JPanel"
    jscrollpane = "JScrollPane"
    jscrollbar = "JScrollBar"
    jsplitpane = "JSplitPane"
    jtabbedpane = "JTabbedPane"
    jtable = "JTable"
    jtree = "JTree"
    jmenubar = "JMenuBar"
    jpopupmenu = "JPopupMenu"
    jtoolbar = "JToolBar"
    jseparator = "JSeparator"
    jframe = "JFrame"
    jdialog = "JDialog"
    jwindow = "JWindow"
    joptionpane = "JOptionPane"
    jfilechooser = "JFileChooser"
    jcolorchooser = "JColorChooser"
    jtooltip = "JToolTip"
    jinternalframe = "JInternalFrame"
    jlayeredpane = "JLayeredPane"
    jeditorpane = "JEditorPane"
    layout = "Layout"
}
# Actions after launch has settled (keys are "<component>/<file name>", * is allowed). Separated by commas and performed in order.
#   close    : send WM_CLOSE to the bottom (main) window (same as pressing x)
#   closetop : send WM_CLOSE to the top window (the x of a Swing dialog)
#   enter / esc : send the Enter / Esc key to the top window
#   top      : capture only the top window (to show only the file dialog)
#   nopopup  : capture without popups (tooltips etc.)
#   blurnav  : blur the navigation list on the left of the top window (the Windows file dialog)
#              (because it shows the Quick access folder names and the OneDrive name of the capturing PC)
$ActionTable = [ordered]@{
    # Show the confirmation dialog when trying to close
    "jframe/ClosingSwing" = "close"
    "jframe/ClosingWinUI4K" = "close"
    # Press the default button (OK) to show the input error
    "jdialog/ValidateSwing" = "enter"
    "jdialog/ValidateWinUI4K" = "enter"
    # Show that it does not close when trying to close (ContentDialog has no x, so Esc)
    "jdialog/CloseOperationSwing" = "closetop"
    "jdialog/CloseOperationWinUI4K" = "esc"
    # Show only the file dialog (WinUI4K uses the Windows dialog, so blur the list on the left)
    "jfilechooser/*WinUI4K" = "top,blurnav"
    "jfilechooser/*" = "top"
    # Do not capture the tooltip with the color name that the color picker's color spectrum shows on initial focus
    "jcolorchooser/EmbedWinUI4K" = "nopopup"
    "jcolorchooser/AlphaWinUI4K" = "nopopup"
}

Add-Type -AssemblyName System.Drawing
Add-Type -TypeDefinition @"
using System;
using System.Text;
using System.Collections.Generic;
using System.Runtime.InteropServices;
public static class SampleShot {
    public delegate bool EnumProc(IntPtr h, IntPtr l);
    [DllImport("user32.dll")] public static extern bool SetProcessDPIAware();
    [DllImport("user32.dll")] public static extern bool EnumWindows(EnumProc p, IntPtr l);
    [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr h, out uint pid);
    [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr h);
    [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr h, out RECT r);
    [DllImport("user32.dll")] public static extern bool PrintWindow(IntPtr h, IntPtr hdc, uint flags);
    [DllImport("user32.dll")] public static extern bool PostMessageW(IntPtr h, uint msg, IntPtr w, IntPtr l);
    [DllImport("user32.dll", CharSet=CharSet.Unicode)] public static extern IntPtr FindWindowExW(IntPtr parent, IntPtr after, string cls, string title);
    [DllImport("user32.dll", CharSet=CharSet.Unicode)] public static extern int GetClassNameW(IntPtr h, StringBuilder sb, int n);
    [DllImport("dwmapi.dll")] public static extern int DwmGetWindowAttribute(IntPtr h, int attr, out RECT r, int size);
    [DllImport("dwmapi.dll")] public static extern int DwmGetWindowAttribute(IntPtr h, int attr, out int v, int size);
    [StructLayout(LayoutKind.Sequential)] public struct RECT { public int Left, Top, Right, Bottom; }

    public static string ClassOf(IntPtr h) { var sb = new StringBuilder(256); GetClassNameW(h, sb, 256); return sb.ToString(); }

    // Find a descendant window whose class name is cls
    public static IntPtr FindDeep(IntPtr parent, string cls) {
        IntPtr c = IntPtr.Zero;
        while ((c = FindWindowExW(parent, c, null, null)) != IntPtr.Zero) {
            if (ClassOf(c) == cls) return c;
            var d = FindDeep(c, cls); if (d != IntPtr.Zero) return d;
        }
        return IntPtr.Zero;
    }

    // Top-level windows of pid with a size that are visible (not cloaked), in Z order from top to bottom
    public static List<IntPtr> WindowsOf(uint pid) {
        var list = new List<IntPtr>();
        EnumWindows((h, l) => {
            uint p; GetWindowThreadProcessId(h, out p);
            RECT r; int cloaked;
            if (p == pid && IsWindowVisible(h) && GetWindowRect(h, out r) && r.Right - r.Left > 40 && r.Bottom - r.Top > 40
                    && !(DwmGetWindowAttribute(h, 14, out cloaked, 4) == 0 && cloaked != 0)) list.Add(h); // 14: DWMWA_CLOAKED
            return true;
        }, IntPtr.Zero);
        return list;
    }

    // The visible frame (on Windows 11 the window rectangle includes the transparent shadow border, so DWMWA_EXTENDED_FRAME_BOUNDS)
    public static RECT FrameOf(IntPtr h) {
        RECT r;
        if (DwmGetWindowAttribute(h, 9, out r, 16) != 0) GetWindowRect(h, out r);
        return r;
    }

    // The extent of non-black pixels { left, top, right, bottom } (null if none). px is ARGB with width pixels per row
    public static int[] NonBlackBounds(int[] px, int width, int height) {
        int minX = int.MaxValue, minY = int.MaxValue, maxX = -1, maxY = -1;
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            if ((px[y * width + x] & 0xFFFFFF) == 0) continue;
            if (x < minX) minX = x; if (y < minY) minY = y; if (x > maxX) maxX = x; if (y > maxY) maxY = y;
        }
        return maxX < 0 ? null : new int[] { minX, minY, maxX + 1, maxY + 1 };
    }
}
"@
[SampleShot]::SetProcessDPIAware() | Out-Null

# Capture a window with PrintWindow and crop it to the visible frame (PW_RENDERFULLCONTENT also captures the WinUI content)
function Get-WindowBitmap([IntPtr]$h, $frame) {
    $w = New-Object SampleShot+RECT
    [void][SampleShot]::GetWindowRect($h, [ref]$w)
    $full = New-Object System.Drawing.Bitmap ([Math]::Max(1, $w.Right - $w.Left)), ([Math]::Max(1, $w.Bottom - $w.Top))
    $g = [System.Drawing.Graphics]::FromImage($full)
    $dc = $g.GetHdc()
    [void][SampleShot]::PrintWindow($h, $dc, 2)
    $g.ReleaseHdc($dc); $g.Dispose()
    $crop = [System.Drawing.Rectangle]::Intersect(
        [System.Drawing.Rectangle]::new($frame.Left - $w.Left, $frame.Top - $w.Top, $frame.Right - $frame.Left, $frame.Bottom - $frame.Top),
        [System.Drawing.Rectangle]::new(0, 0, $full.Width, $full.Height))
    $bitmap = $full.Clone($crop, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $full.Dispose()
    return $bitmap
}

# The extent of non-black pixels (Empty if none)
function Get-NonBlackBounds([System.Drawing.Bitmap]$b) {
    $data = $b.LockBits([System.Drawing.Rectangle]::new(0, 0, $b.Width, $b.Height), [System.Drawing.Imaging.ImageLockMode]::ReadOnly,
        [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $px = New-Object int[] ($b.Width * $b.Height)
    [System.Runtime.InteropServices.Marshal]::Copy($data.Scan0, $px, 0, $px.Length)
    $b.UnlockBits($data)
    $r = [SampleShot]::NonBlackBounds($px, $b.Width, $b.Height)
    if (-not $r) { return [System.Drawing.Rectangle]::Empty }
    return [System.Drawing.Rectangle]::FromLTRB($r[0], $r[1], $r[2], $r[3])
}

# WinUI menu, flyout and tooltip windows (Microsoft.UI.Content.PopupWindowSiteBridge)
function Test-Popup([IntPtr]$h) { return [SampleShot]::ClassOf($h) -like "*Popup*" }

# Composite handles (in Z order from top to bottom) at their on-screen positions and save. The blur part (a rectangle in screen coordinates) is blurred
function Save-Windows([IntPtr[]]$handles, [string]$path, [System.Drawing.Rectangle[]]$blur = @()) {
    $layers = @()
    foreach ($h in $handles) {
        $frame = [SampleShot]::FrameOf($h)
        $bitmap = Get-WindowBitmap $h $frame
        $popup = Test-Popup $h
        if ($popup) {
            $bounds = Get-NonBlackBounds $bitmap
            if ($bounds.IsEmpty) { $bitmap.Dispose(); continue }
            $bitmap.MakeTransparent([System.Drawing.Color]::FromArgb(255, 0, 0, 0))
            $extent = [System.Drawing.Rectangle]::new($frame.Left + $bounds.X, $frame.Top + $bounds.Y, $bounds.Width, $bounds.Height)
        } else {
            $extent = [System.Drawing.Rectangle]::new($frame.Left, $frame.Top, $bitmap.Width, $bitmap.Height)
        }
        $layers += [pscustomobject]@{ Bitmap = $bitmap; Left = $frame.Left; Top = $frame.Top; Extent = $extent }
    }
    $union = $layers[0].Extent
    foreach ($layer in $layers) { $union = [System.Drawing.Rectangle]::Union($union, $layer.Extent) }
    $canvas = New-Object System.Drawing.Bitmap $union.Width, $union.Height
    $g = [System.Drawing.Graphics]::FromImage($canvas)
    $g.Clear([System.Drawing.Color]::White)
    # Draw from the bottom window
    for ($i = $layers.Count - 1; $i -ge 0; $i--) {
        $layer = $layers[$i]
        $g.DrawImage($layer.Bitmap, [int]($layer.Left - $union.Left), [int]($layer.Top - $union.Top), $layer.Bitmap.Width, $layer.Bitmap.Height)
        $layer.Bitmap.Dispose()
    }
    # Shrink and enlarge again to blur the text so that it cannot be read
    foreach ($rect in $blur) {
        $area = [System.Drawing.Rectangle]::Intersect([System.Drawing.Rectangle]::new($rect.X - $union.Left, $rect.Y - $union.Top, $rect.Width, $rect.Height),
            [System.Drawing.Rectangle]::new(0, 0, $canvas.Width, $canvas.Height))
        if ($area.Width -le 0 -or $area.Height -le 0) { continue }
        $part = $canvas.Clone($area, $canvas.PixelFormat)
        $small = New-Object System.Drawing.Bitmap ([Math]::Max(1, [int]($area.Width / 24))), ([Math]::Max(1, [int]($area.Height / 24)))
        $sg = [System.Drawing.Graphics]::FromImage($small)
        $sg.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBilinear
        $sg.DrawImage($part, 0, 0, $small.Width, $small.Height)
        $sg.Dispose(); $part.Dispose()
        $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
        $g.SetClip($area)
        $g.DrawImage($small, $area)
        $g.ResetClip()
        $small.Dispose()
    }
    $g.Dispose()
    New-Item -ItemType Directory -Force (Split-Path $path) | Out-Null
    $canvas.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $canvas.Dispose()
    return $layers.Count
}

function Send-Key([IntPtr]$window, [int]$vk, [int]$scan) {
    $target = [SampleShot]::FindDeep($window, "InputSiteWindowClass")
    if ($target -eq [IntPtr]::Zero) { $target = $window }
    [void][SampleShot]::PostMessageW($target, 0x0100, [IntPtr]$vk, [IntPtr](1 + ($scan -shl 16))) # WM_KEYDOWN
    [void][SampleShot]::PostMessageW($target, 0x0101, [IntPtr]$vk, [IntPtr]([int64]0xC0000001 + ($scan -shl 16))) # WM_KEYUP
}

function Get-Steps([string]$key) {
    foreach ($pattern in $ActionTable.Keys) {
        if ($key -like $pattern) { return @($ActionTable[$pattern] -split ',' | ForEach-Object { $_.Trim() }) }
    }
    return @()
}

$sources = Get-ChildItem $Samples -Directory | Where-Object { $_.Name -like $Component } |
    ForEach-Object { Get-ChildItem $_.FullName -Recurse -Filter *.java } |
    Where-Object { $Only.Count -eq 0 -or $Only -contains $_.BaseName } | Sort-Object FullName
if (-not $sources) { throw "No samples found (-Component $Component -Only $($Only -join ','))" }
$failed = @()
foreach ($source in $sources) {
    # <component>/<use case>/<name>.java
    $componentDir = $source.Directory.Parent
    $component = $componentDir.Name
    $key = "$component/$($source.BaseName)"
    $steps = Get-Steps $key
    $out = "$OutRoot\$($ImageDirs[$component])\$($source.BaseName).png"
    Write-Host "$component/$($source.Directory.Name)/$($source.Name)$(if ($steps) { " [$($steps -join ',')]" })"
    $process = Start-Process -FilePath "java" -PassThru -WindowStyle Normal -WorkingDirectory $componentDir.FullName -ArgumentList @(
        "--enable-native-access=ALL-UNNAMED", "-cp", "`"$($Jar.FullName)`"", "`"$($source.FullName)`""
    )
    try {
        # Give up $TimeoutSeconds seconds after launch
        $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
        $handles = @()
        while ((Get-Date) -lt $deadline -and -not $process.HasExited) {
            $handles = @([SampleShot]::WindowsOf([uint32]$process.Id))
            if ($handles.Count -gt 0) { break }
            Start-Sleep -Milliseconds 200
        }
        if ($handles.Count -eq 0) {
            Write-Host "  No window appeared" -ForegroundColor Red
            $failed += $key
            continue
        }
        Start-Sleep -Milliseconds $SettleMillis
        foreach ($action in @($steps | Where-Object { $_ -in "close", "closetop", "enter", "esc" })) {
            $now = @([SampleShot]::WindowsOf([uint32]$process.Id))
            if ($now.Count -eq 0) { break }
            switch ($action) {
                "close" { [void][SampleShot]::PostMessageW($now[$now.Count - 1], 0x0010, [IntPtr]::Zero, [IntPtr]::Zero) } # WM_CLOSE
                "closetop" { [void][SampleShot]::PostMessageW($now[0], 0x0010, [IntPtr]::Zero, [IntPtr]::Zero) }
                "enter" { Send-Key $now[0] 0x0D 0x1C }
                "esc" { Send-Key $now[0] 0x1B 0x01 }
            }
            Start-Sleep -Milliseconds $SettleMillis
        }
        if ((Get-Date) -ge $deadline) {
            Write-Host "  Timed out" -ForegroundColor Red
            $failed += $key
            continue
        }
        $handles = @([SampleShot]::WindowsOf([uint32]$process.Id))
        if ($steps -contains "nopopup") { $handles = @($handles | Where-Object { -not (Test-Popup $_) }) }
        if ($steps -contains "top") { $handles = @($handles | Select-Object -First 1) }
        if ($handles.Count -eq 0) {
            Write-Host "  The window disappeared" -ForegroundColor Red
            $failed += $key
            continue
        }
        $classes = ($handles | ForEach-Object { [SampleShot]::ClassOf($_) }) -join ', '
        $blur = @()
        if ($steps -contains "blurnav") {
            $tree = [SampleShot]::FindDeep($handles[0], "SysTreeView32")
            if ($tree -eq [IntPtr]::Zero -or -not [SampleShot]::IsWindowVisible($tree)) {
                Write-Host "  The list on the left (SysTreeView32) was not found" -ForegroundColor Red
                $failed += $key
                continue
            }
            $r = New-Object SampleShot+RECT
            [void][SampleShot]::GetWindowRect($tree, [ref]$r)
            $blur = @([System.Drawing.Rectangle]::FromLTRB($r.Left, $r.Top, $r.Right, $r.Bottom))
        }
        $count = Save-Windows $handles $out $blur
        Write-Host "  -> $out (windows $count`: $classes)"
    } finally {
        if (-not $process.HasExited) { Stop-Process -Id $process.Id -Force }
    }
}
if ($failed.Count -gt 0) { throw "Samples that could not be captured: $($failed -join ', ')" }
