from hashlib import sha256
from pathlib import Path
import struct

paths = sorted(Path("store/rustore/screenshots/phone").glob("*.png"))
if len(paths) < 3:
    raise SystemExit("RuStore requires at least three phone screenshots")

digests = []
for path in paths:
    data = path.read_bytes()
    if data[:8] != b"\x89PNG\r\n\x1a\n" or data[12:16] != b"IHDR":
        raise SystemExit(f"{path} is not a valid PNG")
    width, height = struct.unpack(">II", data[16:24])
    if (width, height) != (1080, 1920):
        raise SystemExit(f"{path} has unexpected dimensions {(width, height)}")
    if len(data) > 3 * 1024 * 1024:
        raise SystemExit(f"{path} exceeds 3 MB")
    if len(data) < 20 * 1024:
        raise SystemExit(f"{path} is unexpectedly small")
    digests.append(sha256(data).hexdigest())
    print(f"{path}: 1080x1920, {len(data)} bytes")

if len(set(digests)) != len(digests):
    raise SystemExit("RuStore screenshots are not visually distinct")
