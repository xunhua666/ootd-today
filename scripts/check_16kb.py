#!/usr/bin/env python3
"""Verify 16 KB page size support for Android APK/AAB native libraries.

Google Play requires apps targeting Android 15 (API 35) and above to support
16 KB page sizes. The zip-level alignment checked by `zipalign -P 16` is not
enough: every 64-bit native library must also have all `PT_LOAD` segments
aligned to at least 16 KB (0x4000).

Usage:
    python3 scripts/check_16kb.py path/to/app-release.apk [path/to/app-release.aab ...]

Exits with code 1 when any 64-bit library uses a smaller alignment.
"""

from __future__ import annotations

import struct
import sys
import zipfile
from pathlib import Path

PT_LOAD = 1
MIN_ALIGN = 0x4000
CHECKED_ABIS = {"arm64-v8a", "x86_64"}


def elf_load_aligns(data: bytes) -> list[int] | None:
    """Return PT_LOAD alignments for a 32/64-bit ELF, or None if not ELF."""
    if len(data) < 64 or data[:4] != b"\x7fELF":
        return None
    is64 = data[4] == 2
    if is64:
        e_phoff = struct.unpack_from("<Q", data, 32)[0]
        e_phentsize = struct.unpack_from("<H", data, 54)[0]
        e_phnum = struct.unpack_from("<H", data, 56)[0]
        align_offset, align_fmt = 48, "<Q"
    else:
        e_phoff = struct.unpack_from("<I", data, 28)[0]
        e_phentsize = struct.unpack_from("<H", data, 42)[0]
        e_phnum = struct.unpack_from("<H", data, 44)[0]
        align_offset, align_fmt = 28, "<I"

    aligns: list[int] = []
    for index in range(e_phnum):
        offset = e_phoff + index * e_phentsize
        if struct.unpack_from("<I", data, offset)[0] == PT_LOAD:
            aligns.append(struct.unpack_from(align_fmt, data, offset + align_offset)[0])
    return aligns


def abi_of(entry: str) -> str | None:
    parts = entry.split("/")
    if "lib" not in parts:
        return None
    index = parts.index("lib")
    if index + 2 >= len(parts):
        return None
    return parts[index + 1]


def check(path: Path) -> list[str]:
    failures: list[str] = []
    checked = 0
    with zipfile.ZipFile(path) as archive:
        for name in archive.namelist():
            if not name.endswith(".so"):
                continue
            abi = abi_of(name)
            if abi not in CHECKED_ABIS:
                continue
            checked += 1
            aligns = elf_load_aligns(archive.read(name))
            if aligns is None:
                failures.append(f"{name}: not an ELF file")
                continue
            bad = [hex(value) for value in aligns if value < MIN_ALIGN]
            if bad:
                failures.append(f"{name}: PT_LOAD alignment {bad} < 0x4000")
    print(f"{path}: checked {checked} 64-bit native libraries")
    return failures


def main(argv: list[str]) -> int:
    if len(argv) < 2:
        print(__doc__.strip(), file=sys.stderr)
        return 2

    all_failures: list[str] = []
    for raw in argv[1:]:
        path = Path(raw)
        if not path.exists():
            print(f"missing file: {path}", file=sys.stderr)
            return 2
        all_failures.extend(check(path))

    if all_failures:
        print("\nNOT 16 KB PAGE SIZE COMPATIBLE:", file=sys.stderr)
        for failure in all_failures:
            print(f"  - {failure}", file=sys.stderr)
        return 1

    print("OK: all 64-bit native libraries are 16 KB aligned")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))