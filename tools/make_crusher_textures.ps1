Add-Type -AssemblyName System.Drawing

$texDir = "P:\ClaudeMods\ExampleMod\my-first-mod\src\main\resources\assets\automati\textures"
$scratch = "$env:TEMP\automati-previews"

function C($r,$g,$b) { [System.Drawing.Color]::FromArgb(255,$r,$g,$b) }

$K = C 62 66 71; $D = C 118 124 130; $M = C 154 160 166; $L = C 188 194 199; $R = C 220 225 229
$softD = C 140 146 152; $softL = C 172 178 184

function Draw-Base($bmp, $oy) {
  for ($y = 0; $y -lt 16; $y++) {
    for ($x = 0; $x -lt 16; $x++) {
      $c = $M
      if ((($x * 3 + $y * 5) % 9) -eq 0) { $c = $softD }
      elseif ((($x * 5 + $y * 3) % 13) -eq 0) { $c = $softL }
      if ($y -eq 0 -or $x -eq 0) { $c = $L }
      if ($y -eq 15 -or $x -eq 15) { $c = $K }
      $bmp.SetPixel($x, $oy + $y, $c)
    }
  }
}

# ---- side face: casing plate with a hazard-stripe band warning of the blades above ----
$side = New-Object System.Drawing.Bitmap 16,16
Draw-Base $side 0
for ($x = 1; $x -le 14; $x++) { $side.SetPixel($x, 1, $K); $side.SetPixel($x, 5, $K) }
for ($y = 2; $y -le 4; $y++) {
  for ($x = 1; $x -le 14; $x++) {
    $stripe = ([Math]::Floor(($x + $y) / 2) % 2) -eq 0
    $side.SetPixel($x, $y, $(if ($stripe) { C 226 178 44 } else { C 42 43 47 }))
  }
}
foreach ($p in @(@(2,12), @(12,12))) {
  $side.SetPixel($p[0], $p[1], $R); $side.SetPixel($p[0]+1, $p[1], $D)
  $side.SetPixel($p[0], $p[1]+1, $D); $side.SetPixel($p[0]+1, $p[1]+1, $K)
}
$side.Save("$texDir\block\crusher_side.png", [System.Drawing.Imaging.ImageFormat]::Png)

# ---- top face: two counter-rotating toothed rollers seen from above ----
# Left roller's teeth march down, right roller's march up: counter-rotation,
# biting toward the centre seam ??? which is what pulls material into the crush.
$rollerBase = C 52 55 61      # roller body between teeth
$toothFace = C 168 174 180    # tooth top
$toothEdge = C 205 211 216    # leading tooth edge highlight
$railInner = C 96 101 108

function Draw-TopFrame($bmp, $oy, $phase) {
  # housing rails left/right, frame top/bottom
  for ($y = 0; $y -lt 16; $y++) {
    for ($x = 0; $x -lt 16; $x++) {
      if ($y -eq 0 -or $y -eq 15 -or $x -le 1 -or $x -ge 14) {
        $c = $M
        if ($y -eq 0 -or $x -eq 0) { $c = $L }
        if ($y -eq 15 -or $x -eq 15) { $c = $K }
        if ($x -eq 1 -or $x -eq 14) { $c = $railInner }
        $bmp.SetPixel($x, $oy + $y, $c)
      }
    }
  }
  # rollers: columns 2..7 (left, teeth march +phase) and 8..13 (right, -phase)
  for ($y = 1; $y -le 14; $y++) {
    for ($x = 2; $x -le 13; $x++) {
      $left = $x -le 7
      $p = if ($left) { ($y + $phase) % 4 } else { ($y - $phase) % 4 }
      if ($p -lt 0) { $p += 4 }
      $c = $rollerBase
      if ($p -eq 0) { $c = $toothEdge }        # leading edge of the tooth
      elseif ($p -eq 1) { $c = $toothFace }    # tooth face
      # the centre seam where the rollers mesh stays dark and hungry
      if ($x -eq 7 -or $x -eq 8) {
        if ($p -eq 0) { $c = $D } elseif ($p -eq 1) { $c = $softD }
      }
      $bmp.SetPixel($x, $oy + $y, $c)
    }
  }
}

# off: static rollers, phase 0
$topOff = New-Object System.Drawing.Bitmap 16,16
Draw-TopFrame $topOff 0 0
$topOff.Save("$texDir\block\crusher_top.png", [System.Drawing.Imaging.ImageFormat]::Png)

