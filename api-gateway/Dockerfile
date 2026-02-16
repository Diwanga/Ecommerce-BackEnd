FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Install curl for health check
RUN apk add --no-cache curl
COPY target/*.jar app.jar
EXPOSE 8060
ENTRYPOINT ["java", "-jar", "app.jar"]