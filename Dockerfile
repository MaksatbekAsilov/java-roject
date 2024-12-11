FROM openjdk:23
WORKDIR /app
COPY WeatherApp.java /app/WeatherApp.java
RUN javac *.java
CMD [ "java", "WeatherApp" ]