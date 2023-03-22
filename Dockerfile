FROM eclipse-temurin:8-jdk-alpine
VOLUME /tmp
ARG JAR_FILE
COPY ${JAR_FILE} webin-cli.jar
ENTRYPOINT ["java","-jar","/webin-cli.jar"]
