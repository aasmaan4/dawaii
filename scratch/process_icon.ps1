Add-Type -AssemblyName System.Drawing

$srcPath = "C:\Users\aasma\.gemini\antigravity-ide\brain\a38c43e7-f278-4407-8708-ad4fcb7ab12b\.user_uploaded\media_1790791006822.png"
$src = [System.Drawing.Bitmap]::FromFile($srcPath)

$minX = 456
$maxX = 568
$minY = 124
$maxY = 329

$capW = $maxX - $minX
$capH = $maxY - $minY

# Tight crop around capsule/timer graphic
$cropRect = New-Object System.Drawing.Rectangle($minX, $minY, $capW, $capH)
$cropped = $src.Clone($cropRect, $src.PixelFormat)

# Background color
$bgSampleX = [int]($minX + $capW / 2)
$bgSampleY = [Math]::Max(5, $minY - 15)
$bgColor = $src.GetPixel($bgSampleX, $bgSampleY)
Write-Output "Selected Background Color: R=$($bgColor.R), G=$($bgColor.G), B=$($bgColor.B)"

function MakePaddedIcon($targetSize, $outPath) {
    $bmp = New-Object System.Drawing.Bitmap($targetSize, $targetSize)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    
    # Fill background with exact matching background color
    $brush = New-Object System.Drawing.SolidBrush($bgColor)
    $g.FillRectangle($brush, 0, 0, $targetSize, $targetSize)
    
    # Scale central capsule/timer graphic up so it occupies at least 85% of the viewport
    $contentBoxSize = $targetSize * 0.86
    $scale = [Math]::Min($contentBoxSize / $cropped.Width, $contentBoxSize / $cropped.Height)
    $drawW = [int]($cropped.Width * $scale)
    $drawH = [int]($cropped.Height * $scale)
    $drawX = [int](($targetSize - $drawW) / 2)
    $drawY = [int](($targetSize - $drawH) / 2)
    
    $destRect = New-Object System.Drawing.Rectangle($drawX, $drawY, $drawW, $drawH)
    $g.DrawImage($cropped, $destRect)
    
    $g.Dispose()
    $bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Output "Saved: $outPath ($targetSize x $targetSize, graphic occupies 86% viewport)"
}

# Web icons
MakePaddedIcon 512 "C:\Users\aasma\.gemini\antigravity-ide\scratch\dawaii\web\icon-512.png"
MakePaddedIcon 192 "C:\Users\aasma\.gemini\antigravity-ide\scratch\dawaii\web\icon.png"
MakePaddedIcon 192 "C:\Users\aasma\.gemini\antigravity-ide\scratch\dawaii\web\favicon.png"

# Also generate favicon.ico
Copy-Item "C:\Users\aasma\.gemini\antigravity-ide\scratch\dawaii\web\favicon.png" "C:\Users\aasma\.gemini\antigravity-ide\scratch\dawaii\web\favicon.ico" -Force

# Android icons
MakePaddedIcon 512 "C:\Users\aasma\.gemini\antigravity-ide\scratch\dawaii\android\app\src\main\res\drawable\ic_launcher.png"
MakePaddedIcon 512 "C:\Users\aasma\.gemini\antigravity-ide\scratch\dawaii\android\app\src\main\res\drawable\ic_launcher_foreground.png"

# Mipmap icons
$mipmaps = @{
    "mipmap-mdpi" = 48;
    "mipmap-hdpi" = 72;
    "mipmap-xhdpi" = 96;
    "mipmap-xxhdpi" = 144;
    "mipmap-xxxhdpi" = 192
}

foreach ($entry in $mipmaps.GetEnumerator()) {
    $dir = "C:\Users\aasma\.gemini\antigravity-ide\scratch\dawaii\android\app\src\main\res\$($entry.Key)"
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
    MakePaddedIcon $entry.Value "$dir\ic_launcher.png"
    MakePaddedIcon $entry.Value "$dir\ic_launcher_round.png"
}

$cropped.Dispose()
$src.Dispose()
Write-Output "All icons re-scaled with capsule/timer occupying 86% of viewport!"
