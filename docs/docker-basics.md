# Docker basics

Plain-words introduction to Docker as used in GridWatch. How Kafka runs inside Docker and how the services find each other: [kafka-in-docker.md](kafka-in-docker.md).

## The problem it solves

"It works on my machine." An app needs the right Java, the right database version, the right settings. Installing all of that on every laptop and server is slow and breaks easily. Docker packs an app together with everything it needs into a box that runs the same everywhere.

## Words you need

| Word | Plain meaning | GridWatch example |
|---|---|---|
| Image | A frozen box: app + everything it needs. A recipe result, read-only | `gridwatch-alert`, `postgres:17-alpine` |
| Container | A running copy of an image. You can start many from one image | `gridwatch-alert-1` |
| Dockerfile | The recipe to build an image | `alert-service/Dockerfile` |
| Docker Hub | The public shop of ready-made images | `apache/kafka`, `eclipse-temurin` |
| Tag | Version label after `:` | `postgres:17-alpine` |
| Volume | A storage folder that survives when the container is removed | `postgres-data`, `kafka` |
| Port mapping | A door from your laptop into the container: `laptop:container` | `"5435:5432"` |
| Network | Containers in the same Compose file can find each other by service name | alert reaches the database at `db:5432` |
| Docker Compose | One file that describes and starts several containers together | `docker-compose.yml` |

Comparison: an image is like a class, a container is like an object made from it.

## How a Dockerfile works

Each line is one step. Docker saves the result of every step (a **layer**) and reuses it next time if nothing above it changed. That is why we copy `pom.xml` and download libraries before copying the code: code changes often, libraries rarely.

```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS build   # start from Java 21 with build tools
WORKDIR /app                                  # work inside /app
COPY mvnw pom.xml ./                          # copy files from the laptop into the box
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline -B           # run a command while building
COPY src src
RUN ./mvnw package -DskipTests -B             # build the jar

FROM eclipse-temurin:21-jre-alpine            # second, small box: only runs Java
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar   # take only the jar from the first box
EXPOSE 8081                                   # label: listens on 8081
ENTRYPOINT ["java", "-jar", "app.jar"]        # command when the container starts
```

Two stages (**multi-stage build**): the first is a big "factory" with build tools, the second ships only the result. Smaller, faster, safer.

`.dockerignore` lists files Docker must not send into the build (`target/`, `logs/`, `.idea/`). Same idea as `.gitignore`.

## How Docker Compose works

```yaml
services:
  db:                                  # service name = address for other containers
    image: postgres:17-alpine          # ready-made image from Docker Hub
    ports: ["5435:5432"]               # laptop 5435 → container 5432
    volumes: [postgres-data:/var/lib/postgresql/data]
    healthcheck: ...                   # command that says "I'm ready"
  alert:
    build: ./alert-service             # build from our Dockerfile instead of an image
    environment:                       # overrides application.yaml
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/gridwatch
    depends_on:
      db: { condition: service_healthy }   # start only after db is healthy
volumes:
  postgres-data:
```

## Commands you will use

```bash
docker compose up -d --build        # build if needed and start everything in the background
docker compose ps                   # what is running, healthy or not
docker compose logs alert --tail 50 # last log lines of one service (-f to follow)
docker compose stop alert           # stop one service (keeps it)
docker compose start alert          # start it again
docker compose restart alert
docker compose down                 # stop and remove containers, keep data
docker compose down -v              # also delete volumes = delete all data
docker compose exec db psql -U gridwatch -d gridwatch   # run a command inside a container
docker build -t name ./folder       # build one image
docker images                       # list images
docker system df                    # disk usage
```

## Common problems

| Error | Meaning | Fix |
|---|---|---|
| `port is already allocated` / "bind: ... only one usage of each socket address" | Something on the laptop already uses that port (often the same app running in IntelliJ) | Stop the other program, or change the laptop port |
| App can't connect to the database or Kafka | Using `localhost` inside a container | Use the service name, see [kafka-in-docker.md](kafka-in-docker.md) |
| App starts before the database is ready | `depends_on` without a condition | `condition: service_healthy` + a healthcheck |
| Code change not visible | Old image reused | `up -d --build` |
| `mvnw: not found` | Windows line endings in the script | `.gitattributes` with `/mvnw text eol=lf`, or `sed -i 's/\r$//' mvnw` |
| Data gone | `down -v` deleted the volume | Use `down` without `-v` |

