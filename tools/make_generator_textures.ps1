Add-Type -AssemblyName System.Drawing

$texDir = "P:\ClaudeMods\ExampleMod\my-first-mod\src\main\resources\assets\automati\textures"
$scratch = "$env:TEMP\automati-previews"
New-Item -ItemType Directory -Force "$texDir\block" | Out-Null
New-Item -ItemType Directory -Force "$texDir\gui\container" | Out-Null

function C($r,$g,$b) { [System.Drawing.Color]::FromArgb(255,$r,$g,$b) }

# Iron palette (matches factory_block)
$K = C 62 66 71; $D = C 118 124 130; $M = C 154 160 166; $L = C 188 194 199; $R = C 220 225 229
$softD = C 140 146 152; $softL = C 172 178 184

# ---- Generator front faces (off and on) ----
function Make-Front($lit, $path) {
  $bmp = New-Object System.Drawing.Bitmap 16,16
  for ($y = 0; $y -lt 16; $y++) {
    for ($x = 0; $x -lt 16; $x++) {
      $c = $M
      if ((($x * 3 + $y * 5) % 9) -eq 0) { $c = $softD }
      elseif ((($x * 5 + $y * 3) % 13) -eq 0) { $c = $softL }
      if ($y -eq 0 -or $x -eq 0) { $c = $L }
      if ($y -eq 15 -or $x -eq 15) { $c = $K }
      $bmp.SetPixel($x, $y, $c)
    }
  }
  # rivets in the corners
  foreach ($p in @(@(2,2), @(12,2), @(2,12), @(12,12))) {
    $bmp.SetPixel($p[0], $p[1], $R); $bmp.SetPixel($p[0]+1, $p[1], $D)
    $bmp.SetPixel($p[0], $p[1]+1, $D); $bmp.SetPixel($p[0]+1, $p[1]+1, $K)
  }
  # vent frame (x 3..12, y 4..11) with slats inside
  for ($x = 3; $x -le 12; $x++) { $bmp.SetPixel($x, 4, $K); $bmp.SetPixel($x, 11, $K) }
  for ($y = 4; $y -le 11; $y++) { $bmp.SetPixel(3, $y, $K); $bmp.SetPixel(12, $y, $K) }
  for ($y = 5; $y -le 10; $y++) {
    for ($x = 4; $x -le 11; $x++) {
      if ($y % 2 -eq 1) {
        $bmp.SetPixel($x, $y, $D)  # metal slat rows
      } else {
        if ($lit) {
          # glowing gaps: hotter near the top of the firebox
          $glow = switch ($y) { 6 { C 255 205 66 } 8 { C 244 146 38 } 10 { C 205 94 28 } }
          $bmp.SetPixel($x, $y, $glow)
        } else {
          $bmp.SetPixel($x, $y, (C 32 34 38))  # cold dark gaps
        }
      }
    }
  }
  $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
  $bmp
}

$front = Make-Front $false "$texDir\block\coal_generator_front.png"
# NOTE: coal_generator_front_on.png is owned by make_animated_front.ps1 (16x64
# animated sheet) ??? do NOT regenerate it here or the animation gets flattened.
$frontOn = $front

# ---- GUI texture (256x256 canvas, 176x166 panel) ----
$gui = New-Object System.Drawing.Bitmap 256,256
$g = [System.Drawing.Graphics]::FromImage($gui)

$body = C 198 198 198; $bevelL = C 255 255 255; $bevelD = C 85 85 85
$slotD = C 55 55 55; $slotL = C 255 255 255; $slotBody = C 139 139 139

# panel body + bevel
$g.FillRectangle((New-Object System.Drawing.SolidBrush $body), 0, 0, 176, 166)
for ($i = 0; $i -lt 3; $i++) {
  $g.FillRectangle((New-Object System.Drawing.SolidBrush $bevelL), 0, $i, 176, 1)   # top
  $g.FillRectangle((New-Object System.Drawing.SolidBrush $bevelL), $i, 0, 1, 166)   # left
  $g.FillRectangle((New-Object System.Drawing.SolidBrush $bevelD), 0, 165-$i, 176, 1) # bottom
  $g.FillRectangle((New-Object System.Drawing.SolidBrush $bevelD), 175-$i, 0, 1, 166) # right
}

