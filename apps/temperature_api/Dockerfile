FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app
COPY src/TemperatureApiServer.java .
RUN javac -encoding UTF-8 -d /app/out TemperatureApiServer.java

FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=builder /app/out/ .
EXPOSE 8081
CMD ["java", "TemperatureApiServer"]
