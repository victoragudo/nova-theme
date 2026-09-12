import os
import re
import shutil
import subprocess
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CLASSES = ROOT / "classes"
SOURCE = ROOT / "src/com/victoragudo/nova/NovaFontRegistrar.java"
DEFAULT_IDE = Path.home() / "Applications/GoLand.app/Contents"


def plugin_version():
    text = (ROOT / "META-INF/plugin.xml").read_text()
    return re.search(r"<version>([^<]+)</version>", text).group(1)


def compile_sources(ide):
    javac = ide / "jbr/Contents/Home/bin/javac"
    if not javac.exists():
        javac = Path(shutil.which("javac") or "")
    if not javac.exists():
        raise SystemExit("javac not found, pass the IDE path as the first argument")
    if CLASSES.exists():
        shutil.rmtree(CLASSES)
    CLASSES.mkdir()
    subprocess.run(
        [
            str(javac),
            "--release",
            "17",
            "-nowarn",
            "-cp",
            str(ide / "lib" / "*"),
            "-d",
            str(CLASSES),
            str(SOURCE),
        ],
        check=True,
    )


def entries():
    yield "META-INF/plugin.xml", ROOT / "META-INF/plugin.xml"
    yield "META-INF/pluginIcon.svg", ROOT / "META-INF/pluginIcon.svg"
    yield "META-INF/pluginIcon_dark.svg", ROOT / "META-INF/pluginIcon_dark.svg"
    for theme in sorted((ROOT / "themes").glob("*.theme.json")):
        yield f"themes/{theme.name}", theme
    for scheme in sorted((ROOT / "themes").glob("*.xml")):
        yield f"themes/{scheme.name}", scheme
    yield "com/victoragudo/nova/NovaFontRegistrar.class", CLASSES / "com/victoragudo/nova/NovaFontRegistrar.class"
    for font in sorted((ROOT / "fonts").iterdir()):
        yield f"fonts/{font.name}", font


def main():
    ide = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_IDE
    compile_sources(ide)
    target = ROOT / f"Nova-Theme-{plugin_version()}.jar"
    with zipfile.ZipFile(target, "w", zipfile.ZIP_DEFLATED) as archive:
        for name, path in entries():
            if not path.exists():
                raise SystemExit(f"missing file: {path}")
            archive.write(path, name)
    print(f"{target.name} {os.path.getsize(target) // 1024} KB")


if __name__ == "__main__":
    main()
