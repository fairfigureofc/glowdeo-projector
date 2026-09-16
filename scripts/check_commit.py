#!/usr/bin/env python3
"""Validate this project's Conventional Commit header policy."""
import re
import subprocess
import sys
from pathlib import Path

HEADER = re.compile(r'^(feat|fix|docs|style|refactor|perf|test|build|ci|chore|revert)(\([a-zA-Z0-9][a-zA-Z0-9._/-]*\))?!?: \S.*$')


def valid(message):
    first = message.splitlines()[0] if message.splitlines() else ''
    return bool(HEADER.fullmatch(first)) and first == first.rstrip()


def main():
    if len(sys.argv) != 2:
        print('Usage: check_commit.py MESSAGE_FILE | --history', file=sys.stderr)
        return 2
    if sys.argv[1] == '--history':
        result = subprocess.run(['git', 'log', '--format=%s'], text=True, capture_output=True)
        if result.returncode:
            print(result.stderr, file=sys.stderr)
            return result.returncode
        messages = result.stdout.splitlines()
    else:
        messages = [Path(sys.argv[1]).read_text()]
    invalid = [m.splitlines()[0] if m.splitlines() else '(empty)' for m in messages if not valid(m)]
    if invalid:
        print('Use type(scope): description, e.g. feat(pairing): add code entry', file=sys.stderr)
        for header in invalid:
            print(f'Invalid: {header}', file=sys.stderr)
        return 1
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
