#!/bin/bash
until mongo mongo-0.mongo --eval "print(\"waited for connection\")"
  do
    echo 'waiting...'
    sleep 1
  done
mongo --eval "rs.initiate({_id:\"rs0\",members:[{_id:0,host:\"mongo-0.mongo\",priority:1.0},{_id:1,host:\"mongo-1.mongo\",priority:1.0},{_id:2,host:\"mongo-2.mongo\",priority:1.0}]})"
echo 'done'