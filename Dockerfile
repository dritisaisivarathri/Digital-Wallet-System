FROM maven:3.9.9-eclipse-temurin-17 AS builder

ARG MODULE_NAME

WORKDIR /workspace

COPY . .

RUN mvn -B -pl ${MODULE_NAME} -am -DskipTests clean package

FROM eclipse-temurin:17-jre

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

ARG MODULE_NAME
ARG JAR_FILE

WORKDIR /app

COPY --from=builder /workspace/${MODULE_NAME}/target/${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
