FROM docker.dbc.dk/dbc-payara-micro-logback:4

ENV USER gfish
USER $USER

COPY target/triton.war wars

EXPOSE 8080
