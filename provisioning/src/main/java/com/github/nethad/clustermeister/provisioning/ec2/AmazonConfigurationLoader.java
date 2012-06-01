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
import com.github.nethad.clustermeister.api.LogLevel;
import com.github.nethad.clustermeister.api.impl.AmazonConfiguredKeyPairCredentials;
import com.github.nethad.clustermeister.api.impl.ConfigurationUtil;
import com.github.nethad.clustermeister.api.impl.KeyPairCredentials;
import static com.google.common.base.Preconditions.*;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import org.apache.commons.configuration.Configuration;

/**
 * A utility to help parse configuration values for the AWS EC2 (Amazon) 
 * Provisioning Provider.
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
     * Shutdown state configuration property.
     */
    public static final String SHUTDOWN_STATE = "shutdown_state";
    
    /**
     * Group configuration property.
     */
    public static final String GROUP = "group";
    
    /**
     * Spot price configuration property.
     */
    public static final String SPOT_PRICE = "spot_price";
    
    /**
     * Spot request type configuration property.
     */
    public static final String SPOT_REQUEST_TYPE = "spot_request_type";
    
    /**
     * Spot request valid from configuration property.
     */
    public static final String SPOT_REQUEST_VALID_FROM = "spot_request_valid_from";
    
    /**
     * Spot request valid to configuration property.
     */
    public static final String SPOT_REQUEST_VALID_TO = "spot_request_valid_to";
    
    /**
     * Placement group configuration property.
     */
    public static final String PLACEMENT_GROUP = "placement_group";
    
    /**
     * JVM options configuration property.
     */
    public static final String NODE_JVM_OPTIONS = "jvm_options.node";
    
    /**
     * Node log level configuration property.
     */
    public static final String NODE_LOG_LEVEL = "logging.node.level";
    
    /**
     * Node remote logging configuration property.
     */
    public static final String NODE_LOG_REMOTE = "logging.node.remote";
    
    /**
     * Node remote logging configuration property.
     */
    public static final String NODE_LOG_REMOTE_PORT = "logging.node.remote_port";
    
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
    
    //TODO: tests for these
    
    /**
     * Returns the configured node JVM options
     * 
     * @return the JVM options.
     */
    public String getNodeJvmOptions() {
        return configuration.getString(NODE_JVM_OPTIONS, "-Xmx32m");
    }
    
    /**
     * Returns the configured node log level.
     * 
     * @return the SLF4J log level.
     */
    public LogLevel getNodeLogLevel() {
        String level = configuration.getString(NODE_LOG_LEVEL, "INFO");
        try {
            return LogLevel.valueOf(level.toUpperCase());
        } catch(IllegalArgumentException ex) {
            return LogLevel.INFO;
        }
    }
    
    /**
     * Returns whether to activate remote logging for nodes or not.
     */
    public Boolean getNodeRemoteLogging() {
        return configuration.getBoolean(NODE_LOG_REMOTE, Boolean.FALSE);
    }
    
    /**
     * Returns whether to activate remote logging for nodes or not.
     */
    public Integer getNodeRemoteLoggingPort() {
        //TODO: how to get remote default port centrally?
        return configuration.getInt(NODE_LOG_REMOTE_PORT, 52321);
    }
    
    /**
     * Returns a set of configured {@link Credentials}.
     * 
     * @return the configured credentials.
     */
    public Set<Credentials> getConfiguredCredentials() {
        List<Object> keypairList = configuration.getList(KEYPAIRS, Collections.EMPTY_LIST);
        Map<String, Map<String, String>> keypairSpecifications = 
                ConfigurationUtil.reduceObjectList(keypairList, 
                "Keypairs must be specified as a list of objects.");
        Set<Credentials> credentials = 
                Sets.newHashSetWithExpectedSize(keypairSpecifications.size());
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
                credentials.add(new KeyPairCredentials(
                        keyPairName, user, privateKey, publicKey));
            } else {
                credentials.add(new AmazonConfiguredKeyPairCredentials(
                        keyPairName, user, privateKey));
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
                    shutdownState(profileValues.get(SHUTDOWN_STATE)).
                    group(profileValues.get(GROUP)).
                    spotPrice(profileValues.get(SPOT_PRICE)).
                    spotRequestType(profileValues.get(SPOT_REQUEST_TYPE)).
                    spotRequestValidFrom(profileValues.get(SPOT_REQUEST_VALID_FROM)).
                    spotRequestValidTo(profileValues.get(SPOT_REQUEST_VALID_TO)).
                    placementGroup(profileValues.get(PLACEMENT_GROUP)).
                    build();
            profiles.put(profileName, profile);
        }
        
        return profiles;
    }
}
