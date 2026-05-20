#!/usr/bin/env bash

if [ -z "$1" ]; then
  echo "Usage: ./start_chatting.sh <ClientName> [GroupId]"
  exit 1
fi

CLIENT_NAME="$1"
GROUP_ID="${2:-${CLIENT_NAME}_Group}"

export APPLICATION_MONNOM="$CLIENT_NAME"
export SPRING_KAFKA_CONSUMER_GROUP_ID="$GROUP_ID"

echo "Starting client:"
echo "  name    = $CLIENT_NAME"
echo "  groupId = $GROUP_ID"

java -jar "client-shell/target/shellClient-0.0.1.jar"