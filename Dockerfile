FROM eclipse-temurin:17-jdk

# Install Graphviz
RUN apt-get update && apt-get install -y graphviz

WORKDIR /app

# Copy runnable jar
COPY clinicflowCloud.jar /app/app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
