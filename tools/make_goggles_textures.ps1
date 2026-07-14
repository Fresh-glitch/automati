Add-Type -AssemblyName System.Drawing

$texDir = "P:\ClaudeMods\ExampleMod\my-first-mod\src\main\resources\assets\automati\textures"
$previewDir = "$env:TEMP\automati-previews"
New-Item -ItemType Directory -Force "$texDir\item" | Out-Null
New-Item -ItemType Directory -Force "$texDir\misc" | Out-Null
New-Item -ItemType Directory -Force $previewDir | Out-Null

function C($a,$r,$g,$b) { [System.Drawing.Color]::FromArgb($a,$r,$g,$b) }

# ---- item icon: twin copper-framed lenses with a strap ----
$frame = C 255 196 124 58; $frameD = C 255 142 84 36
$lens = C 255 120 210 220; $lensHi = C 255 190 240 245
$strap = C 255 72 58 46

$icon = New-Object System.Drawing.Bitmap 16,16
# strap across the middle
for ($x = 0; $x -lt 16; $x++) { $icon.SetPixel($x, 7, $strap); $icon.SetPixel($x, 8, $strap) }
# two lenses: left centred at (4.5,7.5), right at (11.5,7.5), radius ~3.4
foreach ($cx in @(4.5, 11.5)) {
  for ($y = 4; $y -le 11; $y++) {
    for ($x = 1; $x -le 14; $x++) {
      $d = [Math]::Sqrt(($x - $cx) * ($x - $cx) + ($y - 7.5) * ($y - 7.5))
      if ($d -le 2.2) {
        $icon.SetPixel($x, $y, $lens)
      } elseif ($d -le 3.4) {
        $icon.SetPixel($x, $y, $(if ($y -lt 7) { $frame } else { $frameD }))
      }
    }
  }
}
# lens glints
$icon.SetPixel(4, 6, $lensHi); $icon.SetPixel(11, 6, $lensHi)
# bridge between the lenses
$icon.SetPixel(7, 6, $frame); $icon.SetPixel(8, 6, $frame)
$icon.Save("$texDir\item\engineers_goggles.png", [System.Drawing.Imaging.ImageFormat]::Png)

# ---- camera overlay: subtle lens vignette, mostly transparent ----
$size = 128
$blur = New-Object System.Drawing.Bitmap $size,$size
$cx = ($size - 1) / 2.0; $cy = ($size - 1) / 2.0
$maxD = [Math]::Sqrt($cx * $cx + $cy * $cy)
for ($y = 0; $y -lt $size; $y++) {
  for ($x = 0; $x -lt $size; $x++) {
    $d = [Math]::Sqrt(($x - $cx) * ($x - $cx) + ($y - $cy) * ($y - $cy)) / $maxD
    # transparent centre, darkening toward corners with a faint teal cast
    $alpha = 0
    if ($d -gt 0.62) { $alpha = [int](($d - 0.62) / 0.38 * 150) }
    # thin teal ring suggesting the lens edge
    if ($d -gt 0.58 -and $d -le 0.62) { $blur.SetPixel($x, $y, (C 42 64 160 170)) ; continue }
    $blur.SetPixel($x, $y, (C ([Math]::Min(150, $alpha)) 18 26 30))
  }
}
$blur.Save("$texDir\misc\goggles_blur.png", [System.Drawing.Imaging.ImageFormat]::Png)

# previews
$big = New-Object System.Drawing.Bitmap 160,160
$g = [System.Drawing.Graphics]::FromImage($big)
$g.Clear([System.Drawing.Color]::FromArgb(255, 120, 140, 120))
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$g.DrawImage($icon, 0, 0, 160, 160)
$g.Dispose()
$big.Save("$previewDir\goggles_icon_preview.png", [System.Drawing.Imaging.ImageFormat]::Png)
$big.Dispose(); $icon.Dispose(); $blur.Dispose()
Write-Output "goggles textures written"
