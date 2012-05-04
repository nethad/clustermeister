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
package com.github.nethad.clustermeister.provisioning.cli;

import com.github.nethad.clustermeister.provisioning.CommandRegistry;
import com.github.nethad.clustermeister.api.impl.FileConfiguration;
import com.github.nethad.clustermeister.api.impl.YamlConfiguration;
import com.github.nethad.clustermeister.provisioning.CommandLineEvaluation;
import com.github.nethad.clustermeister.provisioning.CommandLineHandle;
import com.github.nethad.clustermeister.provisioning.CommandRegistry;
import com.github.nethad.clustermeister.provisioning.ec2.AmazonNodeManager;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFConfiguratedComponentFactory;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFLocalDriver;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFManagementByJobsClient;
import com.github.nethad.clustermeister.provisioning.rmi.RmiInfrastructure;
import com.github.nethad.clustermeister.provisioning.torque.TorqueNodeManager;
import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.StringTokenizer;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas
 */
public class Provisioning {
    private CommandLineEvaluation commandLineEvaluation;
    
    private String configFilePath;
//    private String driverHost;
    private Provider provider;
    private Configuration configuration;
    private RmiInfrastructure rmiInfrastructure;
    private Logger logger = LoggerFactory.getLogger(Provisioning.class);
    private JPPFLocalDriver jppfLocalDriver;
    private CommandLineHandle commandLineHandle;

    public Provisioning(String configFilePath, Provider provider, CommandRegistry commandRegistry) {
        this.configFilePath = configFilePath;
        this.provider = provider;
        rmiInfrastructure = new RmiInfrastructure();
        rmiInfrastructure.initialize();
        commandLineHandle = new CommandLineHandle(commandRegistry);
    }
    
    public void execute() {
        readConfigFile();
        switch(provider) {
            case AMAZON:
                startAmazon();
                break;
            case TORQUE:
                startTorque();
                break;
            case TEST:
                startTestSetup();
                break;
            default:
                throw new RuntimeException("Unknown provider");
        }
    }
    
    public void commandShutdown(StringTokenizer tokenizer) {
        commandLineEvaluation.shutdown(tokenizer);
        shutdownDriver();
    }   
    
//    public String helpText(String command) {
//        return commandLineEvaluation.helpText(command);
//    }
    
//    public void commandAddnodes(StringTokenizer tokenizer) {
//        commandLineEvaluation.addNodes(tokenizer, driverHost);
//    }
    
//    public void commandHelp(StringTokenizer tokenizer) {
//        commandLineEvaluation.help(tokenizer);
//    }
    
    public void commandUnknownFallback(String command, StringTokenizer tokenizer) {
        commandLineEvaluation.handleCommand(command, tokenizer);
    }
    
    public void commandState(StringTokenizer tokenizer) {
        commandLineEvaluation.state(tokenizer);
    }
    
    protected Provider getProvider() {
        return provider;
    }
    
    private void readConfigFile() {
        if (configFilePath == null || !(new File(configFilePath).exists())) {
            logger.warn("Configuration file \""+configFilePath+"\" does not exist.");
        } else {
            try {
                configuration = new YamlConfiguration(configFilePath);
            } catch (ConfigurationException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private void shutdownDriver() {
        jppfLocalDriver.shutdown();
    }

    private void startAmazon() {
        commandLineEvaluation = AmazonNodeManager.commandLineEvaluation(configuration, commandLineHandle, rmiInfrastructure);
        jppfLocalDriver = new JPPFLocalDriver();
        jppfLocalDriver.execute();
    }

    private void startTorque() {
        jppfLocalDriver = new JPPFLocalDriver();
        commandLineEvaluation = TorqueNodeManager.commandLineEvaluation(configuration, commandLineHandle, jppfLocalDriver, rmiInfrastructure.getRmiServerForApiObject());
        jppfLocalDriver.execute();
    }
    
    private void startTestSetup() {
        commandLineEvaluation = new CommandLineEvaluation() {
            @Override
            public void state(StringTokenizer tokenizer) {} // do nothing
            @Override
            public void shutdown(StringTokenizer tokenizer) {} // do nothing
            @Override
            public void handleCommand(String command, StringTokenizer tokenizer) {} // do nothing
            @Override
            public Object getNodeManager() { return null; }
            @Override
            public CommandLineHandle getCommandLineHandle() { return null; }
        };
        jppfLocalDriver = new JPPFLocalDriver();
        jppfLocalDriver.execute();
        jppfLocalDriver.update(null, "127.0.0.1");
    }

    @VisibleForTesting
    public RmiInfrastructure getRmiInfrastructure() {
        return rmiInfrastructure;
    }

    @VisibleForTesting
    void setCommandLineEvaluation(CommandLineEvaluation evaluation) {
        this.commandLineEvaluation = evaluation;
    }

    
}
