# Imagem da demonstração pública (docs/DEMO.md): um serviço só, com a API servindo também o editor.
# Na hospedagem gratuita, dois serviços seriam dois servidores dormindo e acordando.

# Editor: gera o bundle estático com o Vite.
FROM node:22-alpine AS frontend
WORKDIR /frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ .
RUN npm run build

# API: o bundle entra em classpath:/static do qr-api e vai junto no jar. Os testes rodam na CI;
# aqui só empacota.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY qr-engine qr-engine
COPY qr-validation qr-validation
COPY qr-render qr-render
COPY qr-api qr-api
COPY --from=frontend /frontend/dist qr-api/src/main/resources/static
RUN mvn -B -q -pl qr-api -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 qrsafe
COPY --from=build /app/qr-api/target/qr-api-*.jar app.jar
USER qrsafe
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
