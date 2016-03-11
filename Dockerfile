FROM anapsix/alpine-java:jre8
MAINTAINER MoOmEeN <moomeen@gmail.com>

ENV PROJECT_DIR /opt/keendly

RUN mkdir -p $PROJECT_DIR
RUN mkdir $PROJECT_DIR/log

COPY target/universal/stage $PROJECT_DIR/app
COPY conf $PROJECT_DIR/conf

EXPOSE 9000

CMD $PROJECT_DIR"/app/bin/keendly" "-Dhttp.port=9000" "-Dlogger.file="$PROJECT_DIR"/conf/prod-logback.xml" \
    "-Dplay.crypto.secret="$APPLICATION_SECRET "-Ddb.default.url="$DB_URL \
    "-Ddb.default.username="$DB_USERNAME "-Ddb.default.password="$DB_PASSWORD
