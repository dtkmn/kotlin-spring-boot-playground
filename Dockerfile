# Use the official gradle image to create a build artifact.
# This is based on Debian and sets the GRADLE_HOME environment variable
FROM gradle:8.3.0-jdk20 as builder

# Set the working directory in the image
WORKDIR /usr/src/app

# Copy your source code to the image
COPY src ./src
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Package the application
RUN gradle build -x test

# Use OpenJDK JRE 20 for the runtime stage of the image
FROM openjdk:20-jdk-slim

WORKDIR /app

# Copy the jar file from the build stage
COPY --from=builder /usr/src/app/build/libs/Kotlin_playground-1.0-*.jar ./my-microservice.jar

# Expose the port your app runs on
EXPOSE 8080

# Start your application
CMD ["java", "-jar", "./my-microservice.jar"]
