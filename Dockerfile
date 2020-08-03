FROM openjdk:8-jre-alpine AS copy-prebuild-package
EXPOSE 8080
COPY build/libs/codetest-1.0.0-SNAPSHOT-fat.jar /usr/app/
ADD build/resources/main/conf/config.json /usr/app/conf/
ENTRYPOINT ["java","-jar","/usr/app/codetest-1.0.0-SNAPSHOT-fat.jar", "-conf", "/usr/app/conf/config.json"]