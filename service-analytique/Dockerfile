# Étape 1 : Build (Utilisation d'une image compatible ARM/Mac)
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Copie des fichiers de configuration Maven
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# Correction des permissions pour Mac/Linux (crucial sur macOS)
RUN chmod +x mvnw
# Téléchargement des dépendances
RUN ./mvnw dependency:go-offline

# Copie du code source et compilation
COPY src ./src
RUN ./mvnw package -DskipTests

# Étape 2 : Runtime (Image légère pour l'exécution)
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8088
ENTRYPOINT ["java", "-jar", "app.jar"]