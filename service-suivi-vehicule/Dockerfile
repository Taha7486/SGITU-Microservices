# ================================
# Stage 1 : Build avec Maven
# ================================
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# Copier le pom.xml en premier (cache des dépendances)
COPY ../../SGITU-Microservices/service-suivi-vehicule/pom.xml .
RUN mvn dependency:go-offline -B

# Copier le code source et builder
COPY ../../src ./src
RUN mvn clean package -DskipTests

# ================================
# Stage 2 : Image de production
# ================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Créer un utilisateur non-root (sécurité)
RUN addgroup -S g7group && adduser -S g7user -G g7group

# Copier le JAR depuis le stage builder
COPY --from=builder /app/target/*.jar app.jar

# Changer le propriétaire
RUN chown g7user:g7group app.jar

USER g7user

# Port exposé par Spring Boot
EXPOSE 8087

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8087/api/v1/vehicules/health || exit 1

# Lancement de l'application
ENTRYPOINT ["java", "-jar", "app.jar"]