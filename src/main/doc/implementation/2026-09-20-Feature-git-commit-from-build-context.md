# Feature: Resolve the displayed git commit from the build context (#153)

## What, Why and Constraints

The footer shows the git commit (`APP_GIT_COMMIT`, default `unknown`). It was only filled when CI passed
`--build-arg GIT_COMMIT=<sha>` (the GitHub Action used for Fly.io). An image built by Dokploy from the repository
received no build arg and showed `unknown`, so the Dokploy and Fly.io deployments could not be compared.

The image now finds the commit by itself when no build arg is given, so a Dokploy build shows the deployed commit
without manual configuration.

Constraints: `--build-arg GIT_COMMIT` must keep working and take precedence (CI unchanged); a build without `.git` in
the context (tarball, some CI setups) must still succeed and show `unknown`; the runtime image must not need git.

## How

- **Modified** `docker/Dockerfile.jvm`:
  - New stage `gitinfo` (`ubi9/ubi-minimal` + git). `COPY pom.xml .gi[t] /gitmeta/` copies the content of `.git` when
    it exists and matches nothing otherwise, so the build never fails on a missing `.git`. The commit is written to
    `/git-commit`: the build arg if given, else `git --git-dir=/gitmeta rev-parse HEAD`, else `unknown`.
  - Runtime stage: `COPY --from=gitinfo /git-commit /deployments/git-commit`; the former `ARG`/`ENV APP_GIT_COMMIT`
    is removed and the entrypoint exports `APP_GIT_COMMIT` from that file unless the variable is already set at
    runtime (so a runtime value still overrides).
  - The stage is independent of the Maven stage, so a new commit only rebuilds this small stage, not the application.
- **Modified** `README.md`: description of `APP_GIT_COMMIT`.
- `.github/workflows/docker-image.yml` is unchanged (it still passes `GIT_COMMIT=${{ github.sha }}`).

## Tests

No automated tests (image build). Verified manually:

- `gitinfo` stage with `.git` in the context and no build arg: returns the current `HEAD` sha.
- `--build-arg GIT_COMMIT=abc1234`: returns `abc1234`.
- Context without `.git`: build succeeds and returns `unknown`.
- Full image built (`docker build -f docker/Dockerfile.jvm .`) and started with the required env vars: `/api/info`
  returns `"commit":"<HEAD sha>"`; started with `-e APP_GIT_COMMIT=runtime-override` it returns `runtime-override`.
