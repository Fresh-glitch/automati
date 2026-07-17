# Generates the Erg Jar block + GUI textures:
#   block/erg_jar_side_0..4.png  (charge window with 0-4 lit pips)
#   block/erg_jar_terminal.png   (copper output terminal ring)
#   block/erg_jar_cap.png        (riveted top/bottom plate)
#   block/ash_block.png          (pressed ash storage block)
#   gui/container/erg_jar.png    (panel + standard Erg gauge)
Add-Type -AssemblyName System.Drawing

$texDir = "P:\ClaudeMods\ExampleMod\my-first-mod\src\main\resources\assets\automati\textures"
$scratch = "$env:TEMP\automati-previews"

function C($r,$g,$b) { [System.Drawing.Color]::FromArgb(255,$r,$g,$b) }

# Iron palette (house style, matches the other machines)
$K = C 62 66 71; $D = C 118 124 130; $M = C 154 160 166; $L = C 188 194 199; $R = C 220 225 229
$softD = C 140 146 152; $softL = C 172 178 184
# Copper palette (matches the wrench)
$cuL = C 224 142 92; $cuM = C 201 119 75; $cuD = C 165 92 55; $cuK = C 122 66 40

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

# ---- side faces: recessed charge window, 4 pip cells, bottom-up fill ----
# Window frame x5..10, y3..12; pip cells (bottom-up) rows: (10,11) (8,9) (6,7) (4,5)
$pipTop  = C 255 205 90   # lit pip, bright row
$pipBot  = C 234 146 44   # lit pip, hot row
$offTop  = C 44 46 51     # unlit pip
$offBot  = C 36 38 42
for ($charge = 0; $charge -le 4; $charge++) {
  $side = New-Object System.Drawing.Bitmap 16,16
  Draw-Base $side 0
  # frame
  for ($x = 5; $x -le 10; $x++) { $side.SetPixel($x, 3, $K); $side.SetPixel($x, 12, $K) }
  for ($y = 3; $y -le 12; $y++) { $side.SetPixel(5, $y, $K); $side.SetPixel(10, $y, $K) }
  # pip cells, index 0 at the bottom
  for ($cell = 0; $cell -lt 4; $cell++) {
    $rowTop = 10 - ($cell * 2)
    $lit = $cell -lt $charge
    for ($x = 6; $x -le 9; $x++) {
      $side.SetPixel($x, $rowTop,     $(if ($lit) { $pipTop } else { $offTop }))
      $side.SetPixel($x, $rowTop + 1, $(if ($lit) { $pipBot } else { $offBot }))
    }
  }
  $side.Save("$texDir\block\erg_jar_side_$charge.png", [System.Drawing.Imaging.ImageFormat]::Png)
  if ($charge -eq 4) { $sidePreview = $side } else { $side.Dispose() }
}

# ---- terminal face: copper output ring around a center boss ----
$term = New-Object System.Drawing.Bitmap 16,16
Draw-Base $term 0
# ring: octagon-ish, radius ~4.5 around center (7.5,7.5)
for ($y = 0; $y -lt 16; $y++) {
  for ($x = 0; $x -lt 16; $x++) {
    $dx = $x - 7.5; $dy = $y - 7.5
    $dist = [Math]::Sqrt($dx*$dx + $dy*$dy)
    if ($dist -ge 3.6 -and $dist -le 5.4) {
      $c = $cuM
      if ($dy -lt -2) { $c = $cuL }          # lit top of the ring
      elseif ($dy -gt 2) { $c = $cuD }        # shadowed bottom
      if ($dist -gt 5.0) { $c = $cuK }        # outer seat
      $term.SetPixel($x, $y, $c)
    }
    elseif ($dist -lt 1.8) {                  # center boss
      $term.SetPixel($x, $y, $(if ($dy -lt 0) { $cuL } else { $cuD }))
    }
  }
}
$term.Save("$texDir\block\erg_jar_terminal.png", [System.Drawing.Imaging.ImageFormat]::Png)

