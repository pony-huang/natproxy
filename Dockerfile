FROM openjdk:17-ea-slim
WORKDIR /app
COPY ../build/natproxy /app/natproxy
RUN chmod +x /app/natproxy/bin/server
RUN cd /app/natproxy/bin/
CMD ["server &"]