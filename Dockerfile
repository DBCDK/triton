FROM docker.dbc.dk/payara5-micro:latest

ENV USER gfish
USER $USER

COPY target/triton.war wars

EXPOSE 8080
