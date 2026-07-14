Add-Type -AssemblyName System.Drawing

$out = "P:\ClaudeMods\ExampleMod\my-first-mod\src\main\resources\assets\automati\textures\block\erg_cable.png"
$previewDir = "$env:TEMP\automati-previews"
New-Item -ItemType Directory -Force $previewDir | Out-Null

function C($r,$g,$b) { [System.Drawing.Color]::FromArgb(255,$r,$g,$b) }

# Cross-layout texture: the model's auto-UV samples the centre square for the
# core and cross-section faces, and the axis strips for the arm sides.
$bg    = C 40 42 47     # unused corners
$insul = C 88 92 99     # insulation sheath
$edge  = C 62 66 71     # sheath edge lines
$cuD   = C 156 92 40    # copper, shadowed winding
$cuL   = C 196 124 58   # copper, lit winding
$cuHot = C 222 152 82   # copper highlight

$bmp = New-Object System.Drawing.Bitmap 16,16
for ($y = 0; $y -lt 16; $y++) {
  for ($x = 0; $x -lt 16; $x++) { $bmp.SetPixel($x, $y, $bg) }
}

# horizontal strip (arm sides along X): rows 6..9
for ($x = 0; $x -lt 16; $x++) {
  $bmp.SetPixel($x, 6, $edge); $bmp.SetPixel($x, 9, $edge)
  $bmp.SetPixel($x, 7, $insul); $bmp.SetPixel($x, 8, $insul)
  # copper pinstripe showing through the sheath every 4px
  if (($x % 4) -eq 1) { $bmp.SetPixel($x, 7, $cuL); $bmp.SetPixel($x, 8, $cuD) }
}
# vertical strip (arm sides along Y/Z): cols 6..9
for ($y = 0; $y -lt 16; $y++) {
  if ($y -ge 6 -and $y -le 9) { continue }  # centre handled below
  $bmp.SetPixel(6, $y, $edge); $bmp.SetPixel(9, $y, $edge)
  $bmp.SetPixel(7, $y, $insul); $bmp.SetPixel(8, $y, $insul)
  if (($y % 4) -eq 1) { $bmp.SetPixel(7, $y, $cuL); $bmp.SetPixel(8, $y, $cuD) }
}

# core square 5..10: exposed copper winding, vertical coils
for ($y = 5; $y -le 10; $y++) {
  for ($x = 5; $x -le 10; $x++) {
    $c = switch ($x % 3) { 0 { $cuD } 1 { $cuL } 2 { $cuHot } }
    if ($y -eq 5 -or $y -eq 10) { $c = $edge }  # core end caps
    $bmp.SetPixel($x, $y, $c)
  }
}

$bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)

$big = New-Object System.Drawing.Bitmap 160,160
$g = [System.Drawing.Graphics]::FromImage($big)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$g.DrawImage($bmp, 0, 0, 160, 160)
$g.Dispose()
$big.Save("$previewDir\erg_cable_preview.png", [System.Drawing.Imaging.ImageFormat]::Png)
$big.Dispose(); $bmp.Dispose()
Write-Output "cable texture written: $out"
