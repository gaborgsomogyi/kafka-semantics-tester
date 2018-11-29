#!/bin/bash

. config.sh

java -jar kafka-producer/target/kafka-producer-0.0.1-SNAPSHOT-jar-with-dependencies.jar ${BOOTSTREP_SERVER} ${SRC_TOPIC} 10 1000
