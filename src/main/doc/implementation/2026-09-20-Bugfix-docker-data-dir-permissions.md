# Bugfix: `/data` not writable by the container user (#153)

## What, Why and Constraints

On a first Dokploy deployment, creating a split failed with HTTP 500:

```
java.nio.file.AccessDeniedException: /data/default
  at FileSystemService.saveFile (Files.createDirectories)
```

`docker/Dockerfile.jvm` runs the app as user `1001`, declares `VOLUME ["/data"]` and sets
`FAIRNSQUARE_DATA_PATH=/data`, but never creates `/data` nor gives it to `1001` (only `/deployments` was handled).
Docker therefore creates `/data` as `root:root` when a volume is mounted, and the app cannot write to it. The health
check still passes because nothing is written at startup, so the failure only shows on the first write.

Constraints: keep running as non-root (`USER 1001`); the ownership must be set **before** the `VOLUME` instruction
(changes made to the directory after `VOLUME` are discarded); the directory must stay group-writable (`g+rwX`, group
`root`) to keep working on platforms that run containers with an arbitrary UID in group `0`.

## How

- **Modified** `docker/Dockerfile.jvm`: in the runtime stage, the existing `RUN` that prepares `/deployments` also
  runs `mkdir -p /data && chown 1001:root /data && chmod g+rwX /data`.

Docker only copies the image's ownership into a **new, empty** named volume. A volume created by a previous
deployment stays root-owned and must be recreated (it is empty if no split was ever saved) or fixed once with
`chown -R 1001:0` on the volume's directory.

## Tests

No automated tests (image build). Reproduced and verified manually with a minimal image built from the same base
(`ubi9/openjdk-25-runtime`), a fresh named volume and `mkdir -p /data/default` as user `1001`:

- Without the change: `mkdir` fails (permission denied).
- With the change: `mkdir` succeeds.

To verify after deployment: create a split, redeploy, and check that the split is still there.
