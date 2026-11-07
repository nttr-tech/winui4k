# Draws the samples' icons/*.png (small images read with ImageIcon / WImageIcon).
# Usage (from the repository root; the output folder is src/main/java/com/appkitbox/winui4k/swing_to_winui4k/<component>/icons; only person.png is placed in jlist/icons):
#   pwsh winui4k-sample-swing-to-winui4k\make_icons.ps1 -Out winui4k-sample-swing-to-winui4k\src\main\java\com\appkitbox\winui4k\swing_to_winui4k\jbutton\icons
param([string]$Out)
Add-Type -AssemblyName System.Drawing
New-Item -ItemType Directory -Force $Out | Out-Null

function New-Icon([string]$name, [scriptblock]$draw) {
    $b = New-Object System.Drawing.Bitmap 20, 20
    $g = [System.Drawing.Graphics]::FromImage($b)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.Clear([System.Drawing.Color]::Transparent)
    & $draw $g
    $g.Dispose()
    $b.Save((Join-Path $Out "$name.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $b.Dispose()
}

# Save: a blue floppy disk
New-Icon "save" {
    param($g)
    $g.FillRectangle((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(0, 103, 192))), 2, 2, 16, 16)
    $g.FillRectangle([System.Drawing.Brushes]::White, 5, 2, 10, 6)
    $g.FillRectangle([System.Drawing.Brushes]::White, 5, 11, 10, 7)
}
# Delete: a red ×
New-Icon "delete" {
    param($g)
    $pen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(196, 43, 28)), 3
    $g.DrawLine($pen, 4, 4, 16, 16)
    $g.DrawLine($pen, 16, 4, 4, 16)
}
# Download: a green downward arrow
New-Icon "download" {
    param($g)
    $brush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(16, 124, 16))
    $g.FillRectangle($brush, 8, 2, 4, 9)
    $points = [System.Drawing.Point[]]@((New-Object System.Drawing.Point 3, 10), (New-Object System.Drawing.Point 17, 10), (New-Object System.Drawing.Point 10, 17))
    $g.FillPolygon($brush, $points)
    $g.FillRectangle($brush, 3, 17, 14, 2)
}
# Person: a gray head and shoulders
New-Icon "person" {
    param($g)
    $brush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(96, 96, 96))
    $g.FillEllipse($brush, 6, 2, 8, 8)
    $g.FillPie($brush, 2, 11, 16, 16, 180, 180)
}
