# Nova Theme

A vibrant, space-inspired theme for GoLand and all JetBrains IDEs, built for the [Islands UI](https://blog.jetbrains.com/platform/2025/12/meet-the-islands-theme-the-new-default-look-for-jetbrains-ides/). Editor and tool windows float as rounded islands over a deep-space canvas, and the syntax palette bursts with supernova energy.

Two variants:

- **Nova Dark**: deep-space navy islands (`#1A1E36`) over a near-black cosmic canvas (`#08090F`).
- **Nova Light**: bright stellar islands (`#FAFBFF`) over a soft nebula canvas (`#DFE2EE`), same high-energy accents corrected for daylight contrast.

Requires IDE version 2025.2 or newer.

## Bundled font

Nova ships with [Commit Mono](https://commitmono.com) (SIL OFL 1.1), a neutral coding font with smart kerning and ligatures. The plugin registers the font automatically at startup and selects it as the editor font with ligatures enabled on first run. No manual font installation is needed. If you change the editor font later, Nova respects your choice and never overrides it again.

## Palette

| Role | Dark | Light |
|------|------|-------|
| Canvas | `#08090F` | `#DFE2EE` |
| Island / editor | `#1A1E36` | `#FAFBFF` |
| Foreground | `#E4E6F1` | `#262B45` |
| Supernova magenta (keywords, operators, accent) | `#FF4D97` | `#D6247A` |
| Electric cyan (variables, links, caret) | `#5CC8FF` | `#0A78C8` |
| Nebula purple (types) | `#A66BFF` | `#7C3AED` |
| Star gold (functions) | `#FFD166` | `#B4830B` |
| Aurora green (strings) | `#3DF5B6` | `#0B9E6E` |
| Plasma orange (numbers) | `#FF8A3D` | `#D96B0A` |
| Red giant (constants, errors) | `#FF5C57` | `#E0403A` |
| Star blue (fields) | `#7A9BFF` | `#4A6BE0` |

## Install

From JetBrains Marketplace: search for "Nova Theme" in `Settings | Plugins | Marketplace`.

From disk:

1. Download the signed jar from the [latest release](https://github.com/victoragudo/nova-theme/releases/latest).
2. Go to `Settings | Plugins`.
3. Click the gear icon and select `Install Plugin from Disk...`.
4. Pick the downloaded jar.
5. Restart the IDE and select `Nova Dark` or `Nova Light` under `Settings | Appearance & Behavior | Appearance | Theme`.

## Build from source

Compile the font registrar against the IntelliJ platform jars of any installed 2025.2+ IDE, then package:

```bash
javac --release 17 -cp "<IDE>/Contents/lib/*" -d classes src/com/victoragudo/nova/NovaFontRegistrar.java
python3 -c "
import zipfile
entries = [('META-INF/plugin.xml',)*2, ('META-INF/pluginIcon.svg',)*2, ('META-INF/pluginIcon_dark.svg',)*2,
           ('themes/NovaDark.theme.json',)*2, ('themes/NovaDark.xml',)*2,
           ('themes/NovaLight.theme.json',)*2, ('themes/NovaLight.xml',)*2,
           ('classes/com/victoragudo/nova/NovaFontRegistrar.class', 'com/victoragudo/nova/NovaFontRegistrar.class'),
           ('fonts/CommitMono-400-Regular.otf',)*2, ('fonts/CommitMono-400-Italic.otf',)*2,
           ('fonts/CommitMono-700-Regular.otf',)*2, ('fonts/CommitMono-700-Italic.otf',)*2,
           ('fonts/LICENSE-CommitMono.txt',)*2]
with zipfile.ZipFile('Nova-Theme-1.0.0.jar', 'w', zipfile.ZIP_DEFLATED) as z:
    for src, arc in entries:
        z.write(src, arc)
"
```

## License

Theme: MIT. Bundled Commit Mono font: SIL Open Font License 1.1 (see `fonts/LICENSE-CommitMono.txt`).
