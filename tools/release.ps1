# The Automati release pipeline, un-fumble-able edition.
#
#   .\tools\release.ps1 -Version 1.10.0 -Name "the datagen update"
#
# Does, in order:
#   1. bumps the version in BOTH build.gradle and mods.toml (they must never
#      drift — that's the whole reason this script exists)
#   2. verifies both files agree afterward
#   3. reruns datagen so src/generated/resources can't ship stale
#   4. builds the jar (a broken build never gets committed)
#   5. commits everything in the tree as "Automati vX.Y.Z - <name>" and pushes
#
# NOT done here, by house tradition: the changelog text and the Modrinth/
# CurseForge uploads. Those belong to the humans.
param(
    [Parameter(Mandatory)][string]$Version,
    [Parameter(Mandatory)][string]$Name
)

$repo = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $repo

if ($Version -notmatch '^\d+\.\d+\.\d+$') { throw "Version must look like 1.10.0 (got '$Version')" }

# 1. bump both version declarations.
# Hard-won rules from the maiden voyage (v1.10.0):
#   - PS 5.1's Set-Content -Encoding utf8 writes a BOM, which Gradle rejects,
#     and Get-Content misdecodes UTF-8 — use [IO.File] with UTF8Encoding($false)
#   - the regexes MUST be line-anchored and case-sensitive (-creplace), or
#     loaderVersion="[65,)" in mods.toml gets clobbered as collateral
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$gradlePath = Join-Path $repo 'build.gradle'
$tomlPath = Join-Path $repo 'src\main\resources\META-INF\mods.toml'
$gradleText = [System.IO.File]::ReadAllText($gradlePath, [System.Text.Encoding]::UTF8)
[System.IO.File]::WriteAllText($gradlePath, ($gradleText -creplace "(?m)^version = '[^']+'", "version = '$Version'"), $utf8NoBom)
$tomlText = [System.IO.File]::ReadAllText($tomlPath, [System.Text.Encoding]::UTF8)
[System.IO.File]::WriteAllText($tomlPath, ($tomlText -creplace '(?m)^version="[^"]+"', "version=""$Version"""), $utf8NoBom)

# 2. sync check: both files must now carry the same version
$gradleOk = (Get-Content $gradlePath -Raw) -match [regex]::Escape("version = '$Version'")
$tomlOk = (Get-Content $tomlPath -Raw) -match [regex]::Escape("version=""$Version""")
if (-not ($gradleOk -and $tomlOk)) { throw "Version bump failed - build.gradle ok: $gradleOk, mods.toml ok: $tomlOk" }
Write-Output "version bumped to $Version in build.gradle + mods.toml"

# 3. datagen freshness
& .\gradlew runData --console=plain -q
if ($LASTEXITCODE -ne 0) { throw "runData failed - not releasing stale or broken generated resources" }
Write-Output "datagen fresh"

# 4. build before committing anything
& .\gradlew build --console=plain -q
if ($LASTEXITCODE -ne 0) { throw "build failed - nothing committed" }
$jar = Join-Path $repo "build\libs\automati-$Version.jar"
if (-not (Test-Path $jar)) { throw "expected jar not found: $jar" }
Write-Output "built $jar"

# 5. commit + push
git add -A
git commit -m "Automati v$Version - $Name" -m "Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
if ($LASTEXITCODE -ne 0) { throw "git commit failed" }
git push
if ($LASTEXITCODE -ne 0) { throw "git push failed - commit is local, push manually" }

Write-Output ""
Write-Output "=== Automati v$Version - $Name is out the door ==="
Write-Output "jar: $jar"
Write-Output "left for the humans: changelog text + Modrinth/CurseForge uploads"
