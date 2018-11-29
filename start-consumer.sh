#!/bin/bash

. config.sh

java -jar kafka-consumer/target/kafka-consumer-0.0.1-SNAPSHOT-jar-with-dependencies.jar ${BOOTSTREP_SERVER} ${DST_TOPIC}
