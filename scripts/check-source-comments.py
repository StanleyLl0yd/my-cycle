from pathlib import Path
import re
import sys

root = Path(__file__).resolve().parents[1] / "app" / "src"
violations = []

for path in sorted(root.rglob("*.kt")):
    in_block = False
    for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
        comments = []
        index = 0
        while index < len(line):
            if in_block:
                end = line.find("*/", index)
                if end == -1:
                    comments.append(line[index:])
                    break
                comments.append(line[index:end])
                in_block = False
                index = end + 2
                continue

            line_comment = line.find("//", index)
            block_comment = line.find("/*", index)
            positions = [pos for pos in (line_comment, block_comment) if pos != -1]
            if not positions:
                break

            position = min(positions)
            if position == line_comment:
                comments.append(line[position + 2:])
                break

            in_block = True
            index = position + 2

        for comment in comments:
            text = comment.strip().lstrip("*").strip()
            if not text:
                continue
            if any(ord(char) > 127 for char in text) or re.search(r"\b(?:TODO|FIXME)\b", text, re.IGNORECASE):
                violations.append(f"{path.relative_to(root.parents[1])}:{number}: {text}")

if violations:
    print("Source comments must be necessary, English-only, and free of TODO/FIXME markers.")
    print("\n".join(violations))
    sys.exit(1)
