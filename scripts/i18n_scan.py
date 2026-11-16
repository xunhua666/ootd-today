#!/usr/bin/env python3
"""Inventory hard-coded Chinese strings in Kotlin sources.

The count is the baseline for the English localization work: string literals are
counted, comments are ignored. Output groups results by module so the migration
can be planned and tracked per screen.

Usage:
    python3 scripts/i18n_scan.py [--json] [--top N]

Exit code is always 0: this is a reporting tool, not a gate.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from collections import Counter
from pathlib import Path

CJK = re.compile(r"[\u4e00-\u9fff\u3400-\u4dbf]")
SOURCE_ROOT = Path("app/src/main/java")


class Literal:
    __slots__ = ("path", "line", "text")

    def __init__(self, path: str, line: int, text: str) -> None:
        self.path = path
        self.line = line
        self.text = text


def scan_file(path: Path, display: str) -> list[Literal]:
    """Extract string literals with CJK characters, skipping comments."""
    try:
        text = path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        return []

    literals: list[Literal] = []
    index = 0
    line = 1
    state = "normal"  # normal | line_comment | block_comment | string | raw_string
    buffer: list[str] = []
    literal_line = 0

    while index < len(text):
        char = text[index]
        nxt = text[index + 1] if index + 1 < len(text) else ""

        if char == "\n":
            line += 1

        if state == "normal":
            if char == "/" and nxt == "/":
                state = "line_comment"
                index += 2
                continue
            if char == "/" and nxt == "*":
                state = "block_comment"
                index += 2
                continue
            if text.startswith('"""', index):
                state = "raw_string"
                buffer = []
                literal_line = line
                index += 3
                continue
            if char == '"':
                state = "string"
                buffer = []
                literal_line = line
                index += 1
                continue
            index += 1
            continue

        if state == "line_comment":
            if char == "\n":
                state = "normal"
            index += 1
            continue

        if state == "block_comment":
            if char == "*" and nxt == "/":
                state = "normal"
                index += 2
                continue
            index += 1
            continue

        if state == "string":
            if char == "\\":
                index += 2
                continue
            if char == '"':
                content = "".join(buffer)
                if CJK.search(content):
                    literals.append(Literal(display, literal_line, content))
                state = "normal"
                index += 1
                continue
            buffer.append(char)
            index += 1
            continue

        if state == "raw_string":
            if text.startswith('"""', index):
                content = "".join(buffer)
                if CJK.search(content):
                    literals.append(Literal(display, literal_line, content))
                state = "normal"
                index += 3
                continue
            buffer.append(char)
            index += 1
            continue

    return literals


def area_of(relative: str) -> str:
    prefix = "com/ootd/today/"
    path = relative[len(prefix):] if relative.startswith(prefix) else relative
    parts = path.split("/")
    if len(parts) > 1:
        return parts[0]
    return "(root)"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--json", action="store_true", help="print machine-readable JSON")
    parser.add_argument("--top", type=int, default=15, help="how many files to list")
    parser.add_argument("--root", default=str(SOURCE_ROOT), help="source root to scan")
    args = parser.parse_args()

    root = Path(args.root)
    if not root.exists():
        print(f"source root not found: {root}", file=sys.stderr)
        return 0

    all_literals: list[Literal] = []
    for path in sorted(root.rglob("*.kt")):
        relative = path.relative_to(root).as_posix()
        all_literals.extend(scan_file(path, relative))

    by_file = Counter(item.path for item in all_literals)
    by_area = Counter(area_of(item.path) for item in all_literals)
    long_literals = [item for item in all_literals if len(item.text) >= 60]

    if args.json:
        print(json.dumps({
            "total": len(all_literals),
            "files": len(by_file),
            "by_area": dict(by_area),
            "long_literals": len(long_literals),
            "top_files": by_file.most_common(args.top),
        }, ensure_ascii=False, indent=2))
        return 0

    print(f"Chinese string literals (comments excluded): {len(all_literals)}")
    print(f"Files affected: {len(by_file)}")
    print(f"Long literals (>= 60 chars, likely prompts/seed text): {len(long_literals)}")
    print()
    print("By area:")
    for area, count in by_area.most_common():
        print(f"  {area:<14} {count:>4}")
    print()
    print(f"Top {min(args.top, len(by_file))} files:")
    for path, count in by_file.most_common(args.top):
        print(f"  {count:>4}  {path}")
    print()
    print("Use --json for machine-readable output.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())