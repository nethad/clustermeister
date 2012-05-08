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
package com.github.nethad.clustermeister.api.impl;

import com.github.nethad.clustermeister.api.Credentials;
import com.google.common.base.Charsets;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Map;
import org.apache.commons.configuration.ConfigurationException;
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
        configBytes = new ByteArrayInputStream(config.toString().getBytes(Charsets.UTF_8));
        
        config = new StringBuilder("amazon:").append("\n");
        config.append("  access_key_id: ").append("\n");
        config.append("  keypairs:").append("\n");
        config.append("    - ").append(KEYPAIR1).append(":").append("\n");
        config.append("        private_key: ").append(privateKeyPath).append("\n");
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
        assertThat(result.get(KEYPAIR1).getUser(), is(equalTo(USER1)));
        assertThat(result.get(KEYPAIR1), is(AmazonConfiguredKeyPairCredentials.class));
        assertThat(result.get(KEYPAIR2).getUser(), is(equalTo(USER2)));
        assertThat(result.get(KEYPAIR2), is(KeyPairCredentials.class));
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testGetBadConfiguredCredentials() throws ConfigurationException {
        badConfigSetup();
        configLoader.getConfiguredCredentials();
    }
}
