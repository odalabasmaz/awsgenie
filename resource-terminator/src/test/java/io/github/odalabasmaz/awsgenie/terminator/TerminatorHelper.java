package io.github.odalabasmaz.awsgenie.terminator;

import io.github.odalabasmaz.awsgenie.fetcher.credentials.AWSClientConfiguration;

public class TerminatorHelper {

    public static AWSClientConfiguration getRegion1Account1Configuration() {
        return new AWSClientConfiguration() {
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

    public static AWSClientConfiguration getRegion2Account2Configuration() {
        return new AWSClientConfiguration() {
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
