/*
 * Copyright 2012 The Clustermeister Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.nethad.clustermeister.provisioning.ec2;

import com.github.nethad.clustermeister.api.Credentials;
import com.github.nethad.clustermeister.api.impl.AmazonConfiguredKeyPairCredentials;
import com.github.nethad.clustermeister.api.impl.KeyPairCredentials;
import com.github.nethad.clustermeister.api.impl.YamlConfiguration;
import com.google.common.base.Charsets;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Map;
import org.apache.commons.configuration.ConfigurationException;
import org.hamcrest.Matchers;
import static org.hamcrest.Matchers.*;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Tests for AmazonConfigurationLoader.
 *
 * @author daniel
 */
public class AmazonConfigurationLoaderTest {
    private static final String SECRET_KEY = "1a2b3c4e5f6g7h8i9j0k";
    private static final String ACCESS_KEY_ID = "ABCDEFGHIJKLMNOPQRSTUVW";
    private static final String KEYPAIR1 = "keypair1";
    private static final String KEYPAIR2 = "keypair2";
    private static final String PROFILE1 = "profile1";
    private static final String PROFILE2 = "profile2";
    private static final String AMI_ID1 = "a-11234";
    private static final String AMI_ID2 = "a-98765";
    private static final String LOCATION1 = "eu-west-1";
    private static final String LOCATION2 = "us-east-1a";
    private static final String USER1 = "user1";
    private static final String USER2 = "user2";
    private ByteArrayInputStream configBytes;
    private ByteArrayInputStream badConfigBytes;

    private AmazonConfigurationLoader configLoader;
    private String privateKeyPath;
    private String publicKeyPath;
    
    public AmazonConfigurationLoaderTest() {
        setConfig();
    }
    
    private void setConfig() {
        privateKeyPath = getClass().getResource("key_rsa").getPath();
        publicKeyPath = getClass().getResource("key_rsa.pub").getPath();
        StringBuilder config = new StringBuilder("amazon:").append("\n");
        config.append("  access_key_id: ").append(ACCESS_KEY_ID).append("\n");
        config.append("  secret_key: ").append(SECRET_KEY).append("\n");
        config.append("  keypairs:").append("\n");
        config.append("    - ").append(KEYPAIR1).append(":").append("\n");
        config.append("        user: ").append(USER1).append("\n");
        config.append("        private_key: ").append(privateKeyPath).append("\n");
        config.append("    - ").append(KEYPAIR2).append(":").append("\n");
        config.append("        user: ").append(USER2).append("\n");
        config.append("        private_key: ").append(privateKeyPath).append("\n");
        config.append("        public_key: ").append(publicKeyPath).append("\n");
        config.append("  profiles:").append("\n");
        config.append("    - ").append(PROFILE1).append(":").append("\n");
        config.append("        ami_id: ").append(AMI_ID1).append("\n");
        config.append("        location: ").append(LOCATION1).append("\n");
        config.append("    - ").append(PROFILE2).append(":").append("\n");
        config.append("        ami_id: ").append(AMI_ID2).append("\n");
        config.append("        location: ").append(LOCATION2).append("\n");
        configBytes = new ByteArrayInputStream(config.toString().getBytes(Charsets.UTF_8));
        
        config = new StringBuilder("amazon:").append("\n");
        config.append("  access_key_id: ").append("\n");
        config.append("  keypairs:").append("\n");
        config.append("    - ").append(KEYPAIR1).append(":").append("\n");
        config.append("        ami_id: ").append(AMI_ID1).append("\n");
        config.append("  profiles:").append("\n");
        config.append("    - ").append(PROFILE1).append(":").append("\n");
        config.append("        ami_id: ").append(AMI_ID1).append("\n");
        badConfigBytes = new ByteArrayInputStream(config.toString().getBytes(Charsets.UTF_8));
    }

    
    public void goodConfigSetup() throws ConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.load(new InputStreamReader(configBytes, Charsets.UTF_8));
        configLoader = new AmazonConfigurationLoader(config);
    }
    
    public void badConfigSetup() throws ConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.load(new InputStreamReader(badConfigBytes, Charsets.UTF_8));
        configLoader = new AmazonConfigurationLoader(config);
    }
    
    @After
    public void tearDown() {
        configLoader = null;
    }

    /**
     * Test of getAccessKeyId method, of class AmazonConfigurationLoader.
     */
    @Test
    public void testGetAccessKeyId() throws ConfigurationException {
        goodConfigSetup();
        assertThat(configLoader.getAccessKeyId(), is(equalTo(ACCESS_KEY_ID)));
    }
    
    @Test(expected=NullPointerException.class)
    public void testGetBadAccessKeyId() throws ConfigurationException {
        badConfigSetup();
        configLoader.getAccessKeyId();
    }

    /**
     * Test of getSecretKey method, of class AmazonConfigurationLoader.
     */
    @Test
    public void testGetSecretKey() throws ConfigurationException {
        goodConfigSetup();
        assertThat(configLoader.getSecretKey(), is(equalTo(SECRET_KEY)));
    }
    
    @Test(expected=NullPointerException.class)
    public void testGetBadSecretKey() throws ConfigurationException {
        badConfigSetup();
        configLoader.getSecretKey();
    }

    /**
     * Test of getConfiguredCredentials method, of class AmazonConfigurationLoader.
     */
    @Test
    public void testGetConfiguredCredentials() throws ConfigurationException {
        goodConfigSetup();
        Map<String, Credentials> result = configLoader.getConfiguredCredentials();
        Credentials c1 = new AmazonConfiguredKeyPairCredentials(USER1, new File(privateKeyPath), KEYPAIR1);
        Credentials c2 = new KeyPairCredentials(USER2, new File(privateKeyPath), new File(publicKeyPath));
        assertThat(result, allOf(
                hasEntry(KEYPAIR1, c1),
                hasEntry(KEYPAIR2, c2)
        ));
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testGetBadConfiguredCredentials() throws ConfigurationException {
        badConfigSetup();
        configLoader.getConfiguredCredentials();
    }
    
    @Test
    public void testGetConfiguredProfiles() throws ConfigurationException {
        goodConfigSetup();
        Map<String, AWSInstanceProfile> result = configLoader.getConfiguredProfiles();
        assertThat(result, allOf(
                Matchers.<String, AWSInstanceProfile>hasKey(PROFILE1),
                Matchers.<String, AWSInstanceProfile>hasKey(PROFILE2)
        ));
        assertThat(result.get(PROFILE1).getAmiId().get(), is(equalTo(AMI_ID1)));
        assertThat(result.get(PROFILE1).getLocation(), is(equalTo(LOCATION1)));
        assertThat(result.get(PROFILE2).getAmiId().get(), is(equalTo(AMI_ID2)));
        assertThat(result.get(PROFILE2).getLocation(), is(equalTo(LOCATION2)));
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testGetBadConfiguredProfiles() throws ConfigurationException {
        badConfigSetup();
        configLoader.getConfiguredProfiles();
    }
}
