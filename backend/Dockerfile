# ---------- Build Stage ----------
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# ---------- Runtime Stage ----------
FROM eclipse-temurin:17-jre-noble

RUN apt-get update && apt-get install -y --no-install-recommends \
	python3-pip libpango-1.0-0 libpangoft2-1.0-0 libharfbuzz-subset0 fontconfig \
 && pip3 install --no-cache-dir --break-system-packages weasyprint==70.0 \
 && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/src/main/resources/fonts/*.ttf /usr/local/share/fonts/nursery/
RUN fc-cache -f

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=60", "-jar", "app.jar"]