Add-Type -AssemblyName System.Drawing

$texDir = "P:\ClaudeMods\ExampleMod\my-first-mod\src\main\resources\assets\automati\textures"
$previewDir = "$env:TEMP\automati-previews"
New-Item -ItemType Directory -Force $previewDir | Out-Null

function C($r,$g,$b) { [System.Drawing.Color]::FromArgb(255,$r,$g,$b) }

$K = C 62 66 71; $D = C 118 124 130; $M = C 154 160 166; $L = C 188 194 199; $R = C 220 225 229
$softD = C 140 146 152; $softL = C 172 178 184
$glass = C 28 30 34

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
  foreach ($p in @(@(2,2), @(12,2), @(2,12), @(12,12))) {
    $bmp.SetPixel($p[0], $oy+$p[1], $R); $bmp.SetPixel($p[0]+1, $oy+$p[1], $D)
    $bmp.SetPixel($p[0], $oy+$p[1]+1, $D); $bmp.SetPixel($p[0]+1, $oy+$p[1]+1, $K)
  }
}

# Draws the element window at frame offset $oy; $phase -1 = off
function Draw-Front($bmp, $oy, $phase) {
  Draw-Base $bmp $oy
  # window frame and dark interior
  for ($x = 3; $x -le 12; $x++) { $bmp.SetPixel($x, $oy+3, $K); $bmp.SetPixel($x, $oy+12, $K) }
  for ($y = 3; $y -le 12; $y++) { $bmp.SetPixel(3, $oy+$y, $K); $bmp.SetPixel(12, $oy+$y, $K) }
  for ($y = 4; $y -le 11; $y++) {
    for ($x = 4; $x -le 11; $x++) { $bmp.SetPixel($x, $oy+$y, $glass) }
  }
  # three horizontal heating elements at y 5, 7, 9
  # (loop var must not be $r — that would clobber the $R rivet colour, see README)
  $rows = @(5, 7, 9)
  for ($elem = 0; $elem -lt 3; $elem++) {
    $y = $rows[$elem]
    for ($x = 4; $x -le 11; $x++) {
      $tick = (($x % 3) -eq 1)
      if ($phase -lt 0) {
        # cold elements: dark coil with faint metal ticks
        if ($tick) { $c = C 84 88 95 } else { $c = C 66 70 77 }
      } else {
        # each element pulses one frame out of phase with the next
        $p = ($phase + $elem) % 4
        if ($p -eq 0) { $hot = C 255 196 66 }
        elseif ($p -eq 1) { $hot = C 255 148 45 }
        elseif ($p -eq 2) { $hot = C 224 100 32 }
        else { $hot = C 250 170 52 }
        if ($tick) { $c = C 255 226 120 } else { $c = $hot }
      }
      $bmp.SetPixel($x, $oy+$y, $c)
    }
  }
  # status lamp top-right: dark red off, bright green running
  $bmp.SetPixel(13, $oy+2, $(if ($phase -lt 0) { C 96 28 28 } else { C 84 224 96 }))
}

# off face: single frame. NOTE: (-1) must be parenthesized — a bare -1
# argument binds as the STRING "-1", and "-1" -lt 0 is FALSE under
# culture-aware string comparison (the hyphen is ignored). See README.
$off = New-Object System.Drawing.Bitmap 16,16
Draw-Front $off 0 (-1)
$off.Save("$texDir\block\electric_furnace_front.png", [System.Drawing.Imaging.ImageFormat]::Png)

# on face: 4 animated frames
$on = New-Object System.Drawing.Bitmap 16,64
for ($f = 0; $f -lt 4; $f++) { Draw-Front $on ($f * 16) $f }
$on.Save("$texDir\block\electric_furnace_front_on.png", [System.Drawing.Imaging.ImageFormat]::Png)
@'
{
  "animation": {
    "frametime": 3,
    "interpolate": true
  }
}
'@ | Out-File -Encoding ascii "$texDir\block\electric_furnace_front_on.png.mcmeta"

# ---- GUI texture (crusher layout: input, arrow, output, Erg gauge) ----
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
Draw-Slot 55 34 18 18 $slotBody
Draw-Slot 115 34 18 18 (C 120 120 120)
for ($row = 0; $row -lt 3; $row++) { for ($col = 0; $col -lt 9; $col++) { Draw-Slot (7 + $col*18) (83 + $row*18) 18 18 $slotBody } }
for ($col = 0; $col -lt 9; $col++) { Draw-Slot (7 + $col*18) 141 18 18 $slotBody }
Draw-Slot 151 17 18 52 (C 43 43 43)
for ($seg = 1; $seg -lt 10; $seg++) {
  $g.FillRectangle((New-Object System.Drawing.SolidBrush (C 66 66 66)), 152, (18 + $seg*5), 16, 1)
}
$g.Dispose()

function Draw-Arrow($ox, $oy, $shaft, $head) {
  for ($y = 5; $y -le 10; $y++) { for ($x = 0; $x -le 14; $x++) { $gui.SetPixel($ox+$x, $oy+$y, $shaft) } }
  for ($i = 0; $i -lt 8; $i++) {
    for ($y = (1 + $i); $y -le (14 - $i); $y++) { $gui.SetPixel($ox+15+$i, $oy+$y, $head) }
  }
}
Draw-Arrow 78 35 (C 160 160 160) (C 160 160 160)
Draw-Arrow 176 0 (C 245 170 40) (C 255 200 70)

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
$gui.Save("$texDir\gui\container\electric_furnace.png", [System.Drawing.Imaging.ImageFormat]::Png)

# previews: off face + first on-frame
$big = New-Object System.Drawing.Bitmap 336,160
$gg = [System.Drawing.Graphics]::FromImage($big)
$gg.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$gg.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$gg.DrawImage($off, 0, 0, 160, 160)
$onF0 = $on.Clone((New-Object System.Drawing.Rectangle 0, 0, 16, 16), $on.PixelFormat)
$gg.DrawImage($onF0, 176, 0, 160, 160)
$gg.Dispose()
$big.Save("$previewDir\efurnace_preview.png", [System.Drawing.Imaging.ImageFormat]::Png)
$onF0.Dispose(); $big.Dispose(); $off.Dispose(); $on.Dispose(); $gui.Dispose()
Write-Output "electric furnace textures written"
