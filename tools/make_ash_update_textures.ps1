# Generates the Ash update textures:
#   items: scf, washed_ash, industrial_fertilizer, ash_clay_blend,
#          ash_brick, sintered_ash_pellet
#   blocks: ash_bricks, road_base, fly_ash_glass (translucent!),
#           ash_rich_soil_top
Add-Type -AssemblyName System.Drawing

$texDir = "P:\ClaudeMods\ExampleMod\my-first-mod\src\main\resources\assets\automati\textures"
$scratch = "$env:TEMP\automati-previews"

function C($r,$g,$b) { [System.Drawing.Color]::FromArgb(255,$r,$g,$b) }
function CA($a,$r,$g,$b) { [System.Drawing.Color]::FromArgb($a,$r,$g,$b) }

$rng = New-Object System.Random(1717)

# ---- powder pile helper: a soft heap silhouette with speckle ----
function Draw-Pile($bmp, $baseR, $baseG, $baseB, $jit, $fleckColor, $fleckMod) {
  for ($py = 6; $py -lt 14; $py++) {
    $halfWidth = [int](1 + ($py - 6) * 0.8)
    for ($px = (8 - $halfWidth); $px -le (7 + $halfWidth); $px++) {
      $n = $rng.Next((-1 * $jit), $jit + 1)
      $shade = if ($py -ge 12) { -18 } elseif ($py -le 7) { 12 } else { 0 }
      $c = C ([Math]::Max(0,[Math]::Min(255,$baseR+$n+$shade))) ([Math]::Max(0,[Math]::Min(255,$baseG+$n+$shade))) ([Math]::Max(0,[Math]::Min(255,$baseB+$n+$shade)))
      if ($fleckColor -and ((($px * 7 + $py * 5) % $fleckMod) -eq 0)) { $c = $fleckColor }
      $bmp.SetPixel($px, $py, $c)
    }
  }
}

