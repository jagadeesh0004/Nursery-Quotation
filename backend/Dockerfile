# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app
COPY . .

RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jdk

WORKDIR /app

RUN apt-get update && apt-get install -y \
    libnss3 \
    libatk-bridge2.0-0 \
    libx11-xcb1 \
    libgtk-3-0 \
    libgbm1 \
    libasound2 \
    libdrm2 \
    libxdamage1 \
    libxrandr2 \
    libcups2 \
    libxcomposite1 \
    libxfixes3 \
    libxkbcommon0 \
    libpango-1.0-0 \
    libcairo2 \
    fonts-liberation \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]