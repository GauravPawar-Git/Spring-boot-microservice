# ---------- Stage 1: build the jar with a full JDK + Maven wrapper ----------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Copy only the build files first: this layer (all dependencies) is cached
# and re-downloaded only when pom.xml changes, not on every code change.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# Now the source code. Tests run in CI / locally (mvnw test); the image build only packages.
COPY src/ src/
RUN ./mvnw -B -q package -DskipTests

# Split the fat jar into layers (dependencies change rarely, our classes change often)
RUN java -Djarmode=tools -jar target/first-project-0.0.1-SNAPSHOT.jar extract --layers --launcher --destination extracted

# ---------- Stage 2: small runtime image, JRE only, no Maven, no source ----------
FROM eclipse-temurin:25-jre
WORKDIR /app

# Never run as root inside the container
RUN groupadd --system spring && useradd --system --gid spring --no-create-home spring
USER spring

# One COPY per layer, least-changing first, so a code change rebuilds only the last small layer
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

EXPOSE 8080

# MaxRAMPercentage: size the heap from the container's memory limit, not the host's RAM
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "org.springframework.boot.loader.launch.JarLauncher"]
