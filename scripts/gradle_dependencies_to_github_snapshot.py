#!/usr/bin/env python3
import argparse
import json
import re
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import quote

COORDINATE_RE = re.compile(
    r"^(?P<group>[^:\s]+):(?P<artifact>[^:\s]+)(?::(?P<requested>\S+))?"
)
RESOLVED_RE = re.compile(r"\s+->\s+(?P<resolved>\S+)")


def parse_dependency(line: str):
    if "--- " not in line:
        return None

    prefix, coordinate = line.split("--- ", 1)
    coordinate = coordinate.strip()

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

    if not version or version in {"FAILED", "(c)", "(n)", "(*)"}:
        raise ValueError(f"Missing resolved version: {coordinate}")
    if version[0] in "{[(":
        raise ValueError(f"Non-concrete resolved version: {coordinate}")

    direct = prefix.strip() in {"+", "\\"}

    return (
        match.group("group"),
        match.group("artifact"),
        version,
        direct,
    )


def purl(group: str, artifact: str, version: str) -> str:
    return (
        "pkg:maven/"
        f"{quote(group, safe='')}/"
        f"{quote(artifact, safe='')}@"
        f"{quote(version, safe='')}"
    )


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Convert Gradle releaseRuntimeClasspath to a GitHub dependency snapshot."
    )
    parser.add_argument("report", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--sha", required=True)
    parser.add_argument("--ref", required=True)
    parser.add_argument("--job-id", required=True)
    args = parser.parse_args()

    packages = {}

    for line in args.report.read_text(encoding="utf-8").splitlines():
        parsed = parse_dependency(line)
        if not parsed:
            continue

        group, artifact, version, direct = parsed
        key = f"{group}:{artifact}"

        existing = packages.get(key)
        if existing and existing["version"] != version:
            raise SystemExit(
                f"Conflicting resolved versions for {key}: "
                f"{existing['version']} and {version}"
            )

        if existing:
            existing["direct"] = existing["direct"] or direct
        else:
            packages[key] = {
                "group": group,
                "artifact": artifact,
                "version": version,
                "direct": direct,
            }

    if not packages:
        raise SystemExit("No resolved Maven runtime dependencies found")

    resolved = {}

    for key, package in sorted(packages.items()):
        resolved[key] = {
            "package_url": purl(
                package["group"],
                package["artifact"],
                package["version"],
            ),
            "relationship": "direct" if package["direct"] else "indirect",
            "scope": "runtime",
            "dependencies": [],
        }

    snapshot = {
        "version": 0,
        "sha": args.sha,
        "ref": args.ref,
        "job": {
            "correlator": "my-cycle-release-runtime",
            "id": args.job_id,
        },
        "detector": {
            "name": "My Cycle releaseRuntimeClasspath",
            "version": "1",
            "url": (
                "https://github.com/StanleyLl0yd/my-cycle/blob/"
                "main/scripts/gradle_dependencies_to_github_snapshot.py"
            ),
        },
        "scanned": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
        "manifests": {
            "app/releaseRuntimeClasspath": {
                "name": "app:releaseRuntimeClasspath",
                "file": {
                    "source_location": "app/build.gradle.kts",
                },
                "resolved": resolved,
            }
        },
    }

    args.output.write_text(
        json.dumps(snapshot, indent=2) + "\n",
        encoding="utf-8",
    )

    print(f"Wrote GitHub dependency snapshot with {len(resolved)} runtime packages.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
