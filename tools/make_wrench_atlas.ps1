# Generates engineers_wrench_atlas.png — the 16x16 swatch atlas for the 3D
# Engineer's Wrench item model (models/item/engineers_wrench.json).
#
# The model assigns per-face UVs into 4x4 (one 4x8) swatch regions with baked
# "fake AO" shading: lit top faces, gradient sides (darker toward the bottom
# edge), deep-shadow undersides, a diagonal knurl for the worm screw, worn
# bright steel for the jaw gripping faces, and a banded copper grip.
#
# Swatch map (uv pixel coords used by the model JSON):
#   CU_L  [0,0,4,4]    copper lit top        ST_L  [0,4,4,8]    steel lit top
#   CU_M  [4,0,8,4]    copper gradient side  ST_M  [4,4,8,8]    steel gradient side
#   CU_D  [8,0,12,4]   copper underside      ST_D  [8,4,12,8]   steel underside
#   GRIP  [12,0,16,8]  banded copper grip (tall)
#   KNL   [0,8,4,12]   knurled copper (worm) COL   [4,8,8,12]   dark collar gradient
#   ST_XD [8,8,12,12]  deep shadow           ST_W  [12,8,16,12] worn bright steel
#   CU_B  [0,12,4,16]  copper butt cap       (rest: spare fill)
#
# Deterministic: seeded RNG (7331) + fixed pixel order — do not reorder the
# loops or the committed atlas will change. Owned file:
# assets/automati/textures/item/engineers_wrench_atlas.png

Add-Type -AssemblyName System.Drawing
$outPath = "P:\ClaudeMods\ExampleMod\my-first-mod\src\main\resources\assets\automati\textures\item\engineers_wrench_atlas.png"
$rng = New-Object System.Random(7331)
$bmp = New-Object System.Drawing.Bitmap(16,16)

function Set-Px($px, $py, $baseR, $baseG, $baseB, $jit, $shift) {
    $n = $rng.Next((-1 * $jit), $jit + 1)
    $cr = [Math]::Max(0,[Math]::Min(255, $baseR + $n + $shift))
    $cg = [Math]::Max(0,[Math]::Min(255, $baseG + $n + $shift))
    $cb = [Math]::Max(0,[Math]::Min(255, $baseB + $n + $shift))
    $bmp.SetPixel($px,$py,[System.Drawing.Color]::FromArgb(255,$cr,$cg,$cb))
}

# copper base 201,119,75 | steel base 158,164,170 | dark steel 98,103,109
# Row A (y0-3): CU_L bright top | CU_M gradient side | CU_D underside
for ($py=0; $py -lt 4; $py++) { for ($px=0; $px -lt 4; $px++) {
    Set-Px $px $py 201 119 75 7 14
    $g = if ($py -eq 0) { 8 } elseif ($py -eq 3) { -18 } else { -4 }
    Set-Px ($px+4) $py 201 119 75 7 $g
    Set-Px ($px+8) $py 201 119 75 6 -34
} }
# GRIP (x12-15, y0-7): horizontal bands + top-to-bottom gradient
for ($py=0; $py -lt 8; $py++) { for ($px=12; $px -lt 16; $px++) {
    $g = [int](8 - (3.4 * $py)); $band = if (($py % 3) -eq 2) { -20 } else { 0 }
    Set-Px $px $py 201 119 75 6 ($g + $band)
} }
# Row B (y4-7): ST_L | ST_M gradient | ST_D
for ($py=4; $py -lt 8; $py++) { for ($px=0; $px -lt 4; $px++) {
    Set-Px $px $py 158 164 170 6 16
    $g = if ($py -eq 4) { 8 } elseif ($py -eq 7) { -20 } else { -4 }
    Set-Px ($px+4) $py 158 164 170 6 $g
    Set-Px ($px+8) $py 158 164 170 5 -34
} }
# Row C (y8-11): KNL knurl | COL collar gradient | ST_XD deep shadow | ST_W worn bright
for ($py=8; $py -lt 12; $py++) { for ($px=0; $px -lt 4; $px++) {
    $k = if ((($px + $py) % 2) -eq 0) { 12 } else { -22 }
    Set-Px $px $py 201 119 75 5 $k
    $g = if ($py -eq 8) { 6 } elseif ($py -eq 11) { -16 } else { -4 }
    Set-Px ($px+4) $py 98 103 109 6 $g
    Set-Px ($px+8) $py 98 103 109 5 -26
    Set-Px ($px+12) $py 176 184 192 5 22
} }
# Row D (y12-15): CU_B butt cap | spare fill
for ($py=12; $py -lt 16; $py++) { for ($px=0; $px -lt 4; $px++) {
    $g = if ($py -eq 12) { 10 } elseif ($py -eq 15) { -20 } else { -2 }
    Set-Px $px $py 201 119 75 7 $g
    Set-Px ($px+4) $py 201 119 75 7 0; Set-Px ($px+8) $py 158 164 170 6 0; Set-Px ($px+12) $py 158 164 170 6 0
} }

$bmp.Save($outPath,[System.Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose()
Write-Output "wrote $outPath"
