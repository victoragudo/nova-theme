import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import palette
import scheme_keys
import ui_keys

ROOT = Path(__file__).resolve().parent.parent
THEMES = ROOT / "themes"
SCHEME_VERSION = "142"
IDE_VERSION = "2026.2"
FONT_FAMILY = "CommitMono"

VARIANTS = [
    {
        "palette": palette.DARK,
        "file": "NovaDark",
        "scheme": "NovaDark",
        "parent": "Islands Dark",
        "islands": True,
    },
    {
        "palette": palette.LIGHT,
        "file": "NovaLight",
        "scheme": "NovaLight",
        "parent": "Islands Light",
        "islands": True,
    },
    {
        "palette": palette.HIGH_CONTRAST,
        "file": "NovaDarkHighContrast",
        "scheme": "NovaDarkHighContrast",
        "parent": "Islands Dark",
        "islands": True,
    },
    {
        "palette": palette.DIM,
        "file": "NovaDim",
        "scheme": "NovaDim",
        "parent": "Islands Dark",
        "islands": True,
    },
    {
        "palette": palette.DARK,
        "file": "NovaDarkClassic",
        "scheme": "NovaDarkClassic",
        "parent": "Dark",
        "islands": False,
        "name": "Nova Dark Classic",
    },
    {
        "palette": palette.LIGHT,
        "file": "NovaLightClassic",
        "scheme": "NovaLightClassic",
        "parent": "Light",
        "islands": False,
        "name": "Nova Light Classic",
    },
]


def scheme_document(p, scheme_name):
    parent_scheme = "Darcula" if p["dark"] else "Default"
    lines = [
        f'<scheme name="{scheme_name}" version="{SCHEME_VERSION}" parent_scheme="{parent_scheme}">',
        "  <metaInfo>",
        f'    <property name="ideVersion">{IDE_VERSION}</property>',
        f'    <property name="originalScheme">{scheme_name}</property>',
        "  </metaInfo>",
        f'  <option name="EDITOR_FONT_NAME" value="{FONT_FAMILY}"/>',
        '  <option name="EDITOR_LIGATURES" value="true"/>',
        f'  <option name="CONSOLE_FONT_NAME" value="{FONT_FAMILY}"/>',
        '  <option name="CONSOLE_LIGATURES" value="true"/>',
        "  <colors>",
    ]
    for name, value in scheme_keys.colors(p).items():
        lines.append(f'    <option name="{name}" value="{value.lstrip("#").lower()}"/>')
    lines.append("  </colors>")
    lines.append("  <attributes>")
    for name, options in scheme_keys.attributes(p).items():
        if not options:
            continue
        lines.append(f'    <option name="{name}">')
        lines.append("      <value>")
        for option, value in options.items():
            lines.append(f'        <option name="{option}" value="{value}"/>')
        lines.append("      </value>")
        lines.append("    </option>")
    lines.append("  </attributes>")
    lines.append("</scheme>")
    return "\n".join(lines) + "\n"


def theme_document(variant):
    p = variant["palette"]
    return {
        "name": variant.get("name", p["name"]),
        "dark": p["dark"],
        "author": "Victor Agudo",
        "editorScheme": f"/themes/{variant['scheme']}.xml",
        "parentTheme": variant["parent"],
        "ui": ui_keys.build(p, variant["islands"]),
        "icons": ui_keys.icons(p),
    }


def main():
    written = []
    schemes = {}
    for variant in VARIANTS:
        p = variant["palette"]
        schemes[variant["scheme"]] = (p, variant.get("name", p["name"]))
        target = THEMES / f"{variant['file']}.theme.json"
        target.write_text(json.dumps(theme_document(variant), indent=2) + "\n")
        written.append(target)
    for file_name, (p, scheme_name) in schemes.items():
        target = THEMES / f"{file_name}.xml"
        target.write_text(scheme_document(p, scheme_name))
        written.append(target)
    for target in written:
        print(target.relative_to(ROOT))


if __name__ == "__main__":
    main()
