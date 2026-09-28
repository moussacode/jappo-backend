# ── Étape 1 : Construction de l'application Spring Boot ────────────────────────
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

# Optimisation du cache des dépendances Maven
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# ── Étape 2 : Image d'exécution légère JRE 21 ─────────────────────────────────
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Création du dossier d'uploads pour les fichiers (livrables, ressources, etc.)
RUN mkdir -p /app/uploads && chmod 777 /app/uploads

COPY --from=build /app/target/*.jar app.jar

ENV SERVER_PORT=8080

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
