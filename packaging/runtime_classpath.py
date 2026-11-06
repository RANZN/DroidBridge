#!/usr/bin/env python3
"""
Prints the desktop app's runtime classpath (one JAR path per line).

Why: `kotlin package -f executable-jar` stores dependencies by file name, and two different
artifacts share the name runtime-desktop-1.12.1.jar (org.jetbrains.compose.runtime and
androidx.compose.runtime), so the real Compose runtime gets overwritten. Instead we read the
resolved dependency graph from `kotlin show dependencies` and locate each JAR in the cache.

Usage: python3 packaging/runtime_classpath.py [--cache-dir DIR]
"""
import argparse
import os
import pathlib
import subprocess
import sys
import zipfile

ROOT = pathlib.Path(__file__).resolve().parent.parent


def default_cache_dir() -> pathlib.Path:
    if sys.platform == "darwin":
        return pathlib.Path.home() / "Library/Caches/JetBrains/Kotlin"
    return pathlib.Path(os.environ.get("XDG_CACHE_HOME", pathlib.Path.home() / ".cache")) / "JetBrains/Kotlin"


def parse(line: str):
    """Returns (depth, group, artifact, version) for a dependency line, or None."""
    entry = line.lstrip("│├╰─ ").strip()
    if not entry or entry.startswith(("Module", "-", "Dependencies")) or entry.endswith("(c)"):
        return None
    depth = len(line) - len(line.lstrip("│├╰─ "))
    entry = entry.replace("(*)", "").strip()
    requested, _, resolved = entry.partition(" -> ")
    parts = requested.strip().split(":")
    if len(parts) < 3:
        return None
    return depth, parts[-3], parts[-2], (resolved.strip() or parts[-1])


def resolved_coordinates(graph: str):
    """
    Yields (group, artifact, version) for every resolved JVM dependency in the graph.
    Kotlin Multiplatform "root" artifacts (e.g. runtime) are skipped: their first child is the
    platform variant with the same group (e.g. runtime-desktop), which is the JAR the JVM needs.
    """
    nodes = [n for n in map(parse, graph.splitlines()) if n]
    roots = set()
    for current, following in zip(nodes, nodes[1:]):
        depth, group, artifact, _ = current
        if following[0] > depth and following[1] == group and following[2].startswith(artifact + "-"):
            roots.add((group, artifact))
    for _, group, artifact, version in nodes:
        if (group, artifact) not in roots:
            yield group, artifact, version


def is_multiplatform_metadata(jar: pathlib.Path) -> bool:
    """KMP metadata JARs (no JVM code) carry this marker; the JVM never needs them."""
    with zipfile.ZipFile(jar) as zf:
        return "META-INF/kotlin-project-structure-metadata.json" in zf.namelist()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--cache-dir", type=pathlib.Path, default=default_cache_dir())
    args = parser.parse_args()

    graph = subprocess.run(
        [str(ROOT / "kotlin"), "show", "dependencies", "-m", "desktopApp", "-p", "jvm", "--scope", "runtime"],
        cwd=ROOT, check=True, capture_output=True, text=True,
    ).stdout

    repo = args.cache_dir / ".m2.cache"
    jars = [
        ROOT / "build/tasks/_desktopApp_jarJvm/desktopApp-jvm.jar",
        ROOT / "build/tasks/_shared_jarJvm/shared-jvm.jar",
    ]
    seen = set()
    for group, artifact, version in resolved_coordinates(graph):
        key = (group, artifact)
        if key in seen:
            continue
        seen.add(key)
        jar = repo / group.replace(".", "/") / artifact / version / f"{artifact}-{version}.jar"
        # BOMs have no JAR at all; multiplatform roots may ship a metadata-only JAR.
        if jar.is_file() and not is_multiplatform_metadata(jar):
            jars.append(jar)

    missing = [j for j in jars[:2] if not j.is_file()]
    if missing:
        sys.exit(f"Module JARs not found, run `./kotlin build` first: {missing}")
    print("\n".join(str(j) for j in jars))


if __name__ == "__main__":
    main()
