# Shared GUI panel painter, dot-sourced by every make_*_textures.ps1 that owns
# a gui/container texture. Draws the vanilla-anatomy 176x166 panel — rounded
# corners (transparent outside a radius-3 diagonal cut), 1px dark outline, 2px
# white top/left bevel, 2px shadow bottom/right, 198-grey body — plus the
# Automati signature: a pair of embossed gears meshing in the top-right of the
# title bar (clear of titles, slots, and the Erg gauge at 151,17).
#
# Usage after creating the 256x256 bitmap, BEFORE any slot drawing:
#   . "P:\ClaudeMods\ExampleMod\my-first-mod\tools\gui_common.ps1"
#   Draw-GuiPanel $gui
# Interior layout coordinates are unchanged from the old square panels.

function Draw-GuiPanel($bmp, $panelW = 176, $panelH = 166) {
  $outline = [System.Drawing.Color]::FromArgb(255, 32, 32, 32)
  $hiLight = [System.Drawing.Color]::FromArgb(255, 255, 255, 255)
  $shadow  = [System.Drawing.Color]::FromArgb(255, 85, 85, 85)
  $bodyCol = [System.Drawing.Color]::FromArgb(255, 198, 198, 198)

  for ($py = 0; $py -lt $panelH; $py++) {
    for ($px = 0; $px -lt $panelW; $px++) {
      $cx = [Math]::Min($px, $panelW - 1 - $px)   # distance to nearest vertical edge
      $cy = [Math]::Min($py, $panelH - 1 - $py)   # distance to nearest horizontal edge

      # rounded corner: outside the radius-3 diagonal stays transparent
      if (($cx + $cy) -lt 3) { continue }

      # 1px outline along edges and across the corner diagonal
      if ($cx -eq 0 -or $cy -eq 0 -or (($cx + $cy) -eq 3)) {
        $bmp.SetPixel($px, $py, $outline)
        continue
      }

      # bevels: white top/left, shadow bottom/right, body where they collide
      $isHi = ($px -le 2 -or $py -le 2)
      $isSh = ($px -ge ($panelW - 3) -or $py -ge ($panelH - 3))
      if ($isHi -and $isSh) { $bmp.SetPixel($px, $py, $bodyCol) }
      elseif ($isHi)        { $bmp.SetPixel($px, $py, $hiLight) }
      elseif ($isSh)        { $bmp.SetPixel($px, $py, $shadow) }
      else                  { $bmp.SetPixel($px, $py, $bodyCol) }
    }
  }

  Draw-PanelGear $bmp 161 9  4.6 2.6 1.1   # big gear
  Draw-PanelGear $bmp 151 13 3.0 1.7 0.7   # small gear meshing at its flank
}

# One embossed gear: 8 teeth, recessed axle hole. Pressed-into-the-plate look:
# darker rim on the top-left arc, faint highlight on the bottom-right arc.
function Draw-PanelGear($bmp, $gearCX, $gearCY, $rTooth, $rBody, $rHole) {
  $gearMid  = [System.Drawing.Color]::FromArgb(255, 172, 172, 172)
  $gearDark = [System.Drawing.Color]::FromArgb(255, 132, 136, 140)
  $gearLite = [System.Drawing.Color]::FromArgb(255, 221, 224, 226)

  $reach = [int][Math]::Ceiling($rTooth)
  for ($py = $gearCY - $reach; $py -le $gearCY + $reach; $py++) {
    for ($px = $gearCX - $reach; $px -le $gearCX + $reach; $px++) {
      $dx = $px - $gearCX; $dy = $py - $gearCY
      $dist = [Math]::Sqrt($dx * $dx + $dy * $dy)
      if ($dist -gt $rTooth -or $dist -le $rHole) { continue }

      $inGear = $false
      if ($dist -le $rBody) { $inGear = $true }
      else {
        # 8 teeth: alternate 22.5-degree sectors
        $ang = [Math]::Atan2($dy, $dx) * 180.0 / [Math]::PI + 180.0
        if (([int][Math]::Floor($ang / 22.5) % 2) -eq 0) { $inGear = $true }
      }
      if (-not $inGear) { continue }

      # embossed shading by which flank of the gear the pixel sits on
      $c = $gearMid
      if (($dx + $dy) -lt -1) { $c = $gearDark }
      elseif (($dx + $dy) -gt 1) { $c = $gearLite }
      $bmp.SetPixel($px, $py, $c)
    }
  }
}
