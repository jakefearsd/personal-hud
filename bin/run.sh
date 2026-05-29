#!/bin/bash

# HUD Server Management Script

CMD=$1

case "$CMD" in
  start)
    echo "Building HUD project..."
    mvn clean install -DskipTests
    if [ $? -ne 0 ]; then
      echo "Build failed! Aborting."
      exit 1
    fi
    echo "Starting HUD containers..."
    docker compose up --build -d
    echo "HUD is running at http://localhost:8889"
    ;;
  stop)
    echo "Stopping HUD containers..."
    docker compose down
    ;;
  *)
    echo "Usage: $0 {start|stop}"
    exit 1
    ;;
esac
