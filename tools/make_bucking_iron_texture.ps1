Add-Type -AssemblyName System.Drawing

# Generates the bucking iron (all-metal maul, heavy head) and the iron stick.
$texDir = "P:\ClaudeMods\ExampleMod\my-first-mod\src\main\resources\assets\automati\textures\item"
$previewDir = "$env:TEMP\automati-previews"
New-Item -ItemType Directory -Force $previewDir | Out-Null

function C($a,$r,$g,$b) { [System.Drawing.Color]::FromArgb($a,$r,$g,$b) }

$outline = C 255 55 58 63
$ironXD = C 255 85 90 96
$ironD = C 255 116 121 127
$iron = C 255 158 164 170
$ironL = C 255 198 203 208
$ironHi = C 255 232 236 240
$gripD = C 255 88 62 40
$grip = C 255 116 84 52

# ---- bucking iron: all-iron handle, massive SOLID maul head ----
$bmp = New-Object System.Drawing.Bitmap 16,16

# handle: 2px diagonal forged rod from (1,14) up to (6,9), darker pommel end
for ($i = 0; $i -le 5; $i++) {
  $x = 1 + $i; $y = 14 - $i
  $pommel = ($i -le 1)
  $bmp.SetPixel($x, $y, $(if ($pommel) { $ironXD } else { $ironD }))
  $bmp.SetPixel($x + 1, $y, $(if ($pommel) { $ironD } else { $iron }))
  if ($y + 1 -le 15) { $bmp.SetPixel($x, $y + 1, $ironXD) }
}

# head: a solid rotated rectangle — every pixel inside the region is filled
# (diagonal-line stacking leaves checkerboard holes; this predicate doesn't).
# Rotated coords: d = x - y picks the band (shading), s = x + y the length.
for ($y = 0; $y -le 15; $y++) {
  for ($x = 0; $x -le 15; $x++) {
    $d = $x - $y; $s = $x + $y
    if ($d -ge -2 -and $d -le 6 -and $s -ge 6 -and $s -le 16) {
      $c = switch ([Math]::Floor(($d + 2) / 2.0)) {
        0 { $ironXD } 1 { $ironD } 2 { $iron } 3 { $ironL } default { $iron }
      }
      if ($s -ge 15) { $c = $ironL }   # polished striking face, front
      if ($s -le 7)  { $c = $ironD }   # rear face in shadow
      $bmp.SetPixel($x, $y, $c)
    }
  }
}
# dark mass outline along the handle-side edge of the head
foreach ($s in @(7, 9, 11, 13, 15)) {
  $x = [int](($s - 3) / 2); $y = $x + 3
  if ($x -ge 0 -and $y -le 15) { $bmp.SetPixel($x, $y, $outline) }
}
# glints on the upper face and the socket where handle meets head
$bmp.SetPixel(9, 3, $ironHi); $bmp.SetPixel(10, 4, $ironHi); $bmp.SetPixel(8, 2, $ironHi)
$bmp.SetPixel(7, 9, $outline)

$bmp.Save("$texDir\bucking_iron.png", [System.Drawing.Imaging.ImageFormat]::Png)

# ---- iron stick: a plain forged rod ----
$rod = New-Object System.Drawing.Bitmap 16,16
for ($i = 0; $i -le 9; $i++) {
  $x = 3 + $i; $y = 12 - $i
  $rod.SetPixel($x, $y, $iron)
  $rod.SetPixel($x + 1, $y, $ironL)
}
# darker tips
$rod.SetPixel(3, 12, $ironXD); $rod.SetPixel(4, 12, $ironD)
$rod.SetPixel(12, 3, $ironD); $rod.SetPixel(13, 3, $ironXD)
$rod.Save("$texDir\iron_stick.png", [System.Drawing.Imaging.ImageFormat]::Png)

# previews
$big = New-Object System.Drawing.Bitmap 336,160
$g = [System.Drawing.Graphics]::FromImage($big)
$g.Clear([System.Drawing.Color]::FromArgb(255, 120, 140, 120))
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$g.DrawImage($bmp, 0, 0, 160, 160)
$g.DrawImage($rod, 176, 0, 160, 160)
$g.Dispose()
$big.Save("$previewDir\bucking_family_preview.png", [System.Drawing.Imaging.ImageFormat]::Png)
$big.Dispose(); $bmp.Dispose(); $rod.Dispose()
Write-Output "bucking iron + iron stick textures written"
