package io.awsgenie.terminator.configuration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Celal Emre CICEK
 * @version 12.04.2021
 */

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationReaderTest {
    @Test
    public void mergeConfigurations() {
        ParameterConfiguration parameterConfiguration = new ParameterConfiguration()
                .setRegion("us-west-1")
                .setResources("res1;res2;res3")
                .setService("sqs")
                .setDescription("ticket1")
                .setAssumeRoleArn("role1");
        FileConfiguration fileConfiguration = new FileConfiguration()
                .setRegion("us-west-2")
                .setService("sns");

        ConfigurationReader.mergeConfiguration(fileConfiguration, parameterConfiguration);

        assertThat(parameterConfiguration.getRegion(), is(equalTo("us-west-2")));
        assertThat(parameterConfiguration.getService(), is(equalTo("sns")));
        assertThat(parameterConfiguration.getResources(), is(equalTo("res1;res2;res3")));
        assertThat(parameterConfiguration.getDescription(), is(equalTo("ticket1")));
        assertThat(parameterConfiguration.getAssumeRoleArn(), is(equalTo("role1")));
    }
}