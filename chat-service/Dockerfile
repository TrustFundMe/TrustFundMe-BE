FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/chat-service.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
