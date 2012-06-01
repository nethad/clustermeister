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

import com.github.nethad.clustermeister.api.Loggers;
import com.github.nethad.clustermeister.api.impl.YamlConfiguration;
import com.github.nethad.clustermeister.provisioning.CommandLineArguments;
import com.github.nethad.clustermeister.provisioning.CommandLineEvaluation;
import com.github.nethad.clustermeister.provisioning.CommandLineHandle;
import com.github.nethad.clustermeister.provisioning.CommandRegistry;
import com.github.nethad.clustermeister.provisioning.ConfigurationKeys;
import com.github.nethad.clustermeister.provisioning.ec2.AmazonNodeManager;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFLocalDriver;
import com.github.nethad.clustermeister.provisioning.local.LocalNodeManager;
import com.github.nethad.clustermeister.provisioning.rmi.RmiInfrastructure;
import com.github.nethad.clustermeister.provisioning.torque.TorqueNodeManager;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.OutputSupplier;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas
 */
public class Provisioning {
    private Logger logger = LoggerFactory.getLogger(Loggers.CLI);
//    @Inject
//    @Named(Loggers.CLI)
//    private Logger logger;
    
    private CommandLineEvaluation commandLineEvaluation;
    private String configFilePath;
//    private String driverHost;
    private Provider provider;
    private Configuration configuration;
    private RmiInfrastructure rmiInfrastructure;
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
        startRemoteLogging();
        
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
            case LOCAL:
                startLocalSetup();
                break;
            default:
                throw new RuntimeException("Unknown provider");
        }
    }

    public void commandShutdown(CommandLineArguments arguments) {
        commandLineEvaluation.shutdown(arguments);
        shutdownDriver();
    }   
    
    public void commandUnknownFallback(String command, CommandLineArguments arguments) {
        commandLineEvaluation.handleCommand(command, arguments);
    }
    
    public void commandState(CommandLineArguments arguments) {
        commandLineEvaluation.state(arguments);
    }
    
    protected Provider getProvider() {
        return provider;
    }
    
    private void readConfigFile() {
        if (configFilePath == null || !(new File(configFilePath).exists())) {
            logger.warn("Configuration file \""+configFilePath+"\" does not exist, create default configuration.");
            createDefaultConfiguration(configFilePath);
        }
        try {
            configuration = new YamlConfiguration(configFilePath);
        } catch (ConfigurationException ex) {
            throw new RuntimeException(ex);
        }
        
    }
    
    private void startRemoteLogging() {
        Boolean remoteLoggingActivated = configuration.getBoolean(
                ConfigurationKeys.LOGGING_NODE_REMOTE, Boolean.FALSE);
        if(remoteLoggingActivated) {
            int port = configuration.getInt(ConfigurationKeys.LOGGING_NODE_REMOTE_PORT, 
                    ConfigurationKeys.DEFAULT_LOGGING_NODE_REMOTE_PORT);
            new RemoteLoggingServer(port).start();
        }
    }

    private void shutdownDriver() {
        jppfLocalDriver.shutdown();
    }

    private void startAmazon() {
        commandLineEvaluation = AmazonNodeManager.commandLineEvaluation(configuration, commandLineHandle, rmiInfrastructure);
        startLocalDriver();
    }

    private void startLocalDriver() {
        jppfLocalDriver = new JPPFLocalDriver(configuration);
        jppfLocalDriver.execute();
    }

    private void startTorque() {
        jppfLocalDriver = new JPPFLocalDriver(configuration);
        commandLineEvaluation = TorqueNodeManager.commandLineEvaluation(configuration, commandLineHandle, jppfLocalDriver, rmiInfrastructure.getRmiServerForApiObject());
        jppfLocalDriver.execute();
    }
    
    private void startTestSetup() {
        commandLineEvaluation = new CommandLineEvaluation() {
            @Override
            public void state(CommandLineArguments arguments) {} // do nothing
            @Override
            public void shutdown(CommandLineArguments arguments) {} // do nothing
            @Override
            public void handleCommand(String command, CommandLineArguments arguments) {} // do nothing
            @Override
            public CommandLineHandle getCommandLineHandle() { return null; }
        };
        startLocalDriver();
        jppfLocalDriver.update(null, "127.0.0.1");
    }
    
    private void startLocalSetup() {
        logger.info("start local setup.");
        commandLineEvaluation = LocalNodeManager.commandLineEvaluation(configuration, commandLineHandle, rmiInfrastructure.getRmiServerForApiObject());
        startLocalDriver();
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

    private void createDefaultConfiguration(String configFilePath) {
        final File configFile = new File(configFilePath);
        if (configFile.exists()) {
            return;
        }
        configFile.getParentFile().mkdirs();
        
        OutputSupplier<OutputStreamWriter> writer = Files.newWriterSupplier(configFile, Charsets.UTF_8);
        OutputStreamWriter output = null;
        try {
            output = writer.getOutput();
            output.append(YamlConfiguration.defaultConfiguration());
            output.flush();
            output.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException ex) {
                    // ignore
                }
            }
        }
    }

    
}
