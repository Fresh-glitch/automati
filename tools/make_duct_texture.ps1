Add-Type -AssemblyName System.Drawing

$out = "P:\ClaudeMods\ExampleMod\my-first-mod\src\main\resources\assets\automati\textures\block\item_duct.png"
$previewDir = "$env:TEMP\automati-previews"
New-Item -ItemType Directory -Force $previewDir | Out-Null

function C($r,$g,$b) { [System.Drawing.Color]::FromArgb(255,$r,$g,$b) }

# Cross-layout like the cable texture, but the duct's material story is
# stone tube + iron joint bands instead of copper conductor.
$bg     = C 40 42 47
$stoneD = C 118 118 116
$stone  = C 148 148 145
$stoneL = C 172 172 168
$ironB  = C 96 101 108    # joint bands
$ironL  = C 150 156 162
$mouth  = C 30 32 36      # the open duct interior

$bmp = New-Object System.Drawing.Bitmap 16,16
for ($y = 0; $y -lt 16; $y++) {
  for ($x = 0; $x -lt 16; $x++) { $bmp.SetPixel($x, $y, $bg) }
}

# horizontal strip (arm sides along X): rows 6..9
for ($x = 0; $x -lt 16; $x++) {
  $band = (($x % 5) -eq 2)
  $bmp.SetPixel($x, 6, $(if ($band) { $ironB } else { $stoneD }))
  $bmp.SetPixel($x, 7, $(if ($band) { $ironL } else { $stone }))
  $bmp.SetPixel($x, 8, $(if ($band) { $ironL } else { $stone }))
  $bmp.SetPixel($x, 9, $(if ($band) { $ironB } else { $stoneD }))
  if (-not $band -and (($x + 3) % 4) -eq 0) { $bmp.SetPixel($x, 7, $stoneL) }
}
# vertical strip: cols 6..9
for ($y = 0; $y -lt 16; $y++) {
  if ($y -ge 6 -and $y -le 9) { continue }
  $band = (($y % 5) -eq 2)
  $bmp.SetPixel(6, $y, $(if ($band) { $ironB } else { $stoneD }))
  $bmp.SetPixel(7, $y, $(if ($band) { $ironL } else { $stone }))
  $bmp.SetPixel(8, $y, $(if ($band) { $ironL } else { $stone }))
  $bmp.SetPixel(9, $y, $(if ($band) { $ironB } else { $stoneD }))
  if (-not $band -and (($y + 3) % 4) -eq 0) { $bmp.SetPixel(7, $y, $stoneL) }
}

# core square 5..10: iron collar around a dark open mouth
for ($y = 5; $y -le 10; $y++) {
  for ($x = 5; $x -le 10; $x++) {
    $edge = ($x -eq 5 -or $x -eq 10 -or $y -eq 5 -or $y -eq 10)
    $bmp.SetPixel($x, $y, $(if ($edge) { $ironB } else { $mouth }))
  }
}
$bmp.SetPixel(6, 6, $ironL)  # collar glint

$bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)

$big = New-Object System.Drawing.Bitmap 160,160
$g = [System.Drawing.Graphics]::FromImage($big)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$g.DrawImage($bmp, 0, 0, 160, 160)
$g.Dispose()
$big.Save("$previewDir\item_duct_preview.png", [System.Drawing.Imaging.ImageFormat]::Png)
$big.Dispose(); $bmp.Dispose()
Write-Output "item duct texture written"
