FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp
ARG JAR_FILE
COPY ${JAR_FILE} webin-cli.jar
ENTRYPOINT ["java","-jar","/webin-cli.jar"]