# on: 4 frames of the tooth pattern marching = rotation
$topOn = New-Object System.Drawing.Bitmap 16,64
for ($f = 0; $f -lt 4; $f++) { Draw-TopFrame $topOn ($f * 16) $f }
$topOn.Save("$texDir\block\crusher_top_on.png", [System.Drawing.Imaging.ImageFormat]::Png)
@'
{
  "animation": {
    "frametime": 2
  }
}
'@ | Out-File -Encoding ascii "$texDir\block\crusher_top_on.png.mcmeta"

# ---- GUI texture ----
$gui = New-Object System.Drawing.Bitmap 256,256
$g = [System.Drawing.Graphics]::FromImage($gui)
$body = C 198 198 198; $bevelL = C 255 255 255; $bevelD = C 85 85 85
$slotD = C 55 55 55; $slotL = C 255 255 255; $slotBody = C 139 139 139

# rounded vanilla-style panel + embossed corner gears (shared painter)
. "P:\ClaudeMods\ExampleMod\my-first-mod\tools\gui_common.ps1"
Draw-GuiPanel $gui
function Draw-Slot($x, $y, $w, $h, $interior) {
  $g.FillRectangle((New-Object System.Drawing.SolidBrush $slotD), $x, $y, $w, 1)
  $g.FillRectangle((New-Object System.Drawing.SolidBrush $slotD), $x, $y, 1, $h)
  $g.FillRectangle((New-Object System.Drawing.SolidBrush $slotL), $x, $y+$h-1, $w, 1)
  $g.FillRectangle((New-Object System.Drawing.SolidBrush $slotL), $x+$w-1, $y, 1, $h)
  $g.FillRectangle((New-Object System.Drawing.SolidBrush $interior), $x+1, $y+1, $w-2, $h-2)
}
Draw-Slot 55 34 18 18 $slotBody                    # input (interior 56,35)
Draw-Slot 115 34 18 18 (C 120 120 120)             # output, darker
for ($row = 0; $row -lt 3; $row++) { for ($col = 0; $col -lt 9; $col++) { Draw-Slot (7 + $col*18) (83 + $row*18) 18 18 $slotBody } }
for ($col = 0; $col -lt 9; $col++) { Draw-Slot (7 + $col*18) 141 18 18 $slotBody }
Draw-Slot 151 17 18 52 (C 43 43 43)
for ($seg = 1; $seg -lt 10; $seg++) {
  $g.FillRectangle((New-Object System.Drawing.SolidBrush (C 66 66 66)), 152, (18 + $seg*5), 16, 1)
}
$g.Dispose()

# arrow drawing helper: shaft + head inside a 24x16 cell, origin ($ox,$oy)
function Draw-Arrow($ox, $oy, $shaft, $head) {
  for ($y = 5; $y -le 10; $y++) { for ($x = 0; $x -le 14; $x++) { $gui.SetPixel($ox+$x, $oy+$y, $shaft) } }
  for ($i = 0; $i -lt 8; $i++) {
    for ($y = (1 + $i); $y -le (14 - $i); $y++) { $gui.SetPixel($ox+15+$i, $oy+$y, $head) }
  }
}
# engraved arrow in the panel between the slots
Draw-Arrow 78 35 (C 160 160 160) (C 160 160 160)
# amber fill sprite at (176,0)
Draw-Arrow 176 0 (C 245 170 40) (C 255 200 70)

# energy fill 16x50 at (176,31), same amber gradient + ridges as the other machines
for ($y = 0; $y -lt 50; $y++) {
  $t = $y / 49.0
  $r2 = [int](255 - $t * 25); $g2 = [int](225 - $t * 87); $b2 = [int](120 - $t * 88)
  $ridge = ($y % 5) -eq 0 -and $y -gt 0
  for ($x = 0; $x -lt 16; $x++) {
    if ($ridge) { $c = C ([int]($r2 * 0.72)) ([int]($g2 * 0.72)) ([int]($b2 * 0.72)) }
    else {
      $c = C $r2 $g2 $b2
      if ($x -eq 0) { $c = C ([Math]::Min(255, $r2+20)) ([Math]::Min(255, $g2+20)) ([Math]::Min(255, $b2+30)) }
    }
    $gui.SetPixel(176 + $x, 31 + $y, $c)
  }
}
$gui.Save("$texDir\gui\container\crusher.png", [System.Drawing.Imaging.ImageFormat]::Png)

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
Save-Preview $side 8 "$scratch\crusher_side_preview.png"
Save-Preview $topOff 8 "$scratch\crusher_top_preview.png"
$panel = $gui.Clone((New-Object System.Drawing.Rectangle 0, 0, 220, 170), $gui.PixelFormat)
Save-Preview $panel 2 "$scratch\crusher_gui_preview.png"
$panel.Dispose(); $side.Dispose(); $topOff.Dispose(); $topOn.Dispose(); $gui.Dispose()
Write-Output "crusher textures written"

