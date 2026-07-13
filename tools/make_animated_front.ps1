Add-Type -AssemblyName System.Drawing

$out = "P:\ClaudeMods\ExampleMod\my-first-mod\src\main\resources\assets\automati\textures\block\coal_generator_front_on.png"
$mcmeta = "$out.mcmeta"
$preview = "$env:TEMP\automati-previews\gen_front_anim_preview.png"

function C($r,$g,$b) { [System.Drawing.Color]::FromArgb(255,$r,$g,$b) }

# Iron palette (identical to the static faces)
$K = C 62 66 71; $D = C 118 124 130; $M = C 154 160 166; $L = C 188 194 199; $R = C 220 225 229
$softD = C 140 146 152; $softL = C 172 178 184

# Per-frame glow colours for the three vent gaps (top y=6, mid y=8, bottom y=10).
# Each row pulses on its own rhythm so the fire feels alive, not strobing.
$glow = @(
  @( (C 255 205 66), (C 244 146 38), (C 205  94 28) ),
  @( (C 255 190 55), (C 252 170 50), (C 215 105 32) ),
  @( (C 255 215 85), (C 238 135 35), (C 198  88 26) ),
  @( (C 250 180 48), (C 250 160 45), (C 222 112 36) )
)
# Bright embers that dance to a different spot each frame: @(x, y) with y in {6,8,10}
$embers = @(
  @( @(5,6),  @(9,10) ),
  @( @(7,8),  @(10,6) ),
  @( @(4,10), @(8,6)  ),
  @( @(6,8),  @(11,10) )
)
$emberColor = C 255 235 150

# 4 frames stacked vertically: 16x64
$bmp = New-Object System.Drawing.Bitmap 16,64

for ($f = 0; $f -lt 4; $f++) {
  $oy = $f * 16
  for ($y = 0; $y -lt 16; $y++) {
    for ($x = 0; $x -lt 16; $x++) {
      # static plate base ??? identical on every frame
      $c = $M
      if ((($x * 3 + $y * 5) % 9) -eq 0) { $c = $softD }
      elseif ((($x * 5 + $y * 3) % 13) -eq 0) { $c = $softL }
      if ($y -eq 0 -or $x -eq 0) { $c = $L }
      if ($y -eq 15 -or $x -eq 15) { $c = $K }
      $bmp.SetPixel($x, $oy + $y, $c)
    }
  }
  foreach ($p in @(@(2,2), @(12,2), @(2,12), @(12,12))) {
    $bmp.SetPixel($p[0], $oy+$p[1], $R); $bmp.SetPixel($p[0]+1, $oy+$p[1], $D)
    $bmp.SetPixel($p[0], $oy+$p[1]+1, $D); $bmp.SetPixel($p[0]+1, $oy+$p[1]+1, $K)
  }
  for ($x = 3; $x -le 12; $x++) { $bmp.SetPixel($x, $oy+4, $K); $bmp.SetPixel($x, $oy+11, $K) }
  for ($y = 4; $y -le 11; $y++) { $bmp.SetPixel(3, $oy+$y, $K); $bmp.SetPixel(12, $oy+$y, $K) }
  for ($y = 5; $y -le 10; $y++) {
    for ($x = 4; $x -le 11; $x++) {
      if ($y % 2 -eq 1) {
        $bmp.SetPixel($x, $oy+$y, $D)   # static slats
      } else {
        $row = ($y - 6) / 2             # 0,1,2 for gap rows
        $bmp.SetPixel($x, $oy+$y, $glow[$f][$row])
      }
    }
  }
  foreach ($e in $embers[$f]) {
    $bmp.SetPixel($e[0], $oy+$e[1], $emberColor)
  }
}

$bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)

# animation control file: 3 ticks per frame, interpolated for a smooth pulse
@'
{
  "animation": {
    "frametime": 3,
    "interpolate": true
  }
}
'@ | Out-File -Encoding ascii $mcmeta

# preview: the 4 frames side by side at 8x
$big = New-Object System.Drawing.Bitmap 544,128
$g = [System.Drawing.Graphics]::FromImage($big)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
for ($f = 0; $f -lt 4; $f++) {
  $src = New-Object System.Drawing.Rectangle 0, ($f*16), 16, 16
  $dst = New-Object System.Drawing.Rectangle ($f*136), 0, 128, 128
  $g.DrawImage($bmp, $dst, $src, [System.Drawing.GraphicsUnit]::Pixel)
}
$g.Dispose()
$big.Save($preview, [System.Drawing.Imaging.ImageFormat]::Png)
$big.Dispose(); $bmp.Dispose()
Write-Output "animated front written: $out"

