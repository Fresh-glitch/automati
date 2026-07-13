# Synthesizes the clog sound: a thump, a spinning-down whine, and three
# dry mechanical coughs as the generator chokes on its ash.

$sampleRate = 44100
$dur = 1.1
$n = [int]($sampleRate * $dur)
$twoPi = 2.0 * [Math]::PI

$wavPath = "$env:TEMP\automati-previews\coal_generator_clog.wav"
$oggDir = "P:\ClaudeMods\ExampleMod\my-first-mod\src\main\resources\assets\automati\sounds"

$rnd = New-Object System.Random 7
$samples = New-Object 'double[]' $n
$smooth = 0.0

# dry coughs at these times (seconds)
$coughs = @(0.45, 0.65, 0.85)
$coughLen = [int]($sampleRate * 0.07)

for ($i = 0; $i -lt $n; $i++) {
  $t = $i / [double]$sampleRate
  $v = 0.0

  # initial thump: low sine burst with fast decay
  if ($t -lt 0.12) {
    $decay = 1.0 - ($t / 0.12)
    $v += 0.5 * $decay * $decay * [Math]::Sin($twoPi * 70.0 * $t)
  }

  # the hum spins down: 190 Hz falling to ~45 Hz over 0.7 s, fading out
  if ($t -lt 0.7) {
    $freq = 190.0 * [Math]::Pow(45.0 / 190.0, $t / 0.7)
    # integrate frequency approximately via chirp phase formula
    $k = [Math]::Log(45.0 / 190.0) / 0.7
    $phase = $twoPi * 190.0 * ([Math]::Exp($k * $t) - 1.0) / $k
    $v += 0.28 * (1.0 - $t / 0.7) * [Math]::Sin($phase)
  }

  # dry coughs: short low-passed noise bursts
  foreach ($c in $coughs) {
    $off = $i - [int]($sampleRate * $c)
    if ($off -ge 0 -and $off -lt $coughLen) {
      $decay = 1.0 - ($off / [double]$coughLen)
      $smooth = 0.5 * $smooth + 0.5 * (($rnd.NextDouble() * 2.0) - 1.0)
      $v += 0.34 * $decay * $smooth
    }
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
  $w.Write([int16]([Math]::Round($v * 32000)))
}
$w.Close()

& ffmpeg -y -loglevel error -i $wavPath -c:a libvorbis -qscale:a 4 "$oggDir\coal_generator_clog.ogg"
if ($LASTEXITCODE -eq 0) { Write-Output "clog ogg written" } else { Write-Output "ffmpeg failed: $LASTEXITCODE" }

