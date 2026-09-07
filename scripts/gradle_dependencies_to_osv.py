#!/usr/bin/env python3
import argparse
import json
import re
from pathlib import Path

COORDINATE_RE = re.compile(
    r"^(?P<group>[^:\\s]+):(?P<artifact>[^:\\s]+)(?::(?P<requested>\\S+))?"
)
RESOLVED_RE = re.compile(r"\\s+->\\s+(?P<resolved>\\S+)")


def parse_dependency(line: str):
    if "--- " not in line:
        return None

    coordinate = line.split("--- ", 1)[1].strip()
    if coordinate.startswith("project "):
        return None
    if coordinate.endswith(" (c)") or coordinate.endswith(" (n)"):
        return None
    if " FAILED" in f" {coordinate}":
        raise ValueError(f"Unresolved dependency: {coordinate}")

    match = COORDINATE_RE.match(coordinate)
    if not match:
        return None

    resolved_match = RESOLVED_RE.search(coordinate)
    version = resolved_match.group("resolved") if resolved_match else match.group("requested")
    if not version or version in {"FAILED", "(n)", "(c)", "(*)"}:
        raise ValueError(f"Missing resolved version: {coordinate}")
    if version[0] in "{[(":
        raise ValueError(f"Non-concrete resolved version: {coordinate}")

    return match.group("group"), match.group("artifact"), version


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Convert a Gradle dependency report to an OSV custom lockfile."
    )
    parser.add_argument("report", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()

    packages = set()
    for line in args.report.read_text(encoding="utf-8").splitlines():
        dependency = parse_dependency(line)
        if dependency:
            packages.add(dependency)

    if not packages:
        raise SystemExit("No resolved Maven packages found in Gradle dependency report")

    osv_packages = [
        {
            "package": {
                "name": f"{group}:{artifact}",
                "version": version,
                "ecosystem": "Maven",
            }
        }
        for group, artifact, version in sorted(packages)
    ]

    args.output.write_text(
        json.dumps({"results": [{"packages": osv_packages}]}, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"Wrote {len(osv_packages)} resolved Maven packages to {args.output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
