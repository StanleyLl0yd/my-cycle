#!/usr/bin/env python3
from pathlib import Path
import re
import sys

USES = re.compile(r"^\s*uses:\s*([^\s#]+)")
IMAGE = re.compile(r"^\s*image:\s*([^\s#]+)")
PINNED_ACTION = re.compile(r"^[A-Za-z0-9_.-]+/[A-Za-z0-9_.\-/]+@[0-9a-fA-F]{40}$")
PINNED_IMAGE = re.compile(r"^.+:[^@\s]+@sha256:[0-9a-fA-F]{64}$")
PR_TARGET = re.compile(r"^\s*pull_request_target\s*:")

roots = [Path(".github/workflows"), Path(".github/actions")]
files = sorted(
    path
    for root in roots
    if root.exists()
    for path in root.rglob("*")
    if path.is_file() and path.suffix in {".yml", ".yaml"}
)

errors = []
for path in files:
    for number, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        line = raw.split("#", 1)[0]
        if PR_TARGET.match(line):
            errors.append(f"{path}:{number}: pull_request_target is prohibited")

        uses = USES.match(line)
        if uses:
            target = uses.group(1)
            if not target.startswith("./") and not PINNED_ACTION.fullmatch(target):
                errors.append(f"{path}:{number}: external action is not pinned to a full SHA: {target}")

        image = IMAGE.match(line)
        if image:
            target = image.group(1).strip("'\"")
            if "${{" not in target and not PINNED_IMAGE.fullmatch(target):
                errors.append(f"{path}:{number}: workflow container is not version-and-digest pinned: {target}")

if errors:
    print("\n".join(errors), file=sys.stderr)
    raise SystemExit(1)

print(f"Verified immutable CI dependencies in {len(files)} workflow/action files.")
