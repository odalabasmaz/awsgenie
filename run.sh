#!/usr/bin/env bash

while getopts ":b:u" opt; do
  case "$opt" in
    b)
      mvn clean install -T2C
      shift
      ;;
    u)
      unset AWS_ACCESS_KEY_ID
      unset AWS_SECRET_ACCESS_KEY
      unset AWS_SECURITY_TOKEN
      unset AWS_SESSION_TOKEN
      unset AWS_EXPIRATION
      unset AWS_DELEGATION_TOKEN
      unset AWS_EXPIRATION_TIMESTAMP
      unset AWS_ACCOUNT_ID
      unset AWS_LASTUPDATED
      unset AWS_ROLE_NAME
      shift
      ;;
  esac
done

java -jar target/aws-resource-terminator-fat-1.0-SNAPSHOT.jar $@
