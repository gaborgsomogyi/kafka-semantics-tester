#!/bin/bash

. config.sh

while true
do
  sec_to_sleep=$((((RANDOM % 30)) + 60))
  echo "Sleeping ${sec_to_sleep} seconsds..."
  sleep ${sec_to_sleep}
  echo "Kill app..."
  pkill -f "java -jar standalone-app-to-test" -9
  echo "OK"
done
