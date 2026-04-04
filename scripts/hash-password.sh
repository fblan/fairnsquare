#!/usr/bin/env bash
# Compute the SHA-256 hash of the admin password.
# Usage: ./scripts/hash-password.sh <password>
#
# The output is the value to set as ADMIN_PASSWORD_HASH in your environment /
# GitHub secret.

set -euo pipefail

if [ $# -ne 1 ]; then
  echo "Usage: $0 <password>" >&2
  exit 1
fi

password="$1"

# Try sha256sum (Linux), fall back to shasum -a 256 (macOS)
if command -v sha256sum &>/dev/null; then
  hash=$(printf '%s' "$password" | sha256sum | awk '{print $1}')
elif command -v shasum &>/dev/null; then
  hash=$(printf '%s' "$password" | shasum -a 256 | awk '{print $1}')
else
  echo "Error: neither sha256sum nor shasum found on PATH." >&2
  exit 1
fi

echo "$hash"
