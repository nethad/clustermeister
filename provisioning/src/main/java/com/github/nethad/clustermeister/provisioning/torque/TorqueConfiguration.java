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
package com.github.nethad.clustermeister.provisioning.torque;

import com.github.nethad.clustermeister.api.Loggers;
import com.github.nethad.clustermeister.api.impl.FileConfiguration;
import com.github.nethad.clustermeister.api.impl.KeyPairCredentials;
import java.io.File;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas
 */
public class TorqueConfiguration {
    
    public static final String TORQUE_SSH_USER = "torque.ssh_user";
    public static final String TORQUE_SSH_PRIVATEKEY = "torque.ssh_privatekey";
    public static final String TORQUE_SSH_HOST = "torque.ssh_host";
    public static final String TORQUE_SSH_PORT = "torque.ssh_port";
    public static final String TORQUE_EMAIL_NOTIFY = "torque.email_notify";
    public static final String TORQUE_QUEUE_NAME = "torque.queue_name";
    public static final String DEFAULT_TORQUE_QUEUE_NAME = "superfast";
    
    private static final Logger logger = LoggerFactory.getLogger(Loggers.PROVISIONING);
    
    private KeyPairCredentials sshCredentials;
    private String sshHost;
    private int sshPort;
    private String emailNotify;
    private final String queueName;

    public TorqueConfiguration(String sshUser, String privateKeyPath, String sshHost, int sshPort, String emailNotify, String queueName) {
        this.sshCredentials = new KeyPairCredentials("Torque SSH Credentials", 
                sshUser, new File(privateKeyPath));
        this.sshHost = sshHost;
        this.sshPort = sshPort;
        this.emailNotify = emailNotify;
        this.queueName = queueName;
    }
    
    /**
     * Extracts Torque-specific configuration from a {@link Configuration} source.
     * @param configuration to read configuration values from
     * @return a {@link TorqueConfiguration} object.
     * @throws ConfigurationValueMissingException 
     */
    public static TorqueConfiguration buildFromConfig(Configuration configuration) throws ConfigurationValueMissingException {
        String sshHost = 
                checkString(configuration, TORQUE_SSH_HOST, "", "You didn't specify an ssh host");
        if (sshHost.isEmpty()) {
            throw new ConfigurationValueMissingException("You have to specify an ssh host.");
        }
        String sshUser = 
                checkString(configuration, TORQUE_SSH_USER, System.getProperty("user.name"), "You didn't specify a ssh user");
        String defaultPrivateKeyPath = System.getProperty("user.home") + "/.ssh/id_dsa";
        String privateKeyPath = 
                checkString(configuration, TORQUE_SSH_PRIVATEKEY, defaultPrivateKeyPath, "You didn't specify a private key path");
        String emailNotify = 
                checkString(configuration, TORQUE_EMAIL_NOTIFY, "", "You didn't specify an email address for notifications");
        int sshPort = checkInt(configuration, TORQUE_SSH_PORT, 22, "You didn't specify an ssh port");
        String queueName = checkString(configuration, TORQUE_QUEUE_NAME, DEFAULT_TORQUE_QUEUE_NAME, "You didn't specify a PBS/Torque queue name");
        return new TorqueConfiguration(sshUser, privateKeyPath, sshHost, sshPort, emailNotify, queueName);
    }
    
    private static String checkString(Configuration configuration, String configOption, String defaultValue, String logMessage) {
        String value = configuration.getString(configOption, "");
        if (value.isEmpty()) {
            logger.warn(loggerMessage(logMessage, configOption, defaultValue));
            return defaultValue;
        }
        return value;
    }
    
    private static int checkInt(Configuration configuration, String configOption, int defaultValue, String logMessage) {
        int value = configuration.getInt(configOption, -1);
        if (value == -1) {
            logger.warn(loggerMessage(logMessage, configOption, String.valueOf(defaultValue)));
            return defaultValue;
        }
        return value;
    }
    
    private static String loggerMessage(String message, String configOption, String defaultValue) {
        return message + ", you could do so in "+ FileConfiguration.DEFAULT_CONFIG_FILE +" with "+configOption + ". "
                + "Using default value \""+defaultValue+"\".";
    }

    public KeyPairCredentials getSshCredentials() {
        return sshCredentials;
    }

    public String getSshHost() {
        return sshHost;
    }

    public int getSshPort() {
        return sshPort;
    }

    public String getEmailNotify() {
        return emailNotify;
    }
    
    public String getQueueName() {
        return queueName;
    }
    
}
