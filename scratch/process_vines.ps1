Add-Type -AssemblyName System.Drawing

$srcPath = "C:\Users\aasma\.gemini\antigravity-ide\brain\a38c43e7-f278-4407-8708-ad4fcb7ab12b\.user_uploaded\media_1790801585082.jpg"
$src = [System.Drawing.Bitmap]::FromFile($srcPath)
$width = $src.Width
$height = $src.Height

# Create 32-bit ARGB result bitmap
$dest = New-Object System.Drawing.Bitmap($width, $height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

# Lock bits for fast processing
$rect = New-Object System.Drawing.Rectangle(0, 0, $width, $height)
$srcData = $src.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$destData = $dest.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::WriteOnly, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

$byteCount = $width * $height * 4
$srcBytes = New-Object byte[] $byteCount
$destBytes = New-Object byte[] $byteCount

[System.Runtime.InteropServices.Marshal]::Copy($srcData.Scan0, $srcBytes, 0, $byteCount)

for ($i = 0; $i -lt $byteCount; $i += 4) {
    $b = [int]$srcBytes[$i]
    $g = [int]$srcBytes[$i + 1]
    $r = [int]$srcBytes[$i + 2]
    # $srcBytes[$i + 3] is alpha

    $maxC = [Math]::Max($r, [Math]::Max($g, $b))
    $minC = [Math]::Min($r, [Math]::Min($g, $b))
    $sat = $maxC - $minC
    $lum = [int](0.299 * $r + 0.587 * $g + 0.114 * $b)

    # Detect white/light-gray background
    if ($lum -gt 170 -and $sat -lt 45) {
        # Smooth alpha falloff for anti-aliasing near edges
        if ($lum -ge 205) {
            $destBytes[$i] = 0
            $destBytes[$i + 1] = 0
            $destBytes[$i + 2] = 0
            $destBytes[$i + 3] = 0
            continue
        } else {
            # Intermediate alpha falloff
            $alphaRatio = 1.0 - (($lum - 170) / 35.0)
            $alpha = [byte][Math]::Max(0, [Math]::Min(255, [int]($alphaRatio * 255)))
        }
    } else {
        $alpha = [byte]255
    }

    # Color neutralization according to Sakura dark theme:
    # Determine whether pixel is part of a flower (red/orange/yellow) or leaves (green)
    $isFlower = ($r -gt $g + 15) -or ($r -gt 130 -and $b -lt 110)
    
    if ($isFlower) {
        # Neutralize bright red/orange into dusty rose / blush pink (#FFB7C5 / #8A4F5D)
        $norm = $lum / 255.0
        # Deep muted plum shadow to delicate blush pink highlight
        $outR = [byte][Math]::Min(255, [int](75 + (245 - 75) * $norm))
        $outG = [byte][Math]::Min(255, [int](40 + (175 - 40) * $norm))
        $outB = [byte][Math]::Min(255, [int](55 + (195 - 55) * $norm))
    } else {
        # Leaves & stems: neutralize into muted dark slate-charcoal with subtle earthy green tone
        $norm = $lum / 255.0
        $outR = [byte][Math]::Min(255, [int](35 + (110 - 35) * $norm))
        $outG = [byte][Math]::Min(255, [int](45 + (135 - 45) * $norm))
        $outB = [byte][Math]::Min(255, [int](40 + (120 - 40) * $norm))
    }

    # If alpha was reduced near edge, apply it
    $destBytes[$i] = $outB
    $destBytes[$i + 1] = $outG
    $destBytes[$i + 2] = $outR
    $destBytes[$i + 3] = $alpha
}

[System.Runtime.InteropServices.Marshal]::Copy($destBytes, 0, $destData.Scan0, $byteCount)

$src.UnlockBits($srcData)
$dest.UnlockBits($destData)
$src.Dispose()

# Save neutralized floral overlay to web and android directories
$webPath = "C:\Users\aasma\.gemini\antigravity-ide\scratch\dawaii\web\bg_floral_overlay.png"
$androidPath = "C:\Users\aasma\.gemini\antigravity-ide\scratch\dawaii\android\app\src\main\res\drawable\bg_floral_overlay.png"

$dest.Save($webPath, [System.Drawing.Imaging.ImageFormat]::Png)
$dest.Save($androidPath, [System.Drawing.Imaging.ImageFormat]::Png)

$dest.Dispose()
Write-Output "Successfully generated neutralized transparent floral overlay at:"
Write-Output " - $webPath"
Write-Output " - $androidPath"
