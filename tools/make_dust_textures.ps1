Add-Type -AssemblyName System.Drawing

$texDir = "P:\ClaudeMods\ExampleMod\my-first-mod\src\main\resources\assets\automati\textures\item"
$scratch = "$env:TEMP\automati-previews"

function C($r,$g,$b) { [System.Drawing.Color]::FromArgb(255,$r,$g,$b) }

# Same powder-heap generator as ash, parameterised per metal.
# Palettes: dark edge, mid body, light texture, bright flecks.
$dusts = @{
  "iron_dust"   = @( (C 122 104 92),  (C 158 138 122), (C 186 166 148), (C 209 190 172) )
  "copper_dust" = @( (C 142  78 48),  (C 184 106 66),  (C 212 130 84),  (C 232 156 106) )
  "gold_dust"   = @( (C 168 128 32),  (C 214 170 52),  (C 238 196 74),  (C 252 220 110) )
}

foreach ($name in $dusts.Keys) {
  $p = $dusts[$name]
  $dark = $p[0]; $mid = $p[1]; $light = $p[2]; $fleck = $p[3]

  $bmp = New-Object System.Drawing.Bitmap 16,16
  for ($y = 6; $y -le 13; $y++) {
    $half = [Math]::Min(6.2, ($y - 5) * 0.95 + 0.5)
    for ($x = 0; $x -lt 16; $x++) {
      $d = [Math]::Abs($x - 7.5)
      if ($d -le $half) {
        if ($y -le 8 -and ($d -gt $half - 1) -and ((($x * 5 + $y * 7) % 4) -eq 0)) { continue }
        $c = $mid
        if ($d -gt $half - 1.2 -or $y -eq 13) { $c = $dark }
        elseif ((($x * 3 + $y * 5) % 7) -eq 0) { $c = $light }
        elseif ((($x * 7 + $y * 3) % 11) -eq 0) { $c = $fleck }
        $bmp.SetPixel($x, $y, $c)
      }
    }
  }
  $bmp.SetPixel(5, 4, $light); $bmp.SetPixel(10, 3, $mid); $bmp.SetPixel(8, 5, $fleck)
  $bmp.Save("$texDir\$name.png", [System.Drawing.Imaging.ImageFormat]::Png)
  $bmp.Dispose()
}

# combined preview strip
$big = New-Object System.Drawing.Bitmap 416,128
$g = [System.Drawing.Graphics]::FromImage($big)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$i = 0
foreach ($name in @("iron_dust","copper_dust","gold_dust")) {
  $t = New-Object System.Drawing.Bitmap "$texDir\$name.png"
  $g.DrawImage($t, ($i * 144), 0, 128, 128)
  $t.Dispose(); $i++
}
$g.Dispose()
$big.Save("$scratch\dusts_preview.png", [System.Drawing.Imaging.ImageFormat]::Png)
$big.Dispose()
Write-Output "dust textures written"

