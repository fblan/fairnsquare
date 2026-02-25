# Feature: Docker Image

## What, Why and Constraints

**What:** Added the ability to build a Docker container image for the FairNSquare application using the Quarkus `quarkus-container-image-docker` extension.

**Why:** Packaging the application as a Docker image is a prerequisite for deploying it to any container-based environment (local Docker, Docker Compose, Kubernetes, etc.). It also ensures a reproducible, self-contained runtime environment.

**Constraints:**
- The project uses **Java 25**, which is not supported by the Red Hat UBI9 OpenJDK images (which cap at Java 21). **Eclipse Temurin 25 JRE** (`eclipse-temurin:25-jre`) was chosen as the base image.
- The Quarkus `container-image-docker` extension requires **Docker to be installed and running** on the build machine.
- The application stores split data on the filesystem via `FAIRNSQUARE_DATA_PATH`. The Docker image declares `/data` as a `VOLUME` and sets `FAIRNSQUARE_DATA_PATH=/data` by default to ensure data persists across container restarts.
- Container image properties are not profile-scoped: they apply globally and are only activated when `-Dquarkus.container-image.build=true` is passed at build time.

## How

### Files created

**`fairnsquare-app/src/main/docker/Dockerfile.jvm`**
Created the standard Quarkus JVM fast-jar Dockerfile, adapted for this project:
- Base image: `eclipse-temurin:25-jre`
- Four `COPY` layers for efficient Docker layer caching: `lib/` (rarely changes), `*.jar` (launcher), `app/` (application classes), `quarkus/` (framework classes)
- Non-root user (UID 1001) for security best practices
- `VOLUME ["/data"]` to persist split data outside the container filesystem
- `FAIRNSQUARE_DATA_PATH=/data` environment variable wired to the application's data path config
- `JAVA_OPTS` includes `-Djava.util.logging.manager=org.jboss.logmanager.LogManager` required for Quarkus logging

### Files modified

**`fairnsquare-app/pom.xml`**
Added the `quarkus-container-image-docker` extension dependency. Version is managed by the Quarkus BOM.

**`fairnsquare-app/src/main/resources/application.properties`**
Added a `Container Image Configuration` section with:
- `quarkus.container-image.name=fairnsquare`
- `quarkus.container-image.group=asymetrik` → produces image `asymetrik/fairnsquare`
- `quarkus.container-image.tag=${quarkus.application.version}` → tag matches the Maven project version

## Build commands

```bash
# Build the Docker image (requires Docker running locally)
./mvnw package -Dquarkus.container-image.build=true

# Build and push to a registry
./mvnw package -Dquarkus.container-image.build=true -Dquarkus.container-image.push=true

# Run the container with persistent data
docker run -p 8080:8080 -v fairnsquare-data:/data asymetrik/fairnsquare:1.0.0-SNAPSHOT
```

## Tests

No automated tests were added. This feature is pure infrastructure configuration (a Maven dependency, a Dockerfile, and application properties). The build is validated by running `./mvnw package -Dquarkus.container-image.build=true` and confirming the image appears in `docker images`.