# ---- electrode face (opposite the terminal): square iron input pad ----
# Visibly different from the copper ring: square, grey, bolted — "iron goes in
# here". Sits opposite the terminal; the cell stack runs through the block.
$elec = New-Object System.Drawing.Bitmap 16,16
Draw-Base $elec 0
# recessed square pad x4..11, y4..11
for ($x = 4; $x -le 11; $x++) { $elec.SetPixel($x, 4, $K); $elec.SetPixel($x, 11, $K) }
for ($y = 4; $y -le 11; $y++) { $elec.SetPixel(4, $y, $K); $elec.SetPixel(11, $y, $K) }
$ironHi = C 134 140 147; $ironLo = C 96 101 108
for ($y = 5; $y -le 10; $y++) {
  for ($x = 5; $x -le 10; $x++) {
    $elec.SetPixel($x, $y, $(if (($x + $y) % 2 -eq 0) { $ironHi } else { $ironLo }))
  }
}
# bolts at the pad corners
foreach ($p in @(@(5,5), @(10,5), @(5,10), @(10,10))) {
  $elec.SetPixel($p[0], $p[1], $R)
}
$elec.Save("$texDir\block\erg_jar_electrode.png", [System.Drawing.Imaging.ImageFormat]::Png)

# ---- ash block: pressed powder, soft clumps, darker seams ----
$ashHi = C 168 164 158; $ashMid = C 148 144 139; $ashLo = C 128 124 120; $ashSeam = C 106 102 98
$ash = New-Object System.Drawing.Bitmap 16,16
for ($y = 0; $y -lt 16; $y++) {
  for ($x = 0; $x -lt 16; $x++) {
    $c = $ashMid
    if ((($x * 7 + $y * 3) % 11) -eq 0) { $c = $ashHi }
    elseif ((($x * 2 + $y * 9) % 13) -eq 0) { $c = $ashLo }
    if ((($x * 5 + $y * 7) % 23) -eq 0) { $c = $ashSeam }
    $ash.SetPixel($x, $y, $c)
  }
}
$ash.Save("$texDir\block\ash_block.png", [System.Drawing.Imaging.ImageFormat]::Png)

# ---- GUI texture: standard panel + Erg socket, center plaque for readouts ----
$gui = New-Object System.Drawing.Bitmap 256,256
$gfx = [System.Drawing.Graphics]::FromImage($gui)
$body = C 198 198 198; $bevelL = C 255 255 255; $bevelD = C 85 85 85
$slotD = C 55 55 55; $slotL = C 255 255 255; $slotBody = C 139 139 139

# rounded vanilla-style panel + embossed corner gears (shared painter)
. "P:\ClaudeMods\ExampleMod\my-first-mod\tools\gui_common.ps1"
Draw-GuiPanel $gui

function Draw-Slot($x, $y, $w, $h, $interior) {
  $gfx.FillRectangle((New-Object System.Drawing.SolidBrush $slotD), $x, $y, $w, 1)
  $gfx.FillRectangle((New-Object System.Drawing.SolidBrush $slotD), $x, $y, 1, $h)
  $gfx.FillRectangle((New-Object System.Drawing.SolidBrush $slotL), $x, $y+$h-1, $w, 1)
  $gfx.FillRectangle((New-Object System.Drawing.SolidBrush $slotL), $x+$w-1, $y, 1, $h)
  $gfx.FillRectangle((New-Object System.Drawing.SolidBrush $interior), $x+1, $y+1, $w-2, $h-2)
}

for ($row = 0; $row -lt 3; $row++) { for ($col = 0; $col -lt 9; $col++) { Draw-Slot (7 + $col*18) (83 + $row*18) 18 18 $slotBody } }
for ($col = 0; $col -lt 9; $col++) { Draw-Slot (7 + $col*18) 141 18 18 $slotBody }

# dark instrument plaque where the screen prints charge % and In/Out rates
# in goggles-green — multimeter styling
Draw-Slot 26 26 108 46 (C 43 43 43)

# Erg buffer socket at (151,17) 18x52 with a ridge every 5px
Draw-Slot 151 17 18 52 (C 43 43 43)
for ($seg = 1; $seg -lt 10; $seg++) {
  $gfx.FillRectangle((New-Object System.Drawing.SolidBrush (C 66 66 66)), 152, (18 + $seg*5), 16, 1)
}
$gfx.Dispose()

# buffer fill 16x50 at (176,31): amber gradient with ridge rows (house standard)
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
$gui.Save("$texDir\gui\container\erg_jar.png", [System.Drawing.Imaging.ImageFormat]::Png)

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
Save-Preview $sidePreview 8 "$scratch\ergjar_side4_preview.png"
Save-Preview $term 8 "$scratch\ergjar_terminal_preview.png"
Save-Preview $ash 8 "$scratch\ashblock_preview.png"
$panel = $gui.Clone((New-Object System.Drawing.Rectangle 0, 0, 220, 170), $gui.PixelFormat)
Save-Preview $panel 2 "$scratch\ergjar_gui_preview.png"
$sidePreview.Dispose(); $term.Dispose(); $elec.Dispose(); $ash.Dispose(); $panel.Dispose(); $gui.Dispose()
Write-Output "erg jar textures written"
