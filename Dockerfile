FROM docker.dbc.dk/payara-micro

ENV USER gfish
USER $USER

COPY target/triton.war wars

EXPOSE 8080
