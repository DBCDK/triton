FROM docker-dbc.artifacts.dbccloud.dk

ENV USER gfish
USER $USER

COPY target/triton.war triton.json deployments/

EXPOSE 8080
