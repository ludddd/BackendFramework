FROM mongo:latest


COPY start.sh .
COPY init_replset.sh .
RUN chmod +x ./start.sh
RUN chmod +x ./init_replset.sh

CMD ./start.sh
