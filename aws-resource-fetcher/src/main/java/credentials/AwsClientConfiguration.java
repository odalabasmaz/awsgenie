package credentials;

public interface AwsClientConfiguration {

    String getAssumeRoleArn();

    String getRegion();
}
