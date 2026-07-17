Add-Type -AssemblyName System.Drawing

$texDir = "P:\ClaudeMods\ExampleMod\my-first-mod\src\main\resources\assets\automati\textures"
$scratch = "$env:TEMP\automati-previews"

function C($r,$g,$b) { [System.Drawing.Color]::FromArgb(255,$r,$g,$b) }

# Iron palette
$K = C 62 66 71; $D = C 118 124 130; $M = C 154 160 166; $L = C 188 194 199; $R = C 220 225 229
$softD = C 140 146 152; $softL = C 172 178 184

# Draws the casing-style plate base into $bmp at frame offset $oy
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

# ---- side face: three horizontal resistor-coil channels ----
# Coil rows sit at y = 4-5, 7-8, 10-11, recessed into the plate.
$coilRows = @(4, 7, 10)

# off: cold dark coils with faint wire winding visible
$off = New-Object System.Drawing.Bitmap 16,16
Draw-Base $off 0
foreach ($cy in $coilRows) {
  for ($x = 2; $x -le 13; $x++) {
    # winding: alternate tick every 2px reads as wrapped wire
    $wire = ((($x / 2) % 2) -eq 0)
    $off.SetPixel($x, $cy,   $(if ($wire) { C 86 90 96 } else { C 58 60 66 }))
    $off.SetPixel($x, $cy+1, $(if ($wire) { C 70 74 80 } else { C 48 50 55 }))
  }
  $off.SetPixel(1, $cy, $K); $off.SetPixel(14, $cy, $K)
  $off.SetPixel(1, $cy+1, $K); $off.SetPixel(14, $cy+1, $K)
}
$off.Save("$texDir\block\load_bank_side.png", [System.Drawing.Imaging.ImageFormat]::Png)

# on: the same coils glowing, 4 animated frames pulsing at slight offsets
$glowA = @( (C 255 195 60), (C 252 168 48), (C 255 210 80), (C 246 150 42) )  # bright winding
$glowB = @( (C 226 106 30), (C 238 128 36), (C 218  96 28), (C 234 120 34) )  # hot channel
$on = New-Object System.Drawing.Bitmap 16,64
for ($f = 0; $f -lt 4; $f++) {
  $oy = $f * 16
  Draw-Base $on $oy
  $rowShift = 0
  foreach ($cy in $coilRows) {
    # each coil row pulses one frame out of phase with the next
    $fi = ($f + $rowShift) % 4
    for ($x = 2; $x -le 13; $x++) {
      $wire = ((($x / 2) % 2) -eq 0)
      $on.SetPixel($x, $oy+$cy,   $(if ($wire) { $glowA[$fi] } else { $glowB[$fi] }))
      $on.SetPixel($x, $oy+$cy+1, $(if ($wire) { $glowB[$fi] } else { $glowA[($fi+2)%4] }))
    }
    $on.SetPixel(1, $oy+$cy, $K); $on.SetPixel(14, $oy+$cy, $K)
    $on.SetPixel(1, $oy+$cy+1, $K); $on.SetPixel(14, $oy+$cy+1, $K)
    $rowShift++
  }
}
$on.Save("$texDir\block\load_bank_side_on.png", [System.Drawing.Imaging.ImageFormat]::Png)
@'
{
  "animation": {
    "frametime": 3,
    "interpolate": true
  }
}
'@ | Out-File -Encoding ascii "$texDir\block\load_bank_side_on.png.mcmeta"

