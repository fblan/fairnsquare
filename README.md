# FairNSquare

A frictionless expense-sharing web app for groups with variable participation.

**Live instance:** https://fairnsquare.web.asymetrik.org

---

## Build & Run

### Prerequisites

- Java 25 (OpenJDK)
- Maven 3.x

No Node.js: the frontend is plain JavaScript web components, served as they are (no build step).

### Development mode

```bash
mvn quarkus:dev
```

Starts the app on port `8080`. The frontend lives in `fairnsquare-app/src/main/resources/META-INF/resources/` (`index.html`, `main.js`, `infra/`, `components/`, `biz/<page>/`); after editing a file, reload the browser.

### Full build

```bash
mvn clean install
```

Compiles the app and runs all the tests, including the Playwright end-to-end tests of the frontend (`make test-backend` / `make test-frontend` run one side only).

### Docker

```bash
docker build -f docker/Dockerfile.jvm -t fairnsquare .
docker run -p 8080:8080 --env-file .env fairnsquare
```

---

## Environment variables

Create a `.env` file at the project root (it is gitignored). The table below lists every variable the application reads.

| Variable | Required | Default | Description |
|---|---|---|---|
| `CAPTCHA_SECRET` | **yes** | — | HMAC-SHA256 secret used to sign CAPTCHA tokens |
| `ADMIN_PASSWORD_HASH` | **yes**, unless OIDC | — | SHA-256 hash of the admin password. Not used when `ADMIN_OIDC_ISSUER` is set |
| `ADMIN_OIDC_ISSUER` | no | — | URL of an OIDC provider (e.g. `https://id.example.com` for Pocket ID). When set, admins sign in with the provider instead of the password; see [Admin sign-in with Pocket ID](#admin-sign-in-with-pocket-id) |
| `ADMIN_OIDC_CLIENT_ID` | with OIDC | — | The provider's client ID for FairNSquare |
| `ADMIN_OIDC_CLIENT_SECRET` | with OIDC | — | The provider's client secret for FairNSquare |
| `ADMIN_OIDC_GROUP` | no | — | Only the members of this provider group are admins. Without it, everyone the provider signs in is |
| `ADMIN_OIDC_PROVIDER_NAME` | no | `Pocket ID` | The name on the admin page's "Sign in with …" button |
| `FAIRNSQUARE_CORS_ORIGINS` | **yes** | dev: `http://localhost:8080` | Comma-separated list of allowed CORS origins |
| `FAIRNSQUARE_DATA_PATH` | no | `data/` | Directory where split JSON files are stored |
| `FAIRNSQUARE_MAX_FILE_COUNT` | no | `5000` | Maximum number of split files retained |
| `FAIRNSQUARE_MAX_FILE_SIZE_BYTES` | no | `524288` | Maximum size of a single split file (bytes) |
| `FAIRNSQUARE_MAX_FILE_AGE_DAYS` | no | `90` | Number of days before a split file is cleaned up |
| `CAPTCHA_CHALLENGE_TTL_SECONDS` | no | `300` | How long a CAPTCHA challenge remains valid |
| `CAPTCHA_TOKEN_TTL_SECONDS` | no | `3600` | How long a solved CAPTCHA token remains valid |
| `BACKUP_S3_BUCKET` | no | — | S3 bucket of the split backups. When set, all the splits are backed up there, encrypted; see [Backups](#backups) |
| `BACKUP_ENCRYPTION_KEY` | with backups | — | Key of the backups: 32 random bytes, base64 (`./scripts/hash-password.sh --secret`). Keep a copy outside the server: without it, the archives can't be read |
| `BACKUP_S3_ENDPOINT` | no | AWS | Endpoint of an S3-compatible provider (OVH, Scaleway, Backblaze, MinIO...), e.g. `https://s3.gra.io.cloud.ovh.net` |
| `BACKUP_S3_REGION` | no | `us-east-1` | Region of the bucket |
| `BACKUP_S3_PATH_STYLE` | no | `false` | `true` for providers that need path-style URLs (MinIO...) |
| `BACKUP_S3_ACCESS_KEY` / `BACKUP_S3_SECRET_KEY` | no | AWS default credentials | Credentials of the bucket: list, read, write and delete objects |
| `BACKUP_S3_PREFIX` | no | `fairnsquare/` | Folder of the archives in the bucket, e.g. `staging/` to share a bucket with production |
| `BACKUP_CRON` | no | `0 0 3 * * ?` | When the automatic backup runs (Quartz cron); it makes no archive if no split changed since the last one |
| `BACKUP_KEEP` | no | `30` | How many archives to keep; older ones are deleted after each backup. `0` keeps them all |
| `APP_GIT_COMMIT` | no | commit of the build | Git commit hash shown in the footer. The Docker image sets it at build time from `--build-arg GIT_COMMIT` (passed by CI), otherwise from the `.git` directory of the build context (e.g. a Dokploy build), otherwise `unknown`. A value set at runtime overrides all of these |

### Generating `ADMIN_PASSWORD_HASH` and `CAPTCHA_SECRET`

```bash
./scripts/hash-password.sh            # prompts for the password (hidden), prints ADMIN_PASSWORD_HASH
./scripts/hash-password.sh --secret   # prints a random CAPTCHA_SECRET
```

The password can also be passed as an argument (`./scripts/hash-password.sh <password>`), but it then ends up in your shell history.

### Admin sign-in with Pocket ID

1. In Pocket ID, add an OIDC client for FairNSquare with the callback URL `https://<fairnsquare host>/api/admin/session/login`. To keep the admin pages to some users, give the client allowed user groups, or set `ADMIN_OIDC_GROUP`.
2. Set `ADMIN_OIDC_ISSUER` (the Pocket ID URL), `ADMIN_OIDC_CLIENT_ID` and `ADMIN_OIDC_CLIENT_SECRET`, and restart.

`/admin` then shows "Sign in with Pocket ID". Signing out of the admin page doesn't sign out of Pocket ID. Remove `ADMIN_OIDC_ISSUER` to go back to the password.

### Backups

With `BACKUP_S3_BUCKET` and `BACKUP_ENCRYPTION_KEY` set, the app backs up all the splits to the bucket:

- **Automatically**, every night at 3 am (`BACKUP_CRON`). There's no new archive if no split changed since the last one.
- **On demand**, with **Back up now** in the Backups section of `/admin`.

Each archive (`<prefix>splits-<date>.fnsbackup`) is encrypted on the server (AES-256-GCM) before it is uploaded. The bucket only ever holds encrypted data. After each backup, only the `BACKUP_KEEP` most recent archives are kept.

`/admin` lists the archives. **Compare** shows how many splits are new, deleted, changed and unchanged since an archive.

**Restoring**: download the archive from the bucket, then decrypt it (Java 25, nothing else to install):

```bash
BACKUP_ENCRYPTION_KEY=... java scripts/DecryptBackup.java splits-<date>.fnsbackup splits.zip
```

`splits.zip` holds `splits/<id>.zip` (copy them into `<FAIRNSQUARE_DATA_PATH>/default/` to restore splits) and `manifest.json`.

---

## Why this project exists

This project is my real-world playground for learning how to work with Claude Code on an actual deployed application — not a toy example, but a live product that handles real concerns: security hardening, CORS policies, cryptographic comparisons, CAPTCHA, CI/CD pipelines, and dependency management.

The goal is to explore how an AI coding assistant fits into a genuine development workflow: reviewing security trade-offs, proposing refactors, writing tests, catching bugs before they reach production, and helping maintain consistency across a multi-module codebase. Every PR on this repo is an experiment in that collaboration.
