Add-Type -AssemblyName System.Drawing

$out = "P:\ClaudeMods\ExampleMod\my-first-mod\src\main\resources\automati_logo.png"
$preview = "$env:TEMP\automati-previews\logo_v2_preview.png"

function C($r,$g,$b) { [System.Drawing.Color]::FromArgb(255,$r,$g,$b) }

$W = 32; $H = 32
$img = New-Object System.Drawing.Bitmap $W,$H

# ---- night sky gradient ----
for ($y = 0; $y -lt 26; $y++) {
  $t = $y / 25.0
  $c = C ([int](22 + $t*24)) ([int](24 + $t*26)) ([int](32 + $t*28))
  for ($x = 0; $x -lt $W; $x++) { $img.SetPixel($x, $y, $c) }
}

# ---- metal ground (the "metaphorical ground": a steel slab) ----
for ($y = 26; $y -lt 32; $y++) {
  for ($x = 0; $x -lt $W; $x++) {
    $c = C 64 68 75
    if ($y -eq 26) { $c = C 96 101 108 }                       # lit top edge
    elseif ($y -ge 30) { $c = C 52 55 61 }                     # darker base
    elseif ((($x * 3 + $y * 5) % 11) -eq 0) { $c = C 74 78 86 } # subtle grain
    $img.SetPixel($x, $y, $c)
  }
}
# impact cracks radiating through the slab
foreach ($p in @(@(7,27), @(5,28), @(3,29), @(13,27), @(15,28), @(17,29), @(10,28), @(9,30))) {
  $img.SetPixel($p[0], $p[1], (C 34 36 42))
}

# ---- the bolt: zigzag polyline with jags, striking at (10,26) ----
$mask = New-Object 'bool[]' ($W * $H)
function CenterAt($y) {
  if ($y -le 9)      { return 22.0 - 0.8 * $y }
  elseif ($y -le 19) { return 19.0 - 0.9 * ($y - 10) }
  else               { return 14.0 - 0.65 * ($y - 20) }
}
function WidthAt($y) {
  if ($y -le 9) { return 2.2 }
  elseif ($y -le 19) { return 2.0 }
  else { return [Math]::Max(1.0, 1.8 - 0.12 * ($y - 20)) }
}
for ($y = 0; $y -le 26; $y++) {
  $bc = CenterAt $y
  $bw = WidthAt $y
  $lo = [int][Math]::Floor($bc - $bw); $hi = [int][Math]::Ceiling($bc + $bw)
  # at the jag rows, bridge across both segment centres for the classic kink
  if ($y -eq 10) { $lo = [int](14.8 - 2); $hi = [int](19 + 2) }
  if ($y -eq 20) { $lo = [int](10.9 - 2); $hi = [int](14 + 2) }
  for ($x = $lo; $x -le $hi; $x++) {
    if ($x -ge 0 -and $x -lt $W) { $mask[$y * $W + $x] = $true }
  }
}
# fill, then outline, then bright core
$fill = C 255 196 44; $edge = C 158 96 12; $core = C 255 232 130
for ($y = 0; $y -lt $H; $y++) {
  for ($x = 0; $x -lt $W; $x++) {
    if ($mask[$y * $W + $x]) { $img.SetPixel($x, $y, $fill) }
  }
}
for ($y = 0; $y -le 26; $y++) {
  for ($x = 0; $x -lt $W; $x++) {
    if (-not $mask[$y * $W + $x]) { continue }
    $isEdge = $false
    foreach ($d in @(@(0,-1), @(0,1), @(-1,0), @(1,0))) {
      $nx = $x + $d[0]; $ny = $y + $d[1]
      if ($nx -lt 0 -or $nx -ge $W -or $ny -lt 0 -or $ny -gt 26 -or -not $mask[$ny * $W + $nx]) { $isEdge = $true; break }
    }
    if ($isEdge) { $img.SetPixel($x, $y, $edge) }
  }
}
for ($y = 0; $y -le 24; $y++) {
  $cx = [int][Math]::Round((CenterAt $y))
  if ($mask[$y * $W + $cx]) { $img.SetPixel($cx, $y, $core) }
}

# ---- impact flash and blue-white sparks ----
$ix = 10; $iy = 26
for ($y = $iy - 4; $y -le $iy + 2; $y++) {
  for ($x = $ix - 5; $x -le $ix + 5; $x++) {
    if ($x -lt 0 -or $x -ge $W -or $y -lt 0 -or $y -ge $H) { continue }
    $d = [Math]::Sqrt(($x - $ix) * ($x - $ix) + ($y - $iy) * ($y - $iy))
    if ($d -le 2.1) { $img.SetPixel($x, $y, (C 255 255 255)) }
    elseif ($d -le 3.4) { $img.SetPixel($x, $y, (C 214 231 255)) }
  }
}
# spark crosses flying outward: bright blue-white centres with pale arms
$sparkC = C 236 246 255; $sparkA = C 148 190 250
foreach ($s in @(@(4,21), @(17,20), @(2,26), @(19,25), @(6,17), @(15,23))) {
  $sx = $s[0]; $sy = $s[1]
  $img.SetPixel($sx, $sy, $sparkC)
  foreach ($d in @(@(0,-1), @(0,1), @(-1,0), @(1,0))) {
    $nx = $sx + $d[0]; $ny = $sy + $d[1]
    if ($nx -ge 0 -and $nx -lt $W -and $ny -ge 0 -and $ny -lt $H) { $img.SetPixel($nx, $ny, $sparkA) }
  }
}
# loose spark dots
foreach ($s in @(@(8,15), @(13,18), @(1,23), @(20,22))) {
  $img.SetPixel($s[0], $s[1], $sparkA)
}

# ---- upscale 4x -> 128x128 ----
$logo = New-Object System.Drawing.Bitmap 128,128
$g = [System.Drawing.Graphics]::FromImage($logo)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$g.DrawImage($img, 0, 0, 128, 128)
$g.Dispose()
$logo.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
$logo.Save($preview, [System.Drawing.Imaging.ImageFormat]::Png)
$logo.Dispose(); $img.Dispose()
Write-Output "logo v2 written: $out"

