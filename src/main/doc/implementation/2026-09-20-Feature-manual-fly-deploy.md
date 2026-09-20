# Feature: Manual Fly.io deployment workflow (#153)

## What, Why and Constraints

The `Docker Image CI` workflow (build and push the image to quay.io, then deploy to Fly.io) ran on every push to
`main`. The application is moving to a Dokploy deployment built from `docker/Dockerfile.jvm`, so deploying to Fly.io
automatically is no longer wanted. The workflow is now a manual action, started from the GitHub Actions tab.

Constraints: keep the build and deploy jobs unchanged, so a manual run behaves exactly like the former automatic run,
and keep the actor restriction on the deploy job (`sherine-k` / `fblan`); with `workflow_dispatch`, `github.actor` is
the person who starts the run. `Java CI with Maven` is unchanged and still runs on push and pull request.

## How

- **Modified** `.github/workflows/docker-image.yml`: trigger changed from `push` on `main` to `workflow_dispatch`.
  The image is built from the ref selected when starting the run (default branch `main`) and tagged with its commit SHA.

## Tests

No automated tests (CI configuration). To verify after merge: push to `main` and check that `Docker Image CI` does not
start, then start it from Actions → Docker Image CI → Run workflow.
