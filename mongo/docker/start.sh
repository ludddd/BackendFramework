#!/bin/bash
/bin/bash -c "./init_replset.sh > var/log/init_replset.log 2>&1 &"
mongod --replSet rs0 --bind_ip_all