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
cd fairnsquare-app && mvn compile vidocq:dev
```

Starts the app on port `8080` in vidocq's dev mode: it recompiles and reloads on a Java change, seeds a sample split (`/splits/devSampleSplit2026001`), accepts the admin password `password`, and opens a debugger port (5005). The frontend lives in `fairnsquare-app/src/main/resources/META-INF/resources/` (`index.html`, `main.js`, `infra/`, `components/`, `biz/<page>/`); after editing a file, reload the browser.

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

The health check is `GET /api/health` (`/api/health/live`, `/api/health/ready`).

---

## Environment variables

Create a `.env` file at the project root (it is gitignored). The table below lists every variable the application reads.

| Variable | Required | Default | Description |
|---|---|---|---|
| `CAPTCHA_SECRET` | **yes** | — | HMAC-SHA256 secret used to sign CAPTCHA tokens |
| `ADMIN_PASSWORD_HASH` | no | — | SHA-256 hash of the admin password; without it, the admin page is off |
| `FAIRNSQUARE_DATA_PATH` | no | `data/` | Directory where split JSON files are stored |
| `FAIRNSQUARE_MAX_FILE_COUNT` | no | `5000` | Maximum number of split files retained |
| `FAIRNSQUARE_MAX_FILE_SIZE_BYTES` | no | `524288` | Maximum size of a single split file (bytes) |
| `FAIRNSQUARE_MAX_FILE_AGE_DAYS` | no | `90` | Number of days before a split file is cleaned up |
| `CAPTCHA_CHALLENGE_TTL_SECONDS` | no | `300` | How long a CAPTCHA challenge remains valid |
| `CAPTCHA_TOKEN_TTL_SECONDS` | no | `3600` | How long a solved CAPTCHA token remains valid |
| `APP_GIT_COMMIT` | no | commit of the build | Git commit hash shown in the footer. The Docker image sets it at build time from `--build-arg GIT_COMMIT` (passed by CI), otherwise from the `.git` directory of the build context (e.g. a Dokploy build), otherwise `unknown`. A value set at runtime overrides all of these |

### Generating `ADMIN_PASSWORD_HASH` and `CAPTCHA_SECRET`

```bash
./scripts/hash-password.sh            # prompts for the password (hidden), prints ADMIN_PASSWORD_HASH
./scripts/hash-password.sh --secret   # prints a random CAPTCHA_SECRET
```

The password can also be passed as an argument (`./scripts/hash-password.sh <password>`), but it then ends up in your shell history.

---

## Why this project exists

This project is my real-world playground for learning how to work with Claude Code on an actual deployed application — not a toy example, but a live product that handles real concerns: security hardening, CORS policies, cryptographic comparisons, CAPTCHA, CI/CD pipelines, and dependency management.

The goal is to explore how an AI coding assistant fits into a genuine development workflow: reviewing security trade-offs, proposing refactors, writing tests, catching bugs before they reach production, and helping maintain consistency across a multi-module codebase. Every PR on this repo is an experiment in that collaboration.
