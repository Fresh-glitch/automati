# Regenerates every script-owned asset in ownership-correct order.
# Textures always; sounds only with -IncludeSounds (they need ffmpeg on PATH).
#
# Ordering rule that matters: make_animated_front.ps1 MUST run after
# make_generator_textures.ps1 — coal_generator_front_on.png (16x64 animated)
# is owned by the former; regenerating it from anywhere else flattens the
# animation (shipped once, see v1.2.0 changelog).
#
# Retired scripts (kept for history, deliberately NOT run):
#   make_wrench_texture.ps1 — 2D wrench sprite, superseded by the 3D model +
#   make_wrench_atlas.ps1 in v1.8.0. Running it would resurrect an orphan PNG.
param([switch]$IncludeSounds)

$tools = Split-Path -Parent $MyInvocation.MyCommand.Path

$textureScripts = @(
    'make_texture.ps1',
    'make_smooth_texture.ps1',
    'make_generator_textures.ps1',
    'make_animated_front.ps1',
    'make_top_texture.ps1',
    'make_ash_texture.ps1',
    'make_dust_textures.ps1',
    'make_loadbank_textures.ps1',
    'make_crusher_textures.ps1',
    'make_electric_furnace_textures.ps1',
    'make_cable_texture.ps1',
    'make_duct_texture.ps1',
    'make_goggles_textures.ps1',
    'make_goggles_equipment.ps1',
    'make_bucking_iron_texture.ps1',
    'make_wrench_atlas.ps1',
    'make_ergjar_textures.ps1',
    'make_ash_update_textures.ps1',
    'make_logo_v2.ps1'
)
$soundScripts = @(
    'make_sound.ps1',
    'make_clog_sound.ps1',
    'make_crusher_sound.ps1'
)

New-Item -ItemType Directory -Force "$env:TEMP\automati-previews" | Out-Null

foreach ($script in $textureScripts) {
    Write-Output ">> $script"
    & (Join-Path $tools $script)
    if ($LASTEXITCODE -and $LASTEXITCODE -ne 0) { throw "$script failed with exit code $LASTEXITCODE" }
}

if ($IncludeSounds) {
    foreach ($script in $soundScripts) {
        Write-Output ">> $script"
        & (Join-Path $tools $script)
        if ($LASTEXITCODE -and $LASTEXITCODE -ne 0) { throw "$script failed with exit code $LASTEXITCODE" }
    }
} else {
    Write-Output "(sounds skipped - pass -IncludeSounds to regenerate them; needs ffmpeg)"
}

Write-Output "all script-owned assets regenerated"
