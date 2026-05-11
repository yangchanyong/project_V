FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY *.jar app.jar
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]
