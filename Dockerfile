FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY PaperFlightEngine.java PaperFlightServer.java ./
COPY paper-plane-gust-challenge.html README-Paper-Flight.txt ./
RUN javac PaperFlightEngine.java PaperFlightServer.java

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/ ./
ENV PORT=10000
EXPOSE 10000
CMD ["java", "PaperFlightServer"]
