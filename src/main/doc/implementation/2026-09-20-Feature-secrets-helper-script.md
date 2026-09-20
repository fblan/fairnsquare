# Feature: Secrets helper script (hidden password prompt + `--secret`)

## What, Why and Constraints

`scripts/hash-password.sh` computed the `ADMIN_PASSWORD_HASH` from a password passed as a command-line argument. Two
problems came up while preparing a Dokploy deployment:

- The password ended up in the shell history and in `ps` output.
- `CAPTCHA_SECRET` (the other required production variable) had no helper at all.

The script now:

- Prompts for the password with hidden input when called without arguments.
- Still accepts the password as an argument (backward compatible).
- Generates a random `CAPTCHA_SECRET` with `--secret`.
- Rejects an empty password and prints usage with `-h` / `--help`.

Constraints: the hash algorithm is unchanged (unsalted SHA-256, lowercase hex, no trailing newline) because the backend
compares against exactly that format. Must keep working on Linux (`sha256sum`) and macOS (`shasum`). The prompt is
written to stderr so stdout only carries the value and stays usable in `$(...)`.

## How

- **Modified** `scripts/hash-password.sh`: split into `generate_secret` (`openssl rand -base64 32`, falling back to
  `/dev/urandom`) and `hash_password` functions; argument handling via `case` for `--help` / `--secret`; hidden
  `read -rs` prompt when no argument is given; empty-password check.
- **Modified** `README.md`: the "Generating `ADMIN_PASSWORD_HASH`" section now documents the script and `--secret`
  instead of the raw `sha256sum` one-liner.

## Tests

No automated tests (shell helper, not covered by the Maven/Vitest suites). Tested manually:

- Argument mode returns `b19eca9b…fa76` for the documented test password (matches `%test.admin.password-hash`).
- Argument mode output equals the previous README one-liner for the same password.
- Prompt mode via piped stdin returns the same hash.
- Empty input exits 1 with an error.
- `--secret` prints a 44-character base64 string.
- More than one argument exits 1; `--help` prints usage.
