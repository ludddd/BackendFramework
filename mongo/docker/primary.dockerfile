FROM mongo:latest


COPY start.sh init_replset.sh /
RUN chmod +x /start.sh
RUN chmod +x /init_replset.sh

CMD /bin/bash /start.sh
