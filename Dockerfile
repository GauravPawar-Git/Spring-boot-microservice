# One Dockerfile for all five services. The build context is always the repo root
# (it has the shared Maven wrapper); SERVICE picks the project folder:
#   docker build -t user-service .                                   (SERVICE defaults to ".")
#   docker build -t comment-service --build-arg SERVICE=comment-service .

# ---------- Stage 1: build the jar with a full JDK + Maven wrapper ----------
# Official Docker images, pulled via the AWS ECR Public mirror (Docker Hub auth was timing out in CI)
FROM public.ecr.aws/docker/library/eclipse-temurin:25-jdk AS build
ARG SERVICE=.
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw ./
COPY ${SERVICE}/pom.xml ${SERVICE}/pom.xml
COPY ${SERVICE}/src ${SERVICE}/src

# BuildKit cache mount: ~/.m2 survives between builds (and is shared by all five services' builds),
# so dependencies are downloaded once, not once per image. It is NOT stored in the image.
RUN --mount=type=cache,target=/root/.m2 \
    chmod +x mvnw && ./mvnw -B -q -f ${SERVICE}/pom.xml package -DskipTests

# Split the fat jar into layers (dependencies change rarely, our classes change often)
RUN java -Djarmode=tools -jar ${SERVICE}/target/*.jar extract --layers --launcher --destination extracted

# ---------- Stage 2: small runtime image, JRE only, no Maven, no source ----------
FROM public.ecr.aws/docker/library/eclipse-temurin:25-jre
WORKDIR /app

# Never run as root inside the container
RUN groupadd --system spring && useradd --system --gid spring --no-create-home spring
USER spring

# One COPY per layer, least-changing first, so a code change rebuilds only the last small layer
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

# MaxRAMPercentage: size the heap from the container's memory limit, not the host's RAM
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "org.springframework.boot.loader.launch.JarLauncher"]
