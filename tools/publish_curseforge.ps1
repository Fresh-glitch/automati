# Uploads a built Automati jar to CurseForge through the upload API
# (https://support.curseforge.com/support/solutions/articles/9000197321).
#
#   .\tools\publish_curseforge.ps1 -Version 1.12.0 -DryRun
#   .\tools\publish_curseforge.ps1 -Version 1.12.0
#
# Needs two things next to the built jar (build\libs\automati-<version>.jar):
#   - the API token in .env in the repo root, as CURSEFORGE_TOKEN=...
#     (.env is git-ignored; never commit or print the token)
#   - the changelog in docs\changelogs\<version>.md, written in markdown
#
# -DryRun checks the token, the jar, the changelog and the game versions and
# prints what would be sent, without uploading anything.
param(
    [Parameter(Mandatory)][string]$Version,
    [string]$ChangelogFile,
    [ValidateSet('release', 'beta', 'alpha')][string]$ReleaseType = 'release',
    # Names as CurseForge lists them. The upload API refuses a file without
    # at least one environment (Client, Server).
    [string[]]$GameVersions = @('26.2', 'Forge', 'Client', 'Server'),
    [switch]$DryRun
)

$ErrorActionPreference = 'Stop'
$projectId = 1609262
$api = 'https://minecraft.curseforge.com/api'
$repo = Split-Path -Parent $PSScriptRoot

if ($Version -notmatch '^\d+\.\d+\.\d+$') { throw "Version must look like 1.12.0 (got '$Version')" }

$jar = Join-Path $repo "build\libs\automati-$Version.jar"
if (-not (Test-Path $jar)) { throw "Jar not found: $jar (build or release that version first)" }

if (-not $ChangelogFile) { $ChangelogFile = Join-Path $repo "docs\changelogs\$Version.md" }
if (-not (Test-Path $ChangelogFile)) { throw "Changelog not found: $ChangelogFile" }
$changelog = [System.IO.File]::ReadAllText($ChangelogFile, [System.Text.Encoding]::UTF8).Trim()
if (-not $changelog) { throw "Changelog is empty: $ChangelogFile" }

# The jar must be the version it claims to be
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

# The token: CURSEFORGE_TOKEN (CURSEFORGE-TOKEN is accepted too)
$envPath = Join-Path $repo '.env'
if (-not (Test-Path $envPath)) { throw "No .env in $repo (expected a line CURSEFORGE_TOKEN=...)" }
$tokenLine = Get-Content $envPath | Where-Object { $_ -match '^\s*CURSEFORGE[-_]TOKEN\s*=' } | Select-Object -First 1
if (-not $tokenLine) { throw ".env has no CURSEFORGE_TOKEN line" }
$token = ($tokenLine -replace '^[^=]*=', '').Trim().Trim('"').Trim("'")
if (-not $token) { throw "CURSEFORGE_TOKEN in .env is empty" }
$headers = @{ 'X-Api-Token' = $token }

# Game version names to ids. A Minecraft version name also exists as a
# Bukkit version (type 1), which a mod file must not carry.
$bukkitType = 1
$known = Invoke-RestMethod -Uri "$api/game/versions" -Headers $headers
$versionIds = foreach ($name in $GameVersions) {
    $found = @($known | Where-Object { $_.name -eq $name -and $_.gameVersionTypeID -ne $bukkitType })
    if ($found.Count -ne 1) {
        throw "Game version '$name' matched $($found.Count) CurseForge entries (expected exactly 1)"
    }
    $found[0].id
}

$metadata = [ordered]@{
    changelog     = $changelog
    changelogType = 'markdown'
    displayName   = "automati-$Version.jar"
    gameVersions  = @($versionIds)
    releaseType   = $ReleaseType
} | ConvertTo-Json -Compress

Write-Output "project:       $projectId (automati)"
Write-Output "file:          $jar ($([math]::Round((Get-Item $jar).Length / 1KB, 1)) KB)"
Write-Output "release type:  $ReleaseType"
Write-Output "game versions: $($GameVersions -join ', ') -> ids $($versionIds -join ', ')"
Write-Output "changelog:     $ChangelogFile ($($changelog.Length) characters)"

if ($DryRun) {
    Write-Output ""
    Write-Output "Dry run: token accepted, nothing uploaded."
    return
}

Add-Type -AssemblyName System.Net.Http
$client = New-Object System.Net.Http.HttpClient
$client.DefaultRequestHeaders.Add('X-Api-Token', $token)
$form = New-Object System.Net.Http.MultipartFormDataContent
$stream = [System.IO.File]::OpenRead($jar)
try {
    $form.Add((New-Object System.Net.Http.StringContent($metadata, [System.Text.Encoding]::UTF8)), 'metadata')
    $filePart = New-Object System.Net.Http.StreamContent($stream)
    $filePart.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse('application/java-archive')
    $form.Add($filePart, 'file', "automati-$Version.jar")

    $response = $client.PostAsync("$api/projects/$projectId/upload-file", $form).GetAwaiter().GetResult()
    $body = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
} finally {
    $stream.Dispose()
    $form.Dispose()
    $client.Dispose()
}
if (-not $response.IsSuccessStatusCode) { throw "Upload failed: HTTP $([int]$response.StatusCode) $body" }

$fileId = ($body | ConvertFrom-Json).id
Write-Output ""
Write-Output "Uploaded as file $fileId"
Write-Output "https://www.curseforge.com/minecraft/mc-mods/automati/files/$fileId"
