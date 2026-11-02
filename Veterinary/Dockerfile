# ── Etapa 1: build ──
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Las dependencias se resuelven en una capa aparte: mientras pom.xml no cambie,
# Docker reusa esta capa y no vuelve a bajar el repositorio entero.
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
# -Dmaven.test.skip=true (no -DskipTests): los tests de este servicio levantan el
# contexto de Spring y necesitan la base real de Supabase, que no existe durante el
# build. Además salta la compilación de los tests, no solo su ejecución.
RUN mvn clean package -Dmaven.test.skip=true

# ── Etapa 2: runtime ──
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

# artifactId=Veterinary, version=0.0.1-SNAPSHOT (pom.xml) -> Veterinary-0.0.1-SNAPSHOT.jar
COPY --from=build /app/target/Veterinary-0.0.1-SNAPSHOT.jar app.jar

RUN chown -R appuser:appgroup /app
USER appuser

# Cloud Run inyecta PORT y la app lo respeta (server.port: ${PORT:${SERVER_PORT:8082}}).
# EXPOSE es documentación: configurá "Puerto del contenedor" = 8082 en Cloud Run.
EXPOSE 8082

# MaxRAMPercentage en vez de -Xmx fijo: la JVM calcula el heap desde la memoria que
# Cloud Run le asigne al contenedor, así cambiar el límite de memoria del servicio no
# obliga a reconstruir la imagen.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
