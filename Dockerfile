FROM docker.dbc.dk/payara5-micro:latest

ENV USER gfish
USER $USER

COPY target/triton.war triton.json deployments/

EXPOSE 8080
