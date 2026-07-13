# Synthesizes the crusher's grinding loop: low rumble, gravelly chatter,
# a thin metallic scrape, and periodic tooth impacts. All modulators complete
# whole cycles per loop so it repeats seamlessly.

$sampleRate = 44100
$loopSeconds = 1.8
$n = [int]($sampleRate * $loopSeconds)
$twoPi = 2.0 * [Math]::PI

$wavPath = "$env:TEMP\automati-previews\crusher_loop.wav"
$oggDir = "P:\ClaudeMods\ExampleMod\my-first-mod\src\main\resources\assets\automati\sounds"

$rnd = New-Object System.Random 13
$samples = New-Object 'double[]' $n
$smooth = 0.0

$impactPeriod = [int]($sampleRate * 0.15)   # 12 tooth impacts per loop
$impactLen = [int]($sampleRate * 0.02)

for ($i = 0; $i -lt $n; $i++) {
  $t = $i / [double]$sampleRate
  $v = 0.0

  # low machine rumble: 60 Hz (108 whole cycles per loop)
  $v += 0.16 * [Math]::Sin($twoPi * 60.0 * $t)

  # gravelly chatter: smoothed noise, amplitude shaken at 10 Hz (18 cycles)
  $smooth = 0.7 * $smooth + 0.3 * (($rnd.NextDouble() * 2.0) - 1.0)
  $chatter = 0.5 + 0.5 * [Math]::Sin($twoPi * 10.0 * $t)
  $v += 0.22 * $chatter * $smooth

  # thin metallic scrape: 700 Hz (1260 cycles), wavering at 5 Hz (9 cycles)
  $v += 0.05 * (0.5 + 0.5 * [Math]::Sin($twoPi * 5.0 * $t)) * [Math]::Sin($twoPi * 700.0 * $t)

  # tooth impact: a hard little click every 0.15 s
  $phase = $i % $impactPeriod
  if ($phase -lt $impactLen) {
    $decay = 1.0 - ($phase / [double]$impactLen)
    $v += 0.3 * $decay * $decay * (($rnd.NextDouble() * 2.0) - 1.0)
  }

  $samples[$i] = $v
}

$stream = [System.IO.File]::Create($wavPath)
$w = New-Object System.IO.BinaryWriter $stream
$dataLen = $n * 2
$w.Write([byte[]][char[]]"RIFF"); $w.Write([int](36 + $dataLen)); $w.Write([byte[]][char[]]"WAVE")
$w.Write([byte[]][char[]]"fmt "); $w.Write([int]16); $w.Write([int16]1); $w.Write([int16]1)
$w.Write([int]$sampleRate); $w.Write([int]($sampleRate * 2)); $w.Write([int16]2); $w.Write([int16]16)
$w.Write([byte[]][char[]]"data"); $w.Write([int]$dataLen)
foreach ($s in $samples) {
  $v = [Math]::Max(-1.0, [Math]::Min(1.0, $s))
  $w.Write([int16]([Math]::Round($v * 31000)))
}
$w.Close()

& ffmpeg -y -loglevel error -i $wavPath -c:a libvorbis -qscale:a 4 "$oggDir\crusher_loop.ogg"
if ($LASTEXITCODE -eq 0) { Write-Output "crusher loop ogg written" } else { Write-Output "ffmpeg failed: $LASTEXITCODE" }

