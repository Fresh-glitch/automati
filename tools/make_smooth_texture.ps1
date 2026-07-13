Add-Type -AssemblyName System.Drawing

$out = "P:\ClaudeMods\ExampleMod\my-first-mod\src\main\resources\assets\automati\textures\block\factory_block_smooth.png"
$preview = "$env:TEMP\automati-previews\factory_block_smooth_preview.png"

# Same iron palette as the riveted plate so the two variants read as one material,
# but no border, seams or rivets ??? must tile seamlessly against itself.
$M  = [System.Drawing.Color]::FromArgb(255, 154, 160, 166)  # base
$D2 = [System.Drawing.Color]::FromArgb(255, 143, 149, 155)  # faint dark grain
$L2 = [System.Drawing.Color]::FromArgb(255, 168, 174, 180)  # faint light grain

$bmp = New-Object System.Drawing.Bitmap 16,16

for ($y = 0; $y -lt 16; $y++) {
  for ($x = 0; $x -lt 16; $x++) {
    $c = $M
    if ((($x * 3 + $y * 5) % 9) -eq 0) { $c = $D2 }
    elseif ((($x * 5 + $y * 3) % 13) -eq 0) { $c = $L2 }
    $bmp.SetPixel($x, $y, $c)
  }
}

$bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)

# Preview: the tile repeated 3x3 at 4x scale, to prove it tiles seamlessly
$big = New-Object System.Drawing.Bitmap 192,192
$g = [System.Drawing.Graphics]::FromImage($big)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
for ($ty = 0; $ty -lt 3; $ty++) {
  for ($tx = 0; $tx -lt 3; $tx++) {
    $g.DrawImage($bmp, $tx * 64, $ty * 64, 64, 64)
  }
}
$g.Dispose()
$big.Save($preview, [System.Drawing.Imaging.ImageFormat]::Png)

$big.Dispose()
$bmp.Dispose()
Write-Output "smooth texture written: $out"

