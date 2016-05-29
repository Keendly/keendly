FROM anapsix/alpine-java:jre8
MAINTAINER MoOmEeN <moomeen@gmail.com>

ENV PROJECT_DIR /opt/keendly

RUN mkdir -p $PROJECT_DIR
RUN mkdir $PROJECT_DIR/log

COPY target/universal/stage $PROJECT_DIR/app
COPY conf $PROJECT_DIR/conf

EXPOSE 9000

CMD $PROJECT_DIR/app/bin/keendly -Dhttp.port=9000 -Dlogger.file=$PROJECT_DIR/conf/prod-logback.xml \
  -Dplay.crypto.secret=$APPLICATION_SECRET -Ddb.default.url=$DB_URL -Ddb.default.username=$DB_USERNAME \
  -Ddb.default.password=$DB_PASSWORD -Dinoreader.client_id=$INOREADER_CLIENT_ID \
  -Dinoreader.client_secret=$INOREADER_CLIENT_SECRET -Dinoreader.redirect_uri=$INOREADER_REDIRECT_URI \
  -Dcom.sun.management.jmxremote=true -Dcom.sun.management.jmxremote.port=1616 \
  -Dcom.sun.management.jmxremote.password.file=$PROJECT_DIR/log/jmxremote.password
