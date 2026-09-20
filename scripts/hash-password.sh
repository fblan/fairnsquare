#!/usr/bin/env bash
# Prepare the production secrets of FairNSquare.
#
# Usage:
#   ./scripts/hash-password.sh              # prompt for the admin password (hidden input)
#   ./scripts/hash-password.sh <password>   # hash the given password (visible in shell history / ps)
#   ./scripts/hash-password.sh --secret     # generate a random CAPTCHA_SECRET
#
# The password mode prints the SHA-256 hash to set as ADMIN_PASSWORD_HASH in your
# environment / GitHub secret / Dokploy. The --secret mode prints the value for
# CAPTCHA_SECRET.

set -euo pipefail

usage() {
  sed -n '2,11p' "$0" | sed 's/^# \{0,1\}//' >&2
}

generate_secret() {
  if command -v openssl &>/dev/null; then
    openssl rand -base64 32
  else
    head -c 32 /dev/urandom | base64
  fi
}

hash_password() {
  local password="$1"
  # Try sha256sum (Linux), fall back to shasum -a 256 (macOS)
  if command -v sha256sum &>/dev/null; then
    printf '%s' "$password" | sha256sum | awk '{print $1}'
  elif command -v shasum &>/dev/null; then
    printf '%s' "$password" | shasum -a 256 | awk '{print $1}'
  else
    echo "Error: neither sha256sum nor shasum found on PATH." >&2
    exit 1
  fi
}

if [ $# -gt 1 ]; then
  usage
  exit 1
fi

case "${1:-}" in
  -h|--help)
    usage
    exit 0
    ;;
  --secret)
    generate_secret
    exit 0
    ;;
esac

if [ $# -eq 1 ]; then
  password="$1"
else
  # Prompt goes to stderr so that stdout only carries the hash
  IFS= read -rsp "Admin password: " password
  echo >&2
fi

if [ -z "$password" ]; then
  echo "Error: password must not be empty." >&2
  exit 1
fi

hash_password "$password"
