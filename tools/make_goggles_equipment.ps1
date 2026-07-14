Add-Type -AssemblyName System.Drawing

# Humanoid equipment layer (64x32, armor-texture layout): goggles drawn on the
# head box — strap wrapping all four side faces, twin lenses on the front.
$out = "P:\ClaudeMods\ExampleMod\my-first-mod\src\main\resources\assets\automati\textures\entity\equipment\humanoid\goggles.png"
New-Item -ItemType Directory -Force (Split-Path $out) | Out-Null

function C($a,$r,$g,$b) { [System.Drawing.Color]::FromArgb($a,$r,$g,$b) }

$strap = C 255 72 58 46
$frame = C 255 196 124 58; $frameD = C 255 142 84 36
$lens = C 255 120 210 220; $lensHi = C 255 190 240 245

$bmp = New-Object System.Drawing.Bitmap 64,32   # transparent

# head side faces in the armor layout, all at rows y=8..15:
#   right x=0..7 | front x=8..15 | left x=16..23 | back x=24..31
# slim single-pixel strap around the sides and back (the armor layer renders
# inflated off the skin, so restraint here is what reads as "fitted" in game)
for ($x = 0; $x -le 7; $x++)   { $bmp.SetPixel($x, 11, $strap) }
for ($x = 16; $x -le 31; $x++) { $bmp.SetPixel($x, 11, $strap) }

# front face (x 8..15): two compact 2x2 lenses with a copper brow bar
foreach ($lx in @(9, 13)) {    # left edges of the two 2-wide lens boxes
  $bmp.SetPixel($lx, 10, $frame);     $bmp.SetPixel($lx + 1, 10, $frame)
  $bmp.SetPixel($lx, 11, $lens);      $bmp.SetPixel($lx + 1, 11, $lensHi)
  $bmp.SetPixel($lx, 12, $lens);      $bmp.SetPixel($lx + 1, 12, $lens)   # square lenses
}
# bridge and outer strap stubs on the eye row
$bmp.SetPixel(11, 11, $frameD); $bmp.SetPixel(12, 11, $frameD)
$bmp.SetPixel(8, 11, $strap); $bmp.SetPixel(15, 11, $strap)

$bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Output "goggles equipment layer written"
