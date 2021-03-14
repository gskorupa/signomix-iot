# Copyright (C) Grzegorz Skorupa 2021.
# Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).

FROM azul/zulu-openjdk-alpine:13

WORKDIR /usr/signomix
RUN mkdir /usr/signomix/logs

COPY target/signomix-iot.jar /usr/signomix/
COPY src/main/resources/settings.json /usr/signomix/config/
COPY src/main/resources/device-script-template.js /usr/signomix/config/
COPY src/main/resources/payload-decoder-envelope.js /usr/signomix/config/

#CMD ["java", "-Xms100m",  "-Xmx1g", "--illegal-access=deny", "-cp", "signomix.jar:jboss-client.jar:javax.activation-1.2.0.jar:jaxb-api-2.4.0.jar:jaxb-core-2.3.0.1.jar:jaxb-impl-2.4.0.jar", "org.cricketmsf.Runner", "-r", "-c", "config/settings.json"]
CMD ["java", "-Xms100m",  "-Xmx1g", "-jar", "./signomix-iot.jar", "-r", "-c", "config/settings.json"]

