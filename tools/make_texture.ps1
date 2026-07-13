Add-Type -AssemblyName System.Drawing

$out = "P:\ClaudeMods\ExampleMod\my-first-mod\src\main\resources\assets\automati\textures\block\factory_block.png"
$preview = "$env:TEMP\automati-previews\factory_block_preview.png"

New-Item -ItemType Directory -Force (Split-Path $out) | Out-Null

# Palette: industrial iron grays
$K = [System.Drawing.Color]::FromArgb(255,  62,  66,  71)  # darkest (seam shadow / rivet shadow)
$D = [System.Drawing.Color]::FromArgb(255, 118, 124, 130)  # dark (grooves, shading)
$M = [System.Drawing.Color]::FromArgb(255, 154, 160, 166)  # mid base plate
$L = [System.Drawing.Color]::FromArgb(255, 188, 194, 199)  # light highlight
$R = [System.Drawing.Color]::FromArgb(255, 220, 225, 229)  # rivet cap highlight
$N = [System.Drawing.Color]::FromArgb(255,  95, 100, 106)  # dent

$bmp = New-Object System.Drawing.Bitmap 16,16

for ($y = 0; $y -lt 16; $y++) {
  for ($x = 0; $x -lt 16; $x++) {
    # base plate with subtle deterministic noise (soft tone, sparse)
    $c = $M
    if ((($x * 3 + $y * 5) % 9) -eq 0) { $c = [System.Drawing.Color]::FromArgb(255, 140, 146, 152) }
    elseif ((($x * 5 + $y * 3) % 13) -eq 0) { $c = [System.Drawing.Color]::FromArgb(255, 172, 178, 184) }

    # cross seam dividing the face into four plates: dark groove with light lip
    if ($x -eq 7 -or $y -eq 7) { $c = $D }
    if (($x -eq 8 -and $y -ne 7) -or ($y -eq 8 -and $x -ne 7)) { $c = $L }
    if ($x -eq 7 -and $y -eq 7) { $c = $K }

    # beveled tile edges: light catches top/left, shadow bottom/right
    if ($y -eq 0 -or $x -eq 0) { $c = $L }
    if ($y -eq 15 -or $x -eq 15) { $c = $K }

    $bmp.SetPixel($x, $y, $c)
  }
}

# rivets: 2x2, bright cap top-left fading to shadow bottom-right
foreach ($p in @(@(2,2), @(12,2), @(2,12), @(12,12))) {
  $rx = $p[0]; $ry = $p[1]
  $bmp.SetPixel($rx,     $ry,     $R)
  $bmp.SetPixel($rx + 1, $ry,     $D)
  $bmp.SetPixel($rx,     $ry + 1, $D)
  $bmp.SetPixel($rx + 1, $ry + 1, $K)
}

# dents: dark pit with a light glint at its lower-right edge
foreach ($p in @(@(5,5), @(10,4), @(4,11), @(11,10))) {
  $bmp.SetPixel($p[0],     $p[1],     $N)
  $bmp.SetPixel($p[0] + 1, $p[1] + 1, $L)
}

$bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)

# 160x160 nearest-neighbour upscale for human inspection
$big = New-Object System.Drawing.Bitmap 160,160
$g = [System.Drawing.Graphics]::FromImage($big)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$g.DrawImage($bmp, 0, 0, 160, 160)
$g.Dispose()
$big.Save($preview, [System.Drawing.Imaging.ImageFormat]::Png)

$big.Dispose()
$bmp.Dispose()
Write-Output "texture written: $out"

