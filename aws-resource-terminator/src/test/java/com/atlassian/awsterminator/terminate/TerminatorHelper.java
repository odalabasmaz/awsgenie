package com.atlassian.awsterminator.terminate;

import credentials.AwsClientConfiguration;

public class TerminatorHelper {

    public static AwsClientConfiguration getRegion1Account1Configuration(){
        return new AwsClientConfiguration() {
            @Override
            public String getAssumeRoleArn() {
                return "account1";
            }

            @Override
            public String getRegion() {
                return "region1";
            }
        };
    }

    public static AwsClientConfiguration getRegion2Account2Configuration(){
        return new AwsClientConfiguration() {
            @Override
            public String getAssumeRoleArn() {
                return "account2";
            }

            @Override
            public String getRegion() {
                return "region2";
            }
        };
    }

}
