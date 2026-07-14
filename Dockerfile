# syntax=docker/dockerfile:1.4
# Build com Maven pré-instalado — evita depender do ./mvnw (que quebra em build Docker
# quando o checkout no Windows aplica CRLF ao script ou perde o bit de execução).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# - cache mount do .m2: persiste as dependências baixadas ENTRE builds (retries acumulam,
#   não re-baixam tudo a cada mudança de pom).
# - -Dmaven.test.skip=true: pula compilar/rodar testes, não baixa deps de teste (Pact/WireMock/ArchUnit).
COPY pom.xml ./
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -q -B -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/payment-service-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
