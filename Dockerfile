# Build com Maven pré-instalado — evita depender do ./mvnw (que quebra em build Docker
# quando o checkout no Windows aplica CRLF ao script ou perde o bit de execução).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Empacota pulando testes (-Dmaven.test.skip pula compilar E rodar): não baixa as
# dependências de teste pesadas (Pact, WireMock, ArchUnit), deixando o build da imagem
# muito mais rápido. A verificação de testes fica no CI (mvn test), não no Docker.
COPY pom.xml ./
COPY src ./src
RUN mvn -q -B -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/payment-service-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
