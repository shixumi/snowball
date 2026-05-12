# Snowball — App Icon Generation Prompt

Use this prompt with an AI image generator (DALL·E 3, Midjourney v6+, Stable Diffusion XL, Flux Pro, Imagen, etc.). Aim for **1024×1024 PNG** output, suitable for the Android adaptive icon foreground layer.

---

## Primary prompt (copy-paste this)

```
A minimalist, premium app icon for "Snowball" — a personal finance app
with an "Editorial Frost" aesthetic.

Subject: a single pristine sphere of compacted snow, slightly imperfect
and organic (not a perfect geometric ball — think a real snowball pressed
by hand). Soft directional light from the upper left creates a subtle
warm highlight and a cool blue shadow on the lower right. The snowball
fills roughly 60% of the canvas, centered, with breathing room around it.

Around the snowball, a single thin arc — about 280 degrees of a circle —
in a pale ice blue (#9FCEE3), placed just outside the sphere's silhouette.
The arc suggests momentum and growth, like the snowball is gathering
mass as it rolls.

Background: solid deep midnight navy (#0B0F14). No gradient. No texture.
Optional: a single tiny warm pinpoint (champagne, #E8C68A) in the upper
right, like a distant winter star. Maximum 4 pixels at this resolution.

Color palette (use only these):
- Background: #0B0F14 (deep midnight)
- Snowball body: white to pale blue (#FFFFFF, #E5EEF4, #9FCEE3)
- Arc: pale ice blue (#9FCEE3)
- Optional star accent: champagne (#E8C68A)

Style: editorial, refined, premium. Influences: Apple Watch face
simplicity, Aesop / Le Labo packaging restraint, Linear or Things app
icon polish. No cartoon. No drop shadows that scream "iOS 7". No
glossy plastic look.

Strictly avoid:
- Any text, letters, numbers, or "S" monograms
- Coins, dollar signs, peso signs, charts, graphs, calculator buttons
- Multiple snowballs, snowmen with faces, scarves, or hats
- Christmas, holiday, or seasonal imagery
- Trees, branches, mountains
- Skeuomorphic surfaces (wood, leather, brushed metal)
- 3D bevels or chrome
- Glowing neon, lens flares
- Watermarks, signatures, borders

Format: square, 1024×1024, centered subject, safe margin of at least
10% on all sides so the adaptive icon's circular/rounded-square masks
don't crop the snowball.
```

## Optional variations

For a stronger arc/orbit feel:

> Replace "a single thin arc — about 280 degrees of a circle" with "a single thin gold-to-ice gradient arc, about 320 degrees, slightly tapered at the ends like a comet trail."

For a more minimal version (drop the arc entirely):

> Replace the arc paragraph with: "No surrounding arc. Just the snowball, alone, centered, with the optional champagne pinpoint in the upper right."

## After generation

You'll get a foreground image. For the Android adaptive icon, you'll also need:

1. **Foreground (PNG with transparent background, ideally):** the snowball + arc + star, no navy background. Many generators don't produce transparency directly — you may need to remove the navy background in a tool like remove.bg, Photoshop, or GIMP. Or just keep the navy as part of the foreground and let the app's adaptive icon mask handle the rest.
2. **Background (solid color):** `#0B0F14` (deep midnight). This is just a hex value; no image needed.

Place them at:
```
composeApp/src/androidMain/res/mipmap-anydpi-v26/ic_launcher.xml         (adaptive icon descriptor)
composeApp/src/androidMain/res/drawable/ic_launcher_foreground.png      (1024×1024)
composeApp/src/androidMain/res/values/colors.xml                         (background color)
```

Or, simpler for v0.1: drop a single square PNG at:
```
composeApp/src/androidMain/res/mipmap-xxxhdpi/ic_launcher.png            (192×192)
```
and reference it from the manifest — works on every device, no adaptive-icon ceremony required.

## Generator notes

- **DALL·E 3 / ChatGPT:** Paste the prompt verbatim. Add at the end: "Square aspect ratio, 1024×1024."
- **Midjourney:** Add `--ar 1:1 --v 6.1 --style raw --stylize 100` to the end. Drop the bullet lists if they hurt prompt adherence; keep the prose paragraphs.
- **Flux / SDXL:** These follow prompts more literally. The prompt above should work as-is.
- **Imagen 3 / Google:** Add "Photorealistic snow texture but minimal composition" if it leans too cartoonish.

Generate 4–8 variations, pick your favorite, iterate.
