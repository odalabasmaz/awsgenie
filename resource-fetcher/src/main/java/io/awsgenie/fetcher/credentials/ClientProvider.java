package io.awsgenie.fetcher.credentials;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClient;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;

public class ClientProvider {

    private AWSCredentialsProvider awsCredentialsProvider;
    private String region;
    private AWSLambda amazonLambda;
    private AmazonSNS amazonSNS;

    private static ClientProvider clientProvider;

    public static ClientProvider getInstance(AWSCredentialsProvider credentialsProvider, String region) {
        if (clientProvider == null) {
            clientProvider = new ClientProvider(credentialsProvider, region);
        }
        return clientProvider;
    }

    private ClientProvider(AWSCredentialsProvider awsCredentialsProvider, String region) {
        this.awsCredentialsProvider = awsCredentialsProvider;
        this.region = region;
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
}
