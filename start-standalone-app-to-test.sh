#!/bin/bash

. config.sh

if [ "$#" -ne 1 ]; then
  echo "Usage $0 [exactly-once]"
  echo "Example: $0 false"
  exit 1
fi

while true
do
  java -jar standalone-app-to-test/target/standalone-app-to-test-0.0.1-SNAPSHOT-jar-with-dependencies.jar ${BOOTSTREP_SERVER} ${SRC_TOPIC} ${DST_TOPIC} $1
done
