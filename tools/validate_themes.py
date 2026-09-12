import json
import re
import subprocess
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
THEMES = ROOT / "themes"
CACHE = Path("/tmp/nova-theme-known-keys.json")

LEGACY_KEYS = {"GO_TYPE_PARAMETER", "GO_STRING_FORMAT_SPECIFIER", "Component.background"}

SCAN_JARS = [
    "lib/intellij.platform.core.jar",
    "lib/intellij.platform.ide.impl.jar",
    "lib/intellij.platform.editor.ex.jar",
    "lib/intellij.profiler.common.jar",
    "plugins/go-plugin",
    "plugins/go-template",
    "plugins/terraform",
    "plugins/protoeditor",
    "plugins/yaml",
    "plugins/json",
    "plugins/markdown",
    "plugins/terminal",
    "plugins/fullLine",
    "plugins/DatabaseTools",
    "plugins/restClient",
    "plugins/clouds-docker-impl",
    "plugins/clouds-kubernetes",
    "plugins/grazie",
    "plugins/javascript-plugin",
    "plugins/platform-bookmarks-plugin",
    "plugins/platform-todo-plugin",
    "plugins/vcs-git",
]

TOKEN = re.compile(r"\b[A-Z][A-Z0-9_]{3,}(?:\.[A-Z][A-Z0-9_]*)*\b")
UI_TOKEN = re.compile(r"\b[A-Za-z]+(?:\.[A-Za-z0-9]+)+\b")


def jar_paths(ide):
    for entry in SCAN_JARS:
        target = ide / entry
        if target.is_file():
            yield target
        elif target.is_dir():
            yield from target.rglob("*.jar")


def scan_tokens(ide):
    if CACHE.exists():
        return set(json.loads(CACHE.read_text()))
    tokens = set()
    for jar in jar_paths(ide):
        output = subprocess.run(
            ["strings", "-a", str(jar)], capture_output=True, text=True, errors="ignore"
        ).stdout
        tokens.update(TOKEN.findall(output))
        tokens.update(UI_TOKEN.findall(output))
    CACHE.write_text(json.dumps(sorted(tokens)))
    return tokens


def metadata_keys(ide):
    keys = set()
    for jar in ide.rglob("*.jar"):
        try:
            archive = zipfile.ZipFile(jar)
        except (zipfile.BadZipFile, OSError):
            continue
        with archive:
            for name in archive.namelist():
                if name.endswith("themeMetadata.json"):
                    data = json.loads(archive.read(name))
                    keys.update(entry["key"] for entry in data.get("ui", []))
                elif name.startswith("themes/") and name.endswith(".theme.json"):
                    data = json.loads(archive.read(name))
                    keys.update(flatten(data.get("ui", {})))
    return keys


def scheme_keys(ide):
    keys = set()
    pattern = re.compile(r'<option name="([A-Za-z][A-Za-z0-9_.]*)"')
    for jar in ide.rglob("*.jar"):
        try:
            archive = zipfile.ZipFile(jar)
        except (zipfile.BadZipFile, OSError):
            continue
        with archive:
            for name in archive.namelist():
                lowered = name.lower()
                if not lowered.endswith(".xml"):
                    continue
                if "colorscheme" not in lowered and "defaultcolorschemes" not in lowered:
                    continue
                keys.update(pattern.findall(archive.read(name).decode("utf-8", "ignore")))
    return keys


def flatten(node, prefix=""):
    for key, value in node.items():
        name = f"{prefix}{key}"
        if isinstance(value, dict):
            yield from flatten(value, name + ".")
        else:
            yield name


def normalize(key):
    return re.sub(r"(?<=[A-Za-z])\d+", "N", key)


def main():
    ide = Path(sys.argv[1]) if len(sys.argv) > 1 else Path.home() / "Applications/GoLand.app/Contents"
    if not ide.exists():
        print(f"IDE not found: {ide}")
        return 1
    known_ui = {normalize(key) for key in metadata_keys(ide)}
    known_scheme = scheme_keys(ide)
    tokens = scan_tokens(ide)
    known_scheme.update(tokens)
    known_ui.update(normalize(token) for token in tokens)

    unknown_ui = {}
    unknown_scheme = {}
    for path in sorted(THEMES.glob("*.theme.json")):
        data = json.loads(path.read_text())
        missing = sorted(
            key
            for key in flatten(data["ui"])
            if key not in LEGACY_KEYS
            and normalize(key) not in known_ui
            and normalize(key.split(".")[0]) not in known_ui
        )
        if missing:
            unknown_ui[path.name] = missing
    pattern = re.compile(r'<option name="([A-Za-z][A-Za-z0-9_.]*)"')
    ignored = {"FOREGROUND", "BACKGROUND", "EFFECT_COLOR", "EFFECT_TYPE", "FONT_TYPE", "ERROR_STRIPE_COLOR", "EDITOR_FONT_NAME", "EDITOR_LIGATURES", "CONSOLE_FONT_NAME", "CONSOLE_LIGATURES"}
    for path in sorted(THEMES.glob("*.xml")):
        found = set(pattern.findall(path.read_text())) - ignored
        missing = sorted(key for key in found if key not in known_scheme and key not in LEGACY_KEYS)
        if missing:
            unknown_scheme[path.name] = missing

    for name, keys in unknown_ui.items():
        print(f"unknown ui keys in {name}: {len(keys)}")
        for key in keys:
            print(f"  {key}")
    for name, keys in unknown_scheme.items():
        print(f"unknown scheme keys in {name}: {len(keys)}")
        for key in keys:
            print(f"  {key}")
    if not unknown_ui and not unknown_scheme:
        print("all theme and scheme keys resolve against the installed IDE")
        return 0
    return 1


if __name__ == "__main__":
    sys.exit(main())
