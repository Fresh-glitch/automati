# Uploads a built Automati jar to Modrinth as a new version
# (https://docs.modrinth.com/api/operations/createversion/).
#
#   .\tools\publish_modrinth.ps1 -Version 1.12.0 -DryRun
#   .\tools\publish_modrinth.ps1 -Version 1.12.0
#
# Needs build\libs\automati-<version>.jar, the changelog in
# docs\changelogs\<version>.md and MODRINTH_TOKEN in .env: a personal access
# token with the "Create versions" scope, plus "Read user data" if -DryRun
# should be able to verify it.
#
# -DryRun checks the jar, the changelog and that the version does not exist
# yet, and prints what would be sent, without uploading anything.
param(
    [Parameter(Mandatory)][string]$Version,
    [string]$ChangelogFile,
    # Every Automati version on Modrinth so far is marked alpha
    [ValidateSet('release', 'beta', 'alpha')][string]$VersionType = 'alpha',
    [string[]]$GameVersions = @('26.2'),
    [string[]]$Loaders = @('forge'),
    [switch]$DryRun
)

. "$PSScriptRoot\publish_common.ps1"
$projectId = 'msjyTfNc'
$api = 'https://api.modrinth.com/v2'
$userAgent = 'automati-publish/1.0 (github.com/Fresh-glitch/automati)'

$jar = Get-ReleaseJar $Version
$changelog = Get-Changelog $Version $ChangelogFile
$token = Get-EnvToken 'MODRINTH_TOKEN'
$headers = @{ 'Authorization' = $token; 'User-Agent' = $userAgent }

# Modrinth only lets a token be checked if it has the "Read user data" scope;
# without it the first real proof is the upload itself
try {
    $account = (Invoke-RestMethod -Uri "$api/user" -Headers $headers).username
    $tokenState = "token accepted for $account"
} catch {
    $tokenState = 'token NOT verified (invalid, or without the "Read user data" scope)'
}

$existing = Invoke-RestMethod -Uri "$api/project/$projectId/version" -Headers @{ 'User-Agent' = $userAgent }
if ($existing | Where-Object { $_.version_number -eq $Version }) {
    throw "Modrinth already has a version $Version of this project"
}

$data = [ordered]@{
    name           = "Automati $Version"
    version_number = $Version
    changelog      = $changelog.Text
    dependencies   = @()
    game_versions  = @($GameVersions)
    version_type   = $VersionType
    loaders        = @($Loaders)
    featured       = $false
    project_id     = $projectId
    file_parts     = @('file')
    primary_file   = 'file'
} | ConvertTo-Json -Compress

Write-Output "project:       $projectId (automati)"
Write-Output "file:          $jar ($(Get-SizeKB $jar) KB)"
Write-Output "version:       Automati $Version ($VersionType)"
Write-Output "game versions: $($GameVersions -join ', '); loaders: $($Loaders -join ', ')"
Write-Output "changelog:     $($changelog.Path) ($($changelog.Text.Length) characters)"

if ($DryRun) {
    Write-Output ""
    Write-Output "Dry run: $tokenState, nothing uploaded."
    return
}

Add-Type -AssemblyName System.Net.Http
$client = New-Object System.Net.Http.HttpClient
[void]$client.DefaultRequestHeaders.TryAddWithoutValidation('Authorization', $token)
[void]$client.DefaultRequestHeaders.TryAddWithoutValidation('User-Agent', $userAgent)
$form = New-Object System.Net.Http.MultipartFormDataContent
$stream = [System.IO.File]::OpenRead($jar)
try {
    $form.Add((New-Object System.Net.Http.StringContent($data, [System.Text.Encoding]::UTF8, 'application/json')), 'data')
    $filePart = New-Object System.Net.Http.StreamContent($stream)
    $filePart.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse('application/java-archive')
    $form.Add($filePart, 'file', "automati-$Version.jar")

    $response = $client.PostAsync("$api/version", $form).GetAwaiter().GetResult()
    $body = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
} finally {
    $stream.Dispose()
    $form.Dispose()
    $client.Dispose()
}
if (-not $response.IsSuccessStatusCode) { throw "Upload failed: HTTP $([int]$response.StatusCode) $body" }

$versionId = ($body | ConvertFrom-Json).id
Write-Output ""
Write-Output "Uploaded as version $versionId"
Write-Output "https://modrinth.com/mod/automati/version/$Version"
