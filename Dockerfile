FROM adoptopenjdk/openjdk11

RUN mkdir /opt/app
COPY build/libs/HolySheet-*-all.jar /opt/app/app.jar

EXPOSE 8888

CMD ["java", "-jar", "/opt/app/app.jar", "-g=8888"]
