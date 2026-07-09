# FairNSquare

A frictionless expense-sharing web app for groups with variable participation.

**Live instance:** https://fairnsquare.web.asymetrik.org

---

## Build & Run

### Prerequisites

- Java 25 (OpenJDK)
- Maven 3.x
- Node.js 24

### Development mode

```bash
mvn quarkus:dev
```

Starts the backend on port `8080` and a Vite dev server on port `5173` with hot reload.

### Full build

```bash
mvn clean install
```

Compiles the backend and the Svelte frontend together via Quarkus Quinoa.

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
| `ADMIN_PASSWORD_HASH` | **yes** | — | SHA-256 hash of the admin password |
| `FAIRNSQUARE_CORS_ORIGINS` | no | `http://localhost:5173,http://localhost:8080` | Comma-separated list of allowed CORS origins |
| `FAIRNSQUARE_DATA_PATH` | no | `data/` | Directory where split JSON files are stored |
| `FAIRNSQUARE_MAX_FILE_COUNT` | no | `5000` | Maximum number of split files retained |
| `FAIRNSQUARE_MAX_FILE_SIZE_BYTES` | no | `524288` | Maximum size of a single split file (bytes) |
| `FAIRNSQUARE_MAX_FILE_AGE_DAYS` | no | `90` | Number of days before a split file is cleaned up |
| `CAPTCHA_CHALLENGE_TTL_SECONDS` | no | `300` | How long a CAPTCHA challenge remains valid |
| `CAPTCHA_TOKEN_TTL_SECONDS` | no | `3600` | How long a solved CAPTCHA token remains valid |
| `APP_GIT_COMMIT` | no | — | Git commit hash, injected automatically by CI |
| `VITE_ALLOWED_HOSTS` | no | — | Additional hosts allowed by the Vite dev server (e.g. an ngrok tunnel) |

### Generating `ADMIN_PASSWORD_HASH`

```bash
echo -n "your-password" | sha256sum | awk '{print $1}'
```

---

## Why this project exists

This project is my real-world playground for learning how to work with Claude Code on an actual deployed application — not a toy example, but a live product that handles real concerns: security hardening, CORS policies, cryptographic comparisons, CAPTCHA, CI/CD pipelines, and dependency management.

The goal is to explore how an AI coding assistant fits into a genuine development workflow: reviewing security trade-offs, proposing refactors, writing tests, catching bugs before they reach production, and helping maintain consistency across a multi-module codebase. Every PR on this repo is an experiment in that collaboration.
