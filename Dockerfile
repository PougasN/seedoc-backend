# Use the official Maven image with JDK 17 to build the application
FROM maven:3.8.5-openjdk-17 AS build

# Set the working directory inside the container
WORKDIR /app

# Copy the pom.xml file and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the source code to the working directory
COPY src ./src

# Build the application
RUN mvn package -DskipTests

# Use the official OpenJDK 17 image to run the application
FROM openjdk:17-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the JAR file from the build stage to the working directory
# Ensure the JAR file name matches the one generated in the build stage
COPY --from=build /app/target/api-0.0.1-SNAPSHOT.jar app.jar

# Expose the port the app runs on
EXPOSE 9090

# Run the application
CMD ["java", "-jar", "app.jar"]
