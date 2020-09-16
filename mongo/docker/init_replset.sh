#!/bin/bash
until mongo mongo-0.mongo.default.svc.cluster.local --eval "print(\"waited for connection with mongo0\")"
  do
    echo 'waiting...'
    sleep 1
  done
until mongo mongo-1.mongo.default.svc.cluster.local --eval "print(\"waited for connection with mongo1\")"
  do
    echo 'waiting...'
    sleep 1
  done
until mongo mongo-2.mongo.default.svc.cluster.local --eval "print(\"waited for connection with mongo2\")"
  do
    echo 'waiting...'
    sleep 1
  done
mongo --eval "rs.initiate({_id:\"rs0\",members:[{_id:0,host:\"mongo-0.mongo.default.svc.cluster.local\",priority:1.0},{_id:1,host:\"mongo-1.mongo.default.svc.cluster.local\",priority:1.0},{_id:2,host:\"mongo-2.mongo.default.svc.cluster.local\",priority:1.0}]})"
echo 'done'