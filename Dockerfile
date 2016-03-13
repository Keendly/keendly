FROM anapsix/alpine-java:jre8
MAINTAINER MoOmEeN <moomeen@gmail.com>

ENV PROJECT_DIR /opt/keendly

RUN mkdir -p $PROJECT_DIR
RUN mkdir $PROJECT_DIR/log

COPY target/universal/stage $PROJECT_DIR/app
COPY conf $PROJECT_DIR/conf

EXPOSE 9000

RUN echo $DB_URL
RUN echo $DB_PASSWORD
RUN echo $DB_USERNAME

CMD $PROJECT_DIR/app/bin/keendly -Dhttp.port=disabled -Dhttps.port=9443 -Dlogger.file=$PROJECT_DIR/conf/prod-logback.xml -Ddb.default.url=$DB_URL -Ddb.default.username=$DB_USERNAME -Ddb.default.password=$DB_PASSWORD -Dplay.server.https.keyStore.path=/opt/ssl/keystore.jks -Dplay.server.https.keyStore.password=$KEYSTORE_PASSWORD
