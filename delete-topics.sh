#!/bin/bash

. config.sh

ssh ${SERVER} "kafka-topics --zookeeper localhost:2181 --delete --topic ${SRC_TOPIC}" > /dev/null
ssh ${SERVER} "kafka-topics --zookeeper localhost:2181 --delete --topic ${DST_TOPIC}" > /dev/null
