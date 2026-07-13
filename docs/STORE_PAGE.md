# Automati

**Industrial automation, one Erg at a time.**

Automati is a tier-1 tech mod built around the **Erg (E)** — its own energy unit
— and machines that look, sound, and behave like real industrial equipment.

## The Machines

⚙️ **Factory Casing** — the riveted iron backbone of all machinery. Lay it as
flooring and interior tiles smooth out automatically into seamless plating,
framed by riveted edges.

🔥 **Coal Generator** — burns coal and charcoal into Ergs behind an animated,
glowing firebox. Every piece of fuel leaves **ash** behind — and if you don't
haul the ash away, the machine clogs up and chokes to a stop, with an audible
sputter. Automate it or suffer.

🌀 **Crusher** — counter-rotating shredder rollers, exposed on the top face and
exactly as dangerous as they look. Pulverizes raw metals into **2× dust** (3×
from silk-touched ore) that smelts into ingots — classic ore doubling. Also
grinds cobblestone, gravel, and bone. **Do not stand on the rollers.** They hit
like an iron sword, ignore half your armor, chew through armor durability, and
drag you toward the center. There is a custom death message. You will see it.

⚡ **Load Bank** — a real-world engineer's tool: dial in a dump rate with +/−
controls and burn off surplus Ergs as glowing waste heat to stress-test your
power grid.

## The Details

- Custom synthesized machine sounds with subtitles (accessibility included)
- Animated textures: flickering fireboxes, spinning shredder teeth
- Data-driven crushing recipes — datapacks can add, remove, or rebalance
  everything under `data/automati/recipe/crushing/`
- Energy rides on the standard Forge capability, so hoppers and future
  machines connect naturally
- Ridged energy gauges, warning lamps, and GUI tooltips throughout

## Status

Early alpha, in active development. The energy economy (generator → crusher)
is complete and stable; more machines, Erg cables, and uses for ash are on the
roadmap. Feedback welcome!

**Requires:** Minecraft 26.2, Forge 65.0.3+
