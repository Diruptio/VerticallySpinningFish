FROM alpine:latest
LABEL authors="Fabi.exe"

# Install Adoptium JRE
RUN wget -O /etc/apk/keys/adoptium.rsa.pub https://packages.adoptium.net/artifactory/api/security/keypair/public/repositories/apk
RUN echo 'https://packages.adoptium.net/artifactory/apk/alpine/main' >> /etc/apk/repositories
RUN apk add temurin-25-jre

EXPOSE 25565
VOLUME /data

WORKDIR /data
CMD ["java", "-Xms256M", "-Xmx256M", "-jar", "server.jar", "--ignore-config-servers"]
