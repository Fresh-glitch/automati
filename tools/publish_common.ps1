# Shared by the publish_*.ps1 scripts (dot-sourced; uploads nothing itself):
# finds and checks the release jar, reads the changelog and reads API tokens
# from .env in the repo root. .env is git-ignored; never commit or print a
# token.
$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot

# The built jar for a version, after checking that it really is that version
function Get-ReleaseJar([string]$Version) {
    if ($Version -notmatch '^\d+\.\d+\.\d+$') { throw "Version must look like 1.12.0 (got '$Version')" }
    $jar = Join-Path $repo "build\libs\automati-$Version.jar"
    if (-not (Test-Path $jar)) { throw "Jar not found: $jar (build or release that version first)" }

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::OpenRead($jar)
    try {
        $entry = $zip.GetEntry('META-INF/mods.toml')
        if (-not $entry) { throw "No META-INF/mods.toml in $jar" }
        $reader = New-Object System.IO.StreamReader($entry.Open())
        $toml = $reader.ReadToEnd()
        $reader.Dispose()
    } finally {
        $zip.Dispose()
    }
    if ($toml -notmatch "(?m)^version=""$([regex]::Escape($Version))""") {
        throw "The mods.toml inside the jar does not declare version $Version"
    }
    return $jar
}

# The markdown changelog: docs\changelogs\<version>.md unless a file is given
function Get-Changelog([string]$Version, [string]$ChangelogFile) {
    if (-not $ChangelogFile) { $ChangelogFile = Join-Path $repo "docs\changelogs\$Version.md" }
    if (-not (Test-Path $ChangelogFile)) { throw "Changelog not found: $ChangelogFile" }
    $text = [System.IO.File]::ReadAllText($ChangelogFile, [System.Text.Encoding]::UTF8).Trim()
    if (-not $text) { throw "Changelog is empty: $ChangelogFile" }
    return [pscustomobject]@{ Path = $ChangelogFile; Text = $text }
}

# A token from .env. The name is given with underscores (CURSEFORGE_TOKEN);
# a line spelled with hyphens is accepted too.
function Get-EnvToken([string]$Name) {
    $envPath = Join-Path $repo '.env'
    if (-not (Test-Path $envPath)) { throw "No .env in $repo (expected a line $Name=...)" }
    $pattern = '^\s*' + ($Name -replace '_', '[-_]') + '\s*='
    $line = Get-Content $envPath | Where-Object { $_ -match $pattern } | Select-Object -First 1
    if (-not $line) { throw ".env has no $Name line" }
    $token = ($line -replace '^[^=]*=', '').Trim().Trim('"').Trim("'")
    if (-not $token) { throw "$Name in .env is empty" }
    return $token
}

function Get-SizeKB([string]$Path) {
    return [math]::Round((Get-Item $Path).Length / 1KB, 1)
}
