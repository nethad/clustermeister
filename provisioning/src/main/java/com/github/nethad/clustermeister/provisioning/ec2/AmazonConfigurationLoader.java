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
import com.github.nethad.clustermeister.api.impl.ConfigurationUtil;
import com.github.nethad.clustermeister.api.impl.KeyPairCredentials;
import static com.google.common.base.Preconditions.*;
import com.google.common.collect.Maps;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import org.apache.commons.configuration.Configuration;

/**
 * A utility to help parse Amazon specific configuration values.
 *
 * @author daniel
 */
public class AmazonConfigurationLoader {
    /**
     * AWS Access key ID configuration property.
     */
    public static final String ACCESS_KEY_ID = "amazon.access_key_id";
    
    /**
     * AWS secret key configuration property.
     */
    public static final String SECRET_KEY = "amazon.secret_key";
    
    /**
     * Amazon key pair configurations property.
     */
    public static final String KEYPAIRS = "amazon.keypairs";
    
    /**
     * Amazon profile configurations property.
     */
    public static final String PROFILES = "amazon.profiles";
    
    /**
     * Private key file path configuration property.
     */
    public static final String PRIVATE_KEY = "private_key";
    
    /**
     * Public key file path configuration property.
     */
    public static final String PUBLIC_KEY = "public_key";
    
    /**
     * Username configuration property.
     */
    public static final String USER = "user";
    
    /**
     * AMI (Amazon Image) ID configuration property.
     */
    public static final String AMI_ID = "ami_id";
    
    /**
     * AWS EC2 Region configuration property.
     */
    public static final String REGION = "region";
    
    /**
     * AWS EC2 Availability Zone configuration property.
     */
    public static final String ZONE = "zone";
    
    /**
     * AWS EC2 hardware type configuration property.
     */
    public static final String TYPE = "type";
    
    /**
     * Keypair configuration property.
     */
    public static final String KEYPAIR = "keypair";
    
    /**
     * The configuration.
     */
    final Configuration configuration;

    /**
     * Create a new Configuration Loader with a specific configuration.
     * 
     * @param configuration the configuration.
     */
    public AmazonConfigurationLoader(Configuration configuration) {
        this.configuration = configuration;
    }
    
    /**
     * Returns the configured AWS Access Key.
     * 
     * @return the access key.
     */
    public String getAccessKeyId() {
        return checkNotNull(configuration.getString(ACCESS_KEY_ID, null), 
                "No AWS access key ID configured.").trim();
    }
    
    /**
     * Returns the configured Secret Key.
     * 
     * @return the secret key.
     */
    public String getSecretKey() {
        return checkNotNull(configuration.getString(SECRET_KEY, null), 
                "No AWS secret key configured.").trim();
    }
    
    /**
     * Returns a map containing a mapping of configured credentials names to 
     * {@link Credentials} instances..
     * 
     * @return the configured credentials.
     */
    public Map<String, Credentials> getConfiguredCredentials() {
        List<Object> keypairList = configuration.getList(KEYPAIRS, Collections.EMPTY_LIST);
        Map<String, Map<String, String>> keypairSpecifications = 
                ConfigurationUtil.reduceObjectList(keypairList, 
                "Keypairs must be specified as a list of objects.");
        Map<String, Credentials> credentials = 
                Maps.newHashMapWithExpectedSize(keypairSpecifications.size());
        for (Map.Entry<String, Map<String, String>> entry : keypairSpecifications.entrySet()) {
            String keyPairName = entry.getKey();
            Map<String, String> keyPairValues = entry.getValue();
            String user = ConfigurationUtil.getCheckedConfigValue(
                    USER, keyPairValues, "keypair", keyPairName);
            String privateKeyPath = ConfigurationUtil.getCheckedConfigValue(
                    PRIVATE_KEY, keyPairValues, "keypair", keyPairName);
            File privateKey = ConfigurationUtil.getCheckedFile(
                    privateKeyPath, PRIVATE_KEY, "keypair", keyPairName);
            
            String publicKeyPath = keyPairValues.get(PUBLIC_KEY);
            if(publicKeyPath != null) {
                File publicKey = ConfigurationUtil.getCheckedFile(
                        publicKeyPath, PUBLIC_KEY, "keypair", keyPairName);
                credentials.put(keyPairName,
                        new KeyPairCredentials(user, privateKey, publicKey));
            } else {
                credentials.put(keyPairName, 
                        new AmazonConfiguredKeyPairCredentials(user, privateKey, 
                        keyPairName));
            }
        }
        
        return credentials;
    }
    
    public Map<String, AWSInstanceProfile> getConfiguredProfiles() {
        List<Object> profilesList = configuration.getList(PROFILES, Collections.EMPTY_LIST);
        Map<String, Map<String, String>> profileSpecifications = 
                ConfigurationUtil.reduceObjectList(profilesList, 
                "Profiles must be specified as a list of objects.");
        SortedMap<String, AWSInstanceProfile> profiles = Maps.newTreeMap();
        for (Map.Entry<String, Map<String, String>> entry : profileSpecifications.entrySet()) {
            String profileName = entry.getKey();
            Map<String, String> profileValues = entry.getValue();
            AWSInstanceProfile profile = AWSInstanceProfile.newBuilder().
                    profileName(profileName).
                    region(profileValues.get(REGION)).
                    zone(profileValues.get(ZONE)).
                    type(profileValues.get(TYPE)).
                    amiId(profileValues.get(AMI_ID)).
                    keypairName(profileValues.get(KEYPAIR)).
                    build();
            profiles.put(profileName, profile);
        }
        
        return profiles;
    }
}
