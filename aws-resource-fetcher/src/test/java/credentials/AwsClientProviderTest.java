package credentials;


import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@RunWith(MockitoJUnitRunner.class)
public class AwsClientProviderTest {


    @Before
    public void setup() {
    }

    @Test
    public void checkClientHasChangedIfConfigurationSame() {
        AwsClientProvider instance = AwsClientProvider.getInstance(new AwsClientConfiguration() {
            @Override
            public String getAssumeRoleArn() {
                return "assumeRoleArn1";
            }

            @Override
            public String getRegion() {
                return "assumeRoleRegion1";
            }
        });
        AmazonCloudWatch amazonCloudWatch = instance.getAmazonCloudWatch();
        AmazonCloudWatch amazonCloudWatch1 = instance.getAmazonCloudWatch();
        assertEquals(amazonCloudWatch1, amazonCloudWatch);
    }


    @Test
    public void checkClientHasChangedIfRegionIsDifferent() {
        AwsClientProvider instanceRegion1= AwsClientProvider.getInstance(new AwsClientConfiguration() {
            @Override
            public String getAssumeRoleArn() {
                return "assumeRoleArn1";
            }

            @Override
            public String getRegion() {
                return "assumeRoleRegion1";
            }
        });
        AwsClientProvider instanceRegion2= AwsClientProvider.getInstance(new AwsClientConfiguration() {
            @Override
            public String getAssumeRoleArn() {
                return "assumeRoleArn1";
            }

            @Override
            public String getRegion() {
                return "assumeRoleRegion2";
            }
        });
        AmazonCloudWatch amazonCloudWatchRegion1 = instanceRegion1.getAmazonCloudWatch();
        AmazonCloudWatch amazonCloudWatchRegion2 = instanceRegion2.getAmazonCloudWatch();
        assertNotEquals(amazonCloudWatchRegion1, amazonCloudWatchRegion2);
    }
}