function New-Sprite { New-Object System.Drawing.Bitmap 16,16 }
function Save-Item($bmp, $itemName) { $bmp.Save("$texDir\item\$itemName.png", [System.Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose() }

# SCF: pale grey-green mineral powder (silicon-calcium)
$scf = New-Sprite; Draw-Pile $scf 186 196 178 8 (C 214 224 200) 11; Save-Item $scf "scf"
# Washed ash: clean light-grey powder (toxics leached out)
$washed = New-Sprite; Draw-Pile $washed 176 176 176 7 (C 205 205 205) 13; Save-Item $washed "washed_ash"
# Industrial fertilizer: dark rich granules with green flecks
$fert = New-Sprite; Draw-Pile $fert 96 82 66 9 (C 110 168 76) 7; Save-Item $fert "industrial_fertilizer"
# Ash-clay blend: grey-brown wet lump (rounder pile)
$blend = New-Sprite; Draw-Pile $blend 148 132 118 8 (C 120 104 92) 9; Save-Item $blend "ash_clay_blend"

# Ash brick: dark rectangular fired brick
$brick = New-Sprite
for ($py = 5; $py -le 11; $py++) {
  for ($px = 3; $px -le 12; $px++) {
    $c = C 84 82 84
    if ($py -eq 5 -or $px -eq 3) { $c = C 104 102 104 }
    if ($py -eq 11 -or $px -eq 12) { $c = C 58 56 58 }
    if ((($px * 3 + $py * 7) % 11) -eq 0) { $c = C 92 90 94 }
    $brick.SetPixel($px, $py, $c)
  }
}
Save-Item $brick "ash_brick"

# Sintered ash pellets: three dark rounded nuggets
$pellet = New-Sprite
foreach ($ctr in @(@(5,6), @(10,7), @(7,11))) {
  for ($py = ($ctr[1]-2); $py -le ($ctr[1]+2); $py++) {
    for ($px = ($ctr[0]-2); $px -le ($ctr[0]+2); $px++) {
      $dx = $px - $ctr[0]; $dy = $py - $ctr[1]
      if (($dx*$dx + $dy*$dy) -le 4) {
        $c = C 66 64 68
        if ($dy -lt 0) { $c = C 88 86 92 }
        if ($dy -gt 1) { $c = C 48 46 50 }
        $pellet.SetPixel($px, $py, $c)
      }
    }
  }
}
Save-Item $pellet "sintered_ash_pellet"

# ---- block: ash bricks (2x2 brick courses, dark sooty masonry) ----
$bricksBlk = New-Object System.Drawing.Bitmap 16,16
$mortar = C 52 50 52
for ($py = 0; $py -lt 16; $py++) {
  for ($px = 0; $px -lt 16; $px++) {
    $row = [int]([Math]::Floor($py / 4))
    $shift = if (($row % 2) -eq 0) { 0 } else { 4 }
    $inMortarY = (($py % 4) -eq 3)
    $inMortarX = ((($px + $shift) % 8) -eq 7)
    if ($inMortarY -or $inMortarX) { $bricksBlk.SetPixel($px, $py, $mortar); continue }
    $n = $rng.Next(-6, 7)
    $shade = if (($py % 4) -eq 0) { 10 } elseif (($py % 4) -eq 2) { -8 } else { 0 }
    $bricksBlk.SetPixel($px, $py, (C ([Math]::Max(0,84+$n+$shade)) ([Math]::Max(0,82+$n+$shade)) ([Math]::Max(0,86+$n+$shade))))
  }
}
$bricksBlk.Save("$texDir\block\ash_bricks.png", [System.Drawing.Imaging.ImageFormat]::Png)

# ---- block: road base (compacted ash-bound gravel, flat and even) ----
$road = New-Object System.Drawing.Bitmap 16,16
for ($py = 0; $py -lt 16; $py++) {
  for ($px = 0; $px -lt 16; $px++) {
    $n = $rng.Next(-9, 10)
    $c = C ([Math]::Max(0,124+$n)) ([Math]::Max(0,122+$n)) ([Math]::Max(0,120+$n))
    if ((($px * 5 + $py * 3) % 17) -eq 0) { $c = C 96 94 92 }    # embedded pebbles
    elseif ((($px * 3 + $py * 11) % 19) -eq 0) { $c = C 150 148 146 }
    $road.SetPixel($px, $py, $c)
  }
}
$road.Save("$texDir\block\road_base.png", [System.Drawing.Imaging.ImageFormat]::Png)

# ---- block: fly-ash glass (smoky translucent, alpha ~150, darker frame) ----
$glass = New-Object System.Drawing.Bitmap 16,16
for ($py = 0; $py -lt 16; $py++) {
  for ($px = 0; $px -lt 16; $px++) {
    $edge = ($px -eq 0 -or $py -eq 0 -or $px -eq 15 -or $py -eq 15)
    if ($edge) { $glass.SetPixel($px, $py, (CA 235 38 36 42)) }
    else {
      $n = $rng.Next(-6, 7)
      # faint diagonal sheen band
      $sheen = ((($px + $py) % 16) -in 4..5)
      $alpha = if ($sheen) { 120 } else { 150 }
      $tone = if ($sheen) { 26 } else { 12 }
      $glass.SetPixel($px, $py, (CA $alpha ([Math]::Max(0,44+$n+$tone)) ([Math]::Max(0,42+$n+$tone)) ([Math]::Max(0,48+$n+$tone))))
    }
  }
}
$glass.Save("$texDir\block\fly_ash_glass.png", [System.Drawing.Imaging.ImageFormat]::Png)

# ---- block: ash-rich soil top (dark humus flecked with pale ash + SCF) ----
$soil = New-Object System.Drawing.Bitmap 16,16
for ($py = 0; $py -lt 16; $py++) {
  for ($px = 0; $px -lt 16; $px++) {
    $n = $rng.Next(-8, 9)
    $c = C ([Math]::Max(0,72+$n)) ([Math]::Max(0,56+$n)) ([Math]::Max(0,44+$n))
    if ((($px * 7 + $py * 3) % 13) -eq 0) { $c = C 168 168 164 }   # ash flecks
    elseif ((($px * 5 + $py * 9) % 21) -eq 0) { $c = C 186 196 178 } # SCF flecks
    $soil.SetPixel($px, $py, $c)
  }
}
$soil.Save("$texDir\block\ash_rich_soil_top.png", [System.Drawing.Imaging.ImageFormat]::Png)

# previews
function Save-Preview($bmp, $scale, $path) {
  $big = New-Object System.Drawing.Bitmap ($bmp.Width * $scale), ($bmp.Height * $scale)
  $gg = [System.Drawing.Graphics]::FromImage($big)
  $gg.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
  $gg.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
  $gg.DrawImage($bmp, 0, 0, $big.Width, $big.Height)
  $gg.Dispose()
  $big.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
  $big.Dispose()
}
Save-Preview $bricksBlk 8 "$scratch\ash_bricks_preview.png"
Save-Preview $road 8 "$scratch\road_base_preview.png"
Save-Preview $glass 8 "$scratch\fly_ash_glass_preview.png"
Save-Preview $soil 8 "$scratch\ash_rich_soil_preview.png"
$bricksBlk.Dispose(); $road.Dispose(); $glass.Dispose(); $soil.Dispose()
Write-Output "ash update textures written"
