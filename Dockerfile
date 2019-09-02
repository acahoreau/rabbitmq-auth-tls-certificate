FROM rabbitmq:3.7-management

COPY ./conf /etc/rabbitmq
COPY ./definitions.json /opt/definitions.json
COPY ./server /opt/certs/server
COPY ./testca /opt/certs/ca