function Draw-Slot($x, $y, $w, $h, $interior) {
  # inset box: dark top/left, light bottom/right
  $g.FillRectangle((New-Object System.Drawing.SolidBrush $slotD), $x, $y, $w, 1)
  $g.FillRectangle((New-Object System.Drawing.SolidBrush $slotD), $x, $y, 1, $h)
  $g.FillRectangle((New-Object System.Drawing.SolidBrush $slotL), $x, $y+$h-1, $w, 1)
  $g.FillRectangle((New-Object System.Drawing.SolidBrush $slotL), $x+$w-1, $y, 1, $h)
  $g.FillRectangle((New-Object System.Drawing.SolidBrush $interior), $x+1, $y+1, $w-2, $h-2)
}

# fuel slot (interior at 80,53)
Draw-Slot 79 52 18 18 $slotBody
# ash byproduct slot (interior at 116,53), slightly darker to read as an output bin
Draw-Slot 115 52 18 18 (C 120 120 120)
# player inventory (interiors at 8,84 + 18-grids) and hotbar (8,142)
for ($row = 0; $row -lt 3; $row++) { for ($col = 0; $col -lt 9; $col++) { Draw-Slot (7 + $col*18) (83 + $row*18) 18 18 $slotBody } }
for ($col = 0; $col -lt 9; $col++) { Draw-Slot (7 + $col*18) 141 18 18 $slotBody }
# energy gauge socket: frame at (151,17) 18x52, dark interior (16x50), with a
# ridge line every 5px = every 10,000 E
Draw-Slot 151 17 18 52 (C 43 43 43)
for ($seg = 1; $seg -lt 10; $seg++) {
  $ry = 18 + $seg * 5
  $g.FillRectangle((New-Object System.Drawing.SolidBrush (C 66 66 66)), 152, $ry, 16, 1)
}

# ---- overlay sprites ----
# flame 14x14 at (176,0): orange body, yellow core
for ($y = 0; $y -lt 14; $y++) {
  $half = [Math]::Round(($y + 2) / 14.0 * 6.5)
  for ($x = 0; $x -lt 14; $x++) {
    $dist = [Math]::Abs($x - 6.5)
    if ($dist -le $half) {
      $c = C 226 88 34
      if ($y -ge 5 -and $dist -le ($half - 2)) { $c = C 255 200 60 }
      $gui.SetPixel(176 + $x, $y, $c)
    }
  }
}
# clog warning lamp 8x8 at (192,0): red indicator with a bright glint
for ($y = 0; $y -lt 8; $y++) {
  for ($x = 0; $x -lt 8; $x++) {
    $edge = ($x -eq 0 -or $y -eq 0 -or $x -eq 7 -or $y -eq 7)
    $corner = (($x -eq 0 -or $x -eq 7) -and ($y -eq 0 -or $y -eq 7))
    if ($corner) { continue }  # rounded look, corners stay transparent
    if ($edge) { $gui.SetPixel(192 + $x, $y, (C 110 22 22)) }
    else { $gui.SetPixel(192 + $x, $y, (C 208 44 40)) }
  }
}
$gui.SetPixel(194, 2, (C 255 130 118)); $gui.SetPixel(195, 2, (C 240 90 80))

# energy fill 16x50 at (176,31): vertical amber gradient with left highlight,
# darkened every 5th row so the ridges stay visible through the fill
for ($y = 0; $y -lt 50; $y++) {
  $t = $y / 49.0
  $r2 = [int](255 - $t * 25); $g2 = [int](225 - $t * 87); $b2 = [int](120 - $t * 88)
  $ridge = ($y % 5) -eq 0 -and $y -gt 0
  for ($x = 0; $x -lt 16; $x++) {
    if ($ridge) {
      $c = C ([int]($r2 * 0.72)) ([int]($g2 * 0.72)) ([int]($b2 * 0.72))
    } else {
      $c = C $r2 $g2 $b2
      if ($x -eq 0) { $c = C ([Math]::Min(255, $r2+20)) ([Math]::Min(255, $g2+20)) ([Math]::Min(255, $b2+30)) }
    }
    $gui.SetPixel(176 + $x, 31 + $y, $c)
  }
}

$g.Dispose()
$gui.Save("$texDir\gui\container\coal_generator.png", [System.Drawing.Imaging.ImageFormat]::Png)

# ---- previews ----
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
Save-Preview $front 10 "$scratch\gen_front_preview.png"
Save-Preview $frontOn 10 "$scratch\gen_front_on_preview.png"
$panel = $gui.Clone((New-Object System.Drawing.Rectangle 0, 0, 200, 170), $gui.PixelFormat)
Save-Preview $panel 2 "$scratch\gen_gui_preview.png"
$panel.Dispose(); $front.Dispose(); $frontOn.Dispose(); $gui.Dispose()
Write-Output "generator textures written"

