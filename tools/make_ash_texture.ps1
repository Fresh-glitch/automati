Add-Type -AssemblyName System.Drawing

$out = "P:\ClaudeMods\ExampleMod\my-first-mod\src\main\resources\assets\automati\textures\item\ash.png"
$preview = "$env:TEMP\automati-previews\ash_preview.png"
New-Item -ItemType Directory -Force (Split-Path $out) | Out-Null

function C($r,$g,$b) { [System.Drawing.Color]::FromArgb(255,$r,$g,$b) }

# A soft heap of powdery ash on a transparent background
$dark = C 96 98 102; $mid = C 128 131 135; $light = C 158 161 165; $fleck = C 186 189 193

$bmp = New-Object System.Drawing.Bitmap 16,16   # starts fully transparent

# mound: widens toward the bottom, with a ragged top edge
for ($y = 6; $y -le 13; $y++) {
  $half = [Math]::Min(6.2, ($y - 5) * 0.95 + 0.5)
  for ($x = 0; $x -lt 16; $x++) {
    $d = [Math]::Abs($x - 7.5)
    if ($d -le $half) {
      # ragged silhouette: skip an occasional edge pixel on the upper rows
      if ($y -le 8 -and ($d -gt $half - 1) -and ((($x * 5 + $y * 7) % 4) -eq 0)) { continue }
      $c = $mid
      if ($d -gt $half - 1.2 -or $y -eq 13) { $c = $dark }          # shadowed edge and base
      elseif ((($x * 3 + $y * 5) % 7) -eq 0) { $c = $light }        # powdery texture
      elseif ((($x * 7 + $y * 3) % 11) -eq 0) { $c = $fleck }       # bright flecks
      $bmp.SetPixel($x, $y, $c)
    }
  }
}
# a few drifting specks above the pile
$bmp.SetPixel(5, 4, $light); $bmp.SetPixel(10, 3, $mid); $bmp.SetPixel(8, 5, $fleck)

$bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)

$big = New-Object System.Drawing.Bitmap 160,160
$g = [System.Drawing.Graphics]::FromImage($big)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$g.DrawImage($bmp, 0, 0, 160, 160)
$g.Dispose()
$big.Save($preview, [System.Drawing.Imaging.ImageFormat]::Png)
$big.Dispose(); $bmp.Dispose()
Write-Output "ash texture written"