# ---- top face: wide rectangular heat vent, clearly not the generator's round exhaust ----
$top = New-Object System.Drawing.Bitmap 16,16
Draw-Base $top 0
for ($x = 2; $x -le 13; $x++) { $top.SetPixel($x, 4, $K); $top.SetPixel($x, 11, $K) }
for ($y = 4; $y -le 11; $y++) { $top.SetPixel(2, $y, $K); $top.SetPixel(13, $y, $K) }
for ($y = 5; $y -le 10; $y++) {
  for ($x = 3; $x -le 12; $x++) {
    if ($y % 2 -eq 1) { $top.SetPixel($x, $y, $D) }        # slats
    else { $top.SetPixel($x, $y, (C 32 34 38)) }           # dark gaps
  }
}
foreach ($p in @(@(2,2), @(12,2), @(2,12), @(12,12))) {
  $top.SetPixel($p[0], $p[1], $R); $top.SetPixel($p[0]+1, $p[1], $D)
  $top.SetPixel($p[0], $p[1]+1, $D); $top.SetPixel($p[0]+1, $p[1]+1, $K)
}
$top.Save("$texDir\block\load_bank_top.png", [System.Drawing.Imaging.ImageFormat]::Png)

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

for ($row = 0; $row -lt 3; $row++) { for ($col = 0; $col -lt 9; $col++) { Draw-Slot (7 + $col*18) (83 + $row*18) 18 18 $slotBody } }
for ($col = 0; $col -lt 9; $col++) { Draw-Slot (7 + $col*18) 141 18 18 $slotBody }

# dump-rate gauge frame: interior 40x12 at (68,34)
Draw-Slot 67 33 42 14 (C 43 43 43)
# tick marks every 10px = 100 E/t
for ($t = 1; $t -lt 4; $t++) {
  $g.FillRectangle((New-Object System.Drawing.SolidBrush (C 66 66 66)), (68 + $t*10), 34, 1, 12)
}

# dark instrument plaque behind the dump-rate readout (goggles-green text)
Draw-Slot 57 51 62 15 (C 43 43 43)

# Erg buffer socket at (151,17) 18x52 with a ridge every 5px = 1,000 E
Draw-Slot 151 17 18 52 (C 43 43 43)
for ($seg = 1; $seg -lt 10; $seg++) {
  $g.FillRectangle((New-Object System.Drawing.SolidBrush (C 66 66 66)), 152, (18 + $seg*5), 16, 1)
}
$g.Dispose()

# rate fill 40x12 at (176,0): green -> amber -> red, the universal load scale
for ($x = 0; $x -lt 40; $x++) {
  $t = $x / 39.0
  if ($t -lt 0.5) { $tt = $t / 0.5; $r2 = [int](80 + $tt*170); $g2 = [int](190 - $tt*30); $b2 = 40 }
  else { $tt = ($t-0.5)/0.5; $r2 = [int](250); $g2 = [int](160 - $tt*120); $b2 = [int](40 - $tt*10) }
  for ($y = 0; $y -lt 12; $y++) {
    $c = C $r2 $g2 $b2
    if ($y -eq 0) { $c = C ([Math]::Min(255,$r2+25)) ([Math]::Min(255,$g2+25)) ([Math]::Min(255,$b2+25)) }
    if (($x % 10) -eq 0 -and $x -gt 0) { $c = C ([int]($r2*0.7)) ([int]($g2*0.7)) ([int]($b2*0.7)) }
    $gui.SetPixel(176 + $x, $y, $c)
  }
}

# buffer fill 16x50 at (176,31): amber gradient with ridge rows (matches the generator)
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
$gui.Save("$texDir\gui\container\load_bank.png", [System.Drawing.Imaging.ImageFormat]::Png)

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
Save-Preview $off 8 "$scratch\lb_side_preview.png"
$onF0 = $on.Clone((New-Object System.Drawing.Rectangle 0, 0, 16, 16), $on.PixelFormat)
Save-Preview $onF0 8 "$scratch\lb_side_on_preview.png"
Save-Preview $top 8 "$scratch\lb_top_preview.png"
$panel = $gui.Clone((New-Object System.Drawing.Rectangle 0, 0, 220, 170), $gui.PixelFormat)
Save-Preview $panel 2 "$scratch\lb_gui_preview.png"
$onF0.Dispose(); $panel.Dispose(); $off.Dispose(); $on.Dispose(); $top.Dispose(); $gui.Dispose()
Write-Output "load bank textures written"

