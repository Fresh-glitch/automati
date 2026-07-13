# Synthesizes a seamless machine-hum loop for the coal generator.
# All periodic components fit an integer number of cycles into the loop,
# so it can repeat without clicks.

$sampleRate = 44100
$loopSeconds = 2.4
$n = [int]($sampleRate * $loopSeconds)   # 105840 samples
$twoPi = 2.0 * [Math]::PI

$wavPath = "$env:TEMP\automati-previews\coal_generator_loop.wav"
$oggDir = "P:\ClaudeMods\ExampleMod\my-first-mod\src\main\resources\assets\automati\sounds"
New-Item -ItemType Directory -Force $oggDir | Out-Null

$rnd = New-Object System.Random 42
$samples = New-Object 'double[]' $n

$chugPeriod = [int]($sampleRate * 0.3)   # 8 chugs per loop
$chugLen = [int]($sampleRate * 0.06)
$smooth = 0.0

for ($i = 0; $i -lt $n; $i++) {
  $t = $i / [double]$sampleRate

  # electrical hum: 55 Hz fundamental + quieter 110 Hz overtone
  $hum = 0.16 * [Math]::Sin($twoPi * 55.0 * $t) + 0.08 * [Math]::Sin($twoPi * 110.0 * $t)

  # slow power wobble, 2.5 Hz (6 whole cycles per loop)
  $wobble = 1.0 + 0.15 * [Math]::Sin($twoPi * 2.5 * $t)

  # mechanical chug: a soft, decaying low-pass noise burst every 0.3 s
  $phase = $i % $chugPeriod
  $chug = 0.0
  if ($phase -lt $chugLen) {
    $decay = 1.0 - ($phase / [double]$chugLen)
    $smooth = 0.6 * $smooth + 0.4 * (($rnd.NextDouble() * 2.0) - 1.0)
    $chug = 0.22 * $decay * $decay * $smooth
  }

  $samples[$i] = ($hum * $wobble) + $chug
}

# write 16-bit mono PCM WAV
$stream = [System.IO.File]::Create($wavPath)
$w = New-Object System.IO.BinaryWriter $stream
$dataLen = $n * 2
$w.Write([byte[]][char[]]"RIFF"); $w.Write([int](36 + $dataLen)); $w.Write([byte[]][char[]]"WAVE")
$w.Write([byte[]][char[]]"fmt "); $w.Write([int]16); $w.Write([int16]1); $w.Write([int16]1)
$w.Write([int]$sampleRate); $w.Write([int]($sampleRate * 2)); $w.Write([int16]2); $w.Write([int16]16)
$w.Write([byte[]][char[]]"data"); $w.Write([int]$dataLen)
foreach ($s in $samples) {
  $v = [Math]::Max(-1.0, [Math]::Min(1.0, $s))
  $w.Write([int16]([Math]::Round($v * 32200)))
}
$w.Close()

# encode to OGG Vorbis where Minecraft expects it
& ffmpeg -y -loglevel error -i $wavPath -c:a libvorbis -qscale:a 4 "$oggDir\coal_generator_loop.ogg"
if ($LASTEXITCODE -eq 0) { Write-Output "ogg written: $oggDir\coal_generator_loop.ogg" } else { Write-Output "ffmpeg failed: $LASTEXITCODE" }

