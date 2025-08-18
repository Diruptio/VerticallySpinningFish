FROM alpine:latest
LABEL authors="Fabi.exe"

# Install Adoptium JRE
RUN wget -O /etc/apk/keys/adoptium.rsa.pub https://packages.adoptium.net/artifactory/api/security/keypair/public/repositories/apk
RUN echo 'https://packages.adoptium.net/artifactory/apk/alpine/main' >> /etc/apk/repositories
RUN apk add temurin-21-jre

# Create the application
RUN mkdir /app
COPY build/libs/VerticallySpinningFish.jar /app/VerticallySpinningFish.jar

VOLUME /data
VOLUME /var/run/docker.sock
EXPOSE 7000

WORKDIR /data
ENTRYPOINT ["java", "-jar", "/app/VerticallySpinningFish.jar"]
