Add-Type -AssemblyName System.Drawing

$srcPath = "C:\Users\aasma\.gemini\antigravity-ide\brain\a38c43e7-f278-4407-8708-ad4fcb7ab12b\.user_uploaded\media_1790801585082.jpg"
$bmp = [System.Drawing.Bitmap]::FromFile($srcPath)

Write-Output "Image loaded: $($bmp.Width) x $($bmp.Height)"

# Sample a few pixels
$p1 = $bmp.GetPixel(10, 10)
$p2 = $bmp.GetPixel(500, 500)
$p3 = $bmp.GetPixel($bmp.Width - 20, 20)

Write-Output "Corner pixel (10,10): R=$($p1.R) G=$($p1.G) B=$($p1.B)"
Write-Output "Center pixel (500,500): R=$($p2.R) G=$($p2.G) B=$($p2.B)"
Write-Output "Top-right pixel: R=$($p3.R) G=$($p3.G) B=$($p3.B)"

$bmp.Dispose()
