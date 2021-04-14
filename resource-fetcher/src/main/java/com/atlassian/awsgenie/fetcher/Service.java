package com.atlassian.awsgenie.fetcher;

public enum Service {
    CLOUDFRONT("cloudfront"),
    CLOUDWATCH("cloudwatch"),
    DYNAMODB("dynamodb"),
    IAM_ROLE("iam-role"),
    IAM_POLICY("iam-policy"),
    KINESIS("kinesis"),
    LAMBDA("lambda"),
    SNS("sns"),
    SQS("sqs");

    private String value;

    Service(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Service fromValue(String value) {
        for (Service s : Service.values()) {
            if (s.getValue().equals(value)) {
                return s;
            }
        }
        throw new RuntimeException("Service not found with value: " + value);
    }
}
