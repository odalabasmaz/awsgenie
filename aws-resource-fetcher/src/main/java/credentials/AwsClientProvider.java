package credentials;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEvents;
import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEventsClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClient;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClient;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.*;

public class AwsClientProvider {

    private static Map<String, AwsClientProvider> clientProviderMap = new HashMap<>();
    private static final Logger LOGGER = LogManager.getLogger(AwsClientProvider.class);
    private static final String STS_SESSION_NAME_PREFIX = "aws_resource_terminator_";

    private AWSCredentialsProvider awsCredentialsProvider;
    private String region;

    private AmazonCloudWatchEvents amazonCloudWatchEvents;
    private AmazonCloudWatch amazonCloudWatch;
    private AmazonDynamoDB amazonDynamoDB;
    private AWSLambda amazonLambda;
    private AmazonSNS amazonSNS;
    private AmazonSQS amazonSqs;
    private AmazonIdentityManagement amazonIAM;
    private AmazonKinesis amazonKinesis;
    private AWSSecurityTokenService amazonSts;

    public static AwsClientProvider getInstance(AwsClientConfiguration configuration) {
        String region = configuration.getRegion();
        if (clientProviderMap.get(region) == null) {
            clientProviderMap.put(region, new AwsClientProvider(createAwsCredentialsProvider(configuration.getAssumeRoleArn(), region), region));
        }
        return clientProviderMap.get(region);
    }

    private AwsClientProvider(AWSCredentialsProvider awsCredentialsProvider, String region) {
        this.awsCredentialsProvider = awsCredentialsProvider;
        this.region = region;
    }


    private static AWSCredentialsProvider createAwsCredentialsProvider(String assumeRoleArn, String region) {
        AWSSecurityTokenService stsClient = AWSSecurityTokenServiceClient.builder()
                .withRegion(region)
                .build();
        if (StringUtils.isNotBlank(assumeRoleArn)) {
            STSAssumeRoleSessionCredentialsProvider credentialsProvider = new STSAssumeRoleSessionCredentialsProvider
                    .Builder(assumeRoleArn, getSTSSessionName())
                    .withStsClient(stsClient)
                    .build();
            LOGGER.info("Using assumed role: " + assumeRoleArn);
            return credentialsProvider;
        }
        return null;
    }

    public AWSLambda getAmazonLambda() {
        if (amazonLambda == null) {
            amazonLambda = AWSLambdaClient
                    .builder()
                    .withRegion(region)
                    .withCredentials(this.awsCredentialsProvider)
                    .build();
        }
        return amazonLambda;
    }

    public AmazonIdentityManagement getAmazonIAM() {
        if (amazonIAM == null) {
            amazonIAM = AmazonIdentityManagementClient
                    .builder()
                    .withRegion(region)
                    .withCredentials(this.awsCredentialsProvider)
                    .build();
        }
        return amazonIAM;
    }

    public AWSSecurityTokenService getAmazonSts(){
        if (amazonSts == null){
            amazonSts = AWSSecurityTokenServiceClientBuilder.standard().build();
        }
        return amazonSts;
    }


    public AmazonSNS getAmazonSNS() {
        if (amazonSNS == null) {
            amazonSNS = AmazonSNSClient
                    .builder()
                    .withRegion(region)
                    .withCredentials(this.awsCredentialsProvider)
                    .build();
        }
        return amazonSNS;
    }

    public AmazonKinesis getAmazonKinesis() {
        if (amazonKinesis == null) {
            amazonKinesis = AmazonKinesisClient
                    .builder()
                    .withRegion(region)
                    .withCredentials(this.awsCredentialsProvider)
                    .build();
        }
        return amazonKinesis;
    }

    public AmazonSQS getAmazonSQS() {
        if (amazonSqs == null) {
            amazonSqs = AmazonSQSClient
                    .builder()
                    .withRegion(region)
                    .withCredentials(this.awsCredentialsProvider)
                    .build();
        }
        return amazonSqs;
    }

    public AmazonDynamoDB getAmazonDynamoDB() {
        if (amazonDynamoDB == null) {
            amazonDynamoDB = AmazonDynamoDBClient
                    .builder()
                    .withRegion(region)
                    .withCredentials(this.awsCredentialsProvider)
                    .build();
        }
        return amazonDynamoDB;
    }

    public AmazonCloudWatch getAmazonCloudWatch() {
        if (amazonCloudWatch == null) {
            amazonCloudWatch = AmazonCloudWatchClient
                    .builder()
                    .withRegion(region)
                    .withCredentials(this.awsCredentialsProvider)
                    .build();
        }
        return amazonCloudWatch;
    }

    public AmazonCloudWatchEvents getAmazonCloudWatchEvents() {
        if (amazonCloudWatchEvents == null) {
            amazonCloudWatchEvents = AmazonCloudWatchEventsClient
                    .builder()
                    .withRegion(region)
                    .withCredentials(this.awsCredentialsProvider)
                    .build();
        }
        return amazonCloudWatchEvents;
    }

    private static String getSTSSessionName() {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("HH_mm", Locale.ENGLISH);
        dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        String datePrefix = dateFormatter.format(new Date());

        return STS_SESSION_NAME_PREFIX + datePrefix;
    }
}
