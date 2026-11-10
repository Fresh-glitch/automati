# Uploads a built Automati jar to CurseForge through the upload API
# (https://support.curseforge.com/support/solutions/articles/9000197321).
#
#   .\tools\publish_curseforge.ps1 -Version 1.12.0 -DryRun
#   .\tools\publish_curseforge.ps1 -Version 1.12.0
#
# Needs build\libs\automati-<version>.jar, the changelog in
# docs\changelogs\<version>.md and CURSEFORGE_TOKEN in .env.
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

. "$PSScriptRoot\publish_common.ps1"
$projectId = 1609262
$api = 'https://minecraft.curseforge.com/api'

$jar = Get-ReleaseJar $Version
$changelog = Get-Changelog $Version $ChangelogFile
$token = Get-EnvToken 'CURSEFORGE_TOKEN'

# Game version names to ids. A Minecraft version name also exists as a
# Bukkit version (type 1), which a mod file must not carry.
$bukkitType = 1
$known = Invoke-RestMethod -Uri "$api/game/versions" -Headers @{ 'X-Api-Token' = $token }
$versionIds = foreach ($name in $GameVersions) {
    $found = @($known | Where-Object { $_.name -eq $name -and $_.gameVersionTypeID -ne $bukkitType })
    if ($found.Count -ne 1) {
        throw "Game version '$name' matched $($found.Count) CurseForge entries (expected exactly 1)"
    }
    $found[0].id
}

$metadata = [ordered]@{
    changelog     = $changelog.Text
    changelogType = 'markdown'
    displayName   = "automati-$Version.jar"
    gameVersions  = @($versionIds)
    releaseType   = $ReleaseType
} | ConvertTo-Json -Compress

Write-Output "project:       $projectId (automati)"
Write-Output "file:          $jar ($(Get-SizeKB $jar) KB)"
Write-Output "release type:  $ReleaseType"
Write-Output "game versions: $($GameVersions -join ', ') -> ids $($versionIds -join ', ')"
Write-Output "changelog:     $($changelog.Path) ($($changelog.Text.Length) characters)"

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
