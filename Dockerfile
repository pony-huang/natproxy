FROM openjdk:17-ea-slim

WORKDIR /app

COPY ../build/natproxy /app/natproxy

CMD ["server"]