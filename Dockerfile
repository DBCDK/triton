FROM docker.dbc.dk/payara5-micro:10

ENV USER gfish
USER $USER

COPY target/triton.war wars

EXPOSE 8080
