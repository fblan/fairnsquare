# Local URL Configuration

## What, Why and Constraints

**What**: Replaced hardcoded tunnel URLs (ngrok, localtunnel) in `application.properties` and `vite.config.ts` with environment variable-based configuration, backed by gitignored local config files.

**Why**: Tunnel URLs (ngrok, localtunnel) are ephemeral and developer-specific. Having them in committed files caused unnecessary noise in git history and forced each developer to overwrite each other's URLs. The fix ensures the defaults are safe (localhost only) and each developer manages their own tunnel URLs locally without touching committed files.

**Constraints**:
- Local config files must be gitignored to avoid accidental commits
- Defaults must be safe and work out-of-the-box without any local config (localhost only)
- Must follow existing patterns: `.env` for Quarkus (MicroProfile Config), `.env.local` for Vite
- `.env.example` files must be committed and kept up to date

## How

### `fairnsquare-app/src/main/resources/application.properties` (modified)

Replaced:
```properties
quarkus.http.cors.origins=http://localhost:5173,http://localhost:8080,https://hermelinda-careless-mercilessly.ngrok-free.dev
```
With:
```properties
quarkus.http.cors.origins=${FAIRNSQUARE_CORS_ORIGINS:http://localhost:5173,http://localhost:8080}
```

Quarkus uses SmallRye Config, which supports `${VAR:default}` substitution in property values. The `FAIRNSQUARE_CORS_ORIGINS` variable can be set in `fairnsquare-app/.env` (read automatically by MicroProfile Config).

### `fairnsquare-app/src/main/webui/vite.config.ts` (modified)

`process.env` in `vite.config.ts` only sees OS-level environment variables — Vite loads `.env*` files after the config is evaluated. To read `.env.local` at config time, the config is converted to the functional form of `defineConfig` and uses Vite's `loadEnv(mode, process.cwd(), '')` to explicitly load all env files before building the config object. `VITE_ALLOWED_HOSTS` is then read from the result, split by comma, and used as `allowedHosts`. When not set, `allowedHosts` is `undefined`, preserving Vite's default behavior.

### `fairnsquare-app/.env.example` (created)

Documents the `FAIRNSQUARE_CORS_ORIGINS` variable. Developers copy this to `fairnsquare-app/.env` (gitignored via `.env.*` pattern in root `.gitignore`) and uncomment their tunnel URL.

### `fairnsquare-app/src/main/webui/.env.example` (created)

Documents the `VITE_ALLOWED_HOSTS` variable. Developers copy this to `fairnsquare-app/src/main/webui/.env.local` (gitignored via `*.local` pattern in webui `.gitignore`) and uncomment their tunnel hostname(s).

## Tests

No automated tests added — this is a configuration-only change with no business logic. Manual validation:
- Start the application without any local `.env` / `.env.local` files: CORS origins default to localhost, `allowedHosts` defaults to Vite default behavior.
- Create `fairnsquare-app/.env` with `FAIRNSQUARE_CORS_ORIGINS=http://localhost:5173,http://localhost:8080,https://example.ngrok-free.dev` and verify Quarkus picks it up.
- Create `fairnsquare-app/src/main/webui/.env.local` with `VITE_ALLOWED_HOSTS=example.ngrok-free.dev` and verify Vite allows connections from that host.
