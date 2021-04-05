#!/usr/bin/env bash

case "$1" in
  '-b')
    mvn clean install -T2C
    shift
esac

java -jar target/aws-resource-terminator-fat-1.0-SNAPSHOT.jar $@
