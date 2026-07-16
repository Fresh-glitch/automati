Add-Type -AssemblyName System.Drawing

# Palette texture for the 3D wrench model: four flat-ish cells the model's
# element faces UV into. MC's per-face shading provides the lighting.
$out = "P:\ClaudeMods\ExampleMod\my-first-mod\src\main\resources\assets\automati\textures\item\engineers_wrench.png"
$previewDir = "$env:TEMP\automati-previews"
New-Item -ItemType Directory -Force $previewDir | Out-Null

function C($r,$g,$b) { [System.Drawing.Color]::FromArgb(255,$r,$g,$b) }

$bmp = New-Object System.Drawing.Bitmap 16,16
for ($y = 0; $y -lt 16; $y++) {
  for ($x = 0; $x -lt 16; $x++) {
    $noise = (($x * 3 + $y * 5) % 7) - 3   # -3..3 subtle variation
    if ($x -lt 8 -and $y -lt 8)      { $c = C (184+$noise) (106+$noise) (66+$noise) }   # copper mid
    elseif ($x -ge 8 -and $y -lt 8)  { $c = C (212+$noise) (130+$noise) (84+$noise) }   # copper light
    elseif ($x -lt 8)                { $c = C (158+$noise) (164+$noise) (170+$noise) }  # iron mid
    else                             { $c = C (100+$noise) (105+$noise) (111+$noise) }  # iron dark
    $bmp.SetPixel($x, $y, $c)
  }
}
$bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Output "wrench palette written"
