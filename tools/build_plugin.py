import os
import re
import shutil
import subprocess
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CLASSES = ROOT / "classes"
SOURCES = ROOT / "src"
DESCRIPTORS = ["plugin.xml", "nova-vcs.xml", "pluginIcon.svg", "pluginIcon_dark.svg"]
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
            *sorted(str(source) for source in SOURCES.rglob("*.java")),
        ],
        check=True,
    )


def entries():
    for descriptor in DESCRIPTORS:
        yield f"META-INF/{descriptor}", ROOT / "META-INF" / descriptor
    for theme in sorted((ROOT / "themes").glob("*.theme.json")):
        yield f"themes/{theme.name}", theme
    for scheme in sorted((ROOT / "themes").glob("*.xml")):
        yield f"themes/{scheme.name}", scheme
    for compiled in sorted(CLASSES.rglob("*.class")):
        yield compiled.relative_to(CLASSES).as_posix(), compiled
    for attributes in sorted((ROOT / "colorSchemes").glob("*.xml")):
        yield f"colorSchemes/{attributes.name}", attributes
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
