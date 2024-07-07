FROM openjdk:17
WORKDIR /app
COPY build/natproxy /app

RUN chmod +x /app/bin/server
RUN chmod +x /app/bin/client

EXPOSE 20001
EXPOSE 22222
EXPOSE 33060
EXPOSE 55555

CMD ["/app/bin/server"]
