Add-Type -AssemblyName System.Drawing

$out = "P:\ClaudeMods\ExampleMod\my-first-mod\src\main\resources\assets\automati\textures\block\coal_generator_top.png"
$preview = "$env:TEMP\automati-previews\gen_top_preview.png"

function C($r,$g,$b) { [System.Drawing.Color]::FromArgb(255,$r,$g,$b) }

# Iron palette (matches the casing)
$K = C 62 66 71; $D = C 118 124 130; $M = C 154 160 166; $L = C 188 194 199; $R = C 220 225 229
$softD = C 140 146 152; $softL = C 172 178 184
$hole = C 24 26 29          # exhaust throat
$slat = C 96 101 107        # grille slats over the opening
$soot = C 120 124 128       # light soot staining

$bmp = New-Object System.Drawing.Bitmap 16,16

for ($y = 0; $y -lt 16; $y++) {
  for ($x = 0; $x -lt 16; $x++) {
    # riveted plate base, same language as the casing
    $c = $M
    if ((($x * 3 + $y * 5) % 9) -eq 0) { $c = $softD }
    elseif ((($x * 5 + $y * 3) % 13) -eq 0) { $c = $softL }
    if ($y -eq 0 -or $x -eq 0) { $c = $L }
    if ($y -eq 15 -or $x -eq 15) { $c = $K }

    # circular exhaust stack, centered so it works for every facing
    $dx = $x - 7.5; $dy = $y - 7.5
    $r2 = [Math]::Sqrt($dx * $dx + $dy * $dy)

    if ($r2 -le 5.4 -and $r2 -gt 4.4) {
      # raised collar: lit from the top-left like everything else
      if (($dx + $dy) -lt -1.5) { $c = $L }
      elseif (($dx + $dy) -gt 1.5) { $c = $K }
      else { $c = $D }
    }
    elseif ($r2 -le 4.4 -and $r2 -gt 3.2) {
      # inner rim ring, shadowed opposite the collar highlight
      $c = if (($dx + $dy) -lt 0) { $K } else { $D }
    }
    elseif ($r2 -le 3.2) {
      # dark throat with protective grille slats
      $c = $hole
      if ($y -eq 6 -or $y -eq 9) { $c = $slat }
    }
    elseif ($r2 -le 6.8 -and ((($x * 7 + $y * 11) % 13) -eq 0)) {
      # scattered soot staining around the stack
      $c = $soot
    }

    $bmp.SetPixel($x, $y, $c)
  }
}

# corner rivets, matching the casing plate
foreach ($p in @(@(2,2), @(12,2), @(2,12), @(12,12))) {
  $bmp.SetPixel($p[0], $p[1], $R); $bmp.SetPixel($p[0]+1, $p[1], $D)
  $bmp.SetPixel($p[0], $p[1]+1, $D); $bmp.SetPixel($p[0]+1, $p[1]+1, $K)
}

$bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)

$big = New-Object System.Drawing.Bitmap 160,160
$g = [System.Drawing.Graphics]::FromImage($big)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$g.DrawImage($bmp, 0, 0, 160, 160)
$g.Dispose()
$big.Save($preview, [System.Drawing.Imaging.ImageFormat]::Png)
$big.Dispose(); $bmp.Dispose()
Write-Output "top texture written: $out"

