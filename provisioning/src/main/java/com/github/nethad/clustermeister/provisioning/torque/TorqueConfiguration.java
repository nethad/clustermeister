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

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas
 */
public class TorqueConfiguration {
    
    private static final String CONFIGURATION_FILE = System.getProperty("user.home") + "/.clustermeister/configuration.properties";
    
    public static final String TORQUE_SSH_USER = "torque.ssh_user";
    public static final String TORQUE_SSH_PRIVATEKEY = "torque.ssh_privatekey";
    public static final String TORQUE_SSH_HOST = "torque.ssh_host";
    public static final String TORQUE_SSH_PORT = "torque.ssh_port";
    public static final String TORQUE_EMAIL_NOTIFY = "torque.email_notify";
    public static final String TORQUE_QUEUE_NAME = "torque.queue_name";
    public static final String DEFAULT_TORQUE_QUEUE_NAME = "superfast";
    
    private static final Logger logger = LoggerFactory.getLogger(TorqueConfiguration.class);
    
//    torque.ssh_user = tritter
//torque.ssh_privatekey = /home/thomas/.ssh/id_dsa_kraken
//torque.ssh_host = kraken.ifi.uzh.ch
//torque.ssh_port = 22
//torque.email_notify = thomas.ritter@uzh.ch
    private String sshUser;
    private String privateKeyPath;
    private String sshHost;
    private int sshPort;
    private String emailNotify;
    private final String queueName;

    public TorqueConfiguration(String sshUser, String privateKeyPath, String sshHost, int sshPort, String emailNotify, String queueName) {
        this.sshUser = sshUser;
        this.privateKeyPath = privateKeyPath;
        this.sshHost = sshHost;
        this.sshPort = sshPort;
        this.emailNotify = emailNotify;
        this.queueName = queueName;
    }
    
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
        return message + ", you could do so in "+CONFIGURATION_FILE +" with "+configOption + ". "
                + "Using default value \""+defaultValue+"\".";
    }
    
//    public void printWarningsIfMissing(String sshUser, String privateKeyPath, String sshHost, int sshPort, String emailNotify) {
//        checkString(sshUser, "You didn't specify a ssh user", TORQUE_SSH_USER);
//        checkString(privateKeyPath, "You didn't specify to path to your private key", TORQUE_SSH_PRIVATEKEY);
//        checkString(sshHost, "You didn't specify the ssh host", TORQUE_SSH_HOST);
//        checkInt(sshPort, "You didn't specify an ssh port", TORQUE_SSH_PORT);
//        checkString(emailNotify, "You didn't specify your email address", TORQUE_EMAIL_NOTIFY);
//    }
//    
//    private void checkString(String toCheck, String loggerMessage, String configOption) {
//        if (isEmptyStringOrNull(toCheck)) {
//            logger.warn(loggerMessage(loggerMessage, configOption));
//        }
//    }
//    
//    private void checkInt(int toCheck, String loggerMessage, String configOption) {
//        if (toCheck == -1) {
//            logger.warn(loggerMessage(loggerMessage, configOption));
//        }
//    }
//    
//    private boolean isEmptyStringOrNull(String string) {
//        return string == null || string.isEmpty();
//    }
//    
//    private String loggerMessage(String message, String configOption) {
//        return message + ", you could do so in "+CONFIGURATION_FILE +" with "+configOption + ".";
//    }

    public String getSshUser() {
        return sshUser;
    }

    public String getPrivateKeyPath() {
        return privateKeyPath;
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
