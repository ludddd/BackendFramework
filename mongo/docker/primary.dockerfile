FROM mongo

CMD ["mongod", "--replSet", "rs0"]