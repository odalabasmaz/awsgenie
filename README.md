# AWSGenie
A comprehensive utility for managing & comparing your AWS resources.

## Resource Terminator
- Terminating resources in AWS can be really hard if you have lot's of them. AWSGenie Resource Terminator help you
with removing your resources safely.
- It can check resources that depends on other resources, shows you the last usage data for the resources, and if you confirm, deletes the resources and removes them from the other resources.
- AWSGenie Resource Terminator can be used standalone as a CLI tool, or can be added to your project as a library.

Basic Usage: `java -jar awsgenie-{VERSION}.jar --region {REGION} --service {AWS SERVICE} --resources {RESOURCE NAMES} --description {DESCRIPTION}`

For more detailed explanation about the parameters, please checkout the javadoc.