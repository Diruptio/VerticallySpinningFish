FROM alpine:latest
LABEL authors="Fabi.exe"

# Args
ARG HOST_UID=0
ARG HOST_GID=0

# Install usermod & groupmod (shadow)
RUN echo http://dl-2.alpinelinux.org/alpine/edge/community/ >> /etc/apk/repositories
RUN apk add -U shadow

# Install Adoptium JRE
RUN wget -O /etc/apk/keys/adoptium.rsa.pub https://packages.adoptium.net/artifactory/api/security/keypair/public/repositories/apk
RUN echo 'https://packages.adoptium.net/artifactory/apk/alpine/main' >> /etc/apk/repositories
RUN apk add temurin-21-jre

# Add user & group
RUN addgroup --gid $HOST_GID vsf
RUN adduser --uid $HOST_UID --gid $HOST_GID vsf
USER $HOST_UID:$HOST_GID

# Create the application
RUN mkdir /app
COPY build/libs/VerticallySpinningFish.jar /app/VerticallySpinningFish.jar

VOLUME /data
VOLUME /var/run/docker.sock
EXPOSE 7000
WORKDIR /data

CMD ["java", "-jar", "/app/VerticallySpinningFish.jar"]
