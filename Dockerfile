FROM navikt/java:17-appdynamics

ENV APPD_ENABLED=true
ENV APP_NAME=bidrag-person-hendelse
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

COPY ./target/bidrag-person-hendelse.jar "app.jar"
