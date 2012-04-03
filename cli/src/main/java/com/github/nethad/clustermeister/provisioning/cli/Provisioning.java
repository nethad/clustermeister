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

import com.github.nethad.clustermeister.api.Configuration;
import com.github.nethad.clustermeister.api.Node;
import com.github.nethad.clustermeister.api.NodeType;
import com.github.nethad.clustermeister.api.impl.FileConfiguration;
import com.github.nethad.clustermeister.provisioning.cli.Provider;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFLocalDriver;
import com.github.nethad.clustermeister.provisioning.rmi.RmiInfrastructure;
import com.github.nethad.clustermeister.provisioning.torque.TorqueNodeConfiguration;
import com.github.nethad.clustermeister.provisioning.torque.TorqueNodeManager;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas
 */
public class Provisioning {
    
    private String configFilePath;
    private String driverHost;
    private Provider provider;
    private Configuration configuration;
    private RmiInfrastructure rmiInfrastructure;
    private TorqueNodeManager torqueNodeManager;
    private Logger logger = LoggerFactory.getLogger(Provisioning.class);
    private JPPFLocalDriver jppfLocalDriver;

    public Provisioning(String configFilePath, Provider provider) {
        this.configFilePath = configFilePath;
        this.provider = provider;
        rmiInfrastructure = new RmiInfrastructure();
        rmiInfrastructure.initialize();
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
            default:
                throw new RuntimeException("Unknown provider");
        }
    }
    
    public void shutdown() {
        shutdownTorque();
    }
    
    void addNodes(int numberOfNodes, int numberOfCpusPerNode) {
        final TorqueNodeConfiguration torqueNodeConfiguration = 
                TorqueNodeConfiguration.configurationForNode(driverHost, numberOfCpusPerNode);
        
        ListenableFuture<? extends Node> lastNode = null;
        for (int i = 0; i < numberOfNodes; i++) {
            lastNode = torqueNodeManager.addNode(torqueNodeConfiguration);
        }
        try {
            lastNode.get();
        } catch (InterruptedException ex) {
            logger.warn("Waited for last node to start up", ex);
        } catch (ExecutionException ex) {
            logger.warn("Waited for last node to start up", ex);
//        } catch (TimeoutException ex) {
//            logger.warn("Waited for last node to start up", ex);
        }
    }
    
    protected Provider getProvider() {
        return provider;
    }
    
    protected int getNumberOfRunningNodes() {
        return torqueNodeManager.getNodes().size();
    }
    
    private void readConfigFile() {
        if (!(new File(configFilePath).exists())) {
            logger.error("Configuration file \""+configFilePath+"\" does not exist.");
        }
        configuration = new FileConfiguration(configFilePath);
    }

    private void startAmazon() {
        throw new UnsupportedOperationException("Amazon provisioning not yet implemented");
    }

    private void startTorque() {
        torqueNodeManager = new TorqueNodeManager(configuration);
        
        jppfLocalDriver = new JPPFLocalDriver();
        torqueNodeManager.addPublicIpListener(jppfLocalDriver);
        jppfLocalDriver.execute();
        driverHost = jppfLocalDriver.getIpAddress();
        //        ListenableFuture<? extends Node> driver = torqueNodeManager.addNode(getTorqueDriverConfiguration());
        //        driverHost = PublicIp.getPublicIp();
        //        try {
        //            driver.get();
        //        } catch (InterruptedException ex) {
        //            logger.error("Error while waiting for driver to start up.", ex);
        //        } catch (ExecutionException ex) {
        //            logger.error("Error while waiting for driver to start up.", ex);
        //        }
    }
    
    private void shutdownTorque() {
        torqueNodeManager.removeAllNodes();
        torqueNodeManager.shutdown();
        jppfLocalDriver.shutdown();
    }
    
    private TorqueNodeConfiguration getTorqueDriverConfiguration() {
        return new TorqueNodeConfiguration(NodeType.DRIVER, "", true, 1);
    }



    
}
