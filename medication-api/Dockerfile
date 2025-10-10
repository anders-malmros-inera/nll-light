# Build the multi-module project and package the API module
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml ./pom.xml
COPY medication-api/pom.xml medication-api/pom.xml
COPY medication-web/pom.xml medication-web/pom.xml
COPY medication-api/src medication-api/src
COPY medication-web/src medication-web/src
RUN mvn -q -pl medication-api -am -DskipTests package

# Run the API
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/medication-api/target/medication-api-*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
