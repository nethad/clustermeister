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
import com.github.nethad.clustermeister.api.NodeCapabilities;
import com.github.nethad.clustermeister.api.NodeType;
import com.github.nethad.clustermeister.api.impl.FileConfiguration;
import com.github.nethad.clustermeister.provisioning.ec2.AmazonInstanceShutdownMethod;
import com.github.nethad.clustermeister.provisioning.ec2.AmazonNodeConfiguration;
import com.github.nethad.clustermeister.provisioning.ec2.AmazonNodeManager;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFConfiguratedComponentFactory;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFLocalDriver;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFManagementByJobsClient;
import com.github.nethad.clustermeister.provisioning.rmi.RmiInfrastructure;
import com.github.nethad.clustermeister.provisioning.torque.TorqueNodeConfiguration;
import com.github.nethad.clustermeister.provisioning.torque.TorqueNodeManager;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
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
    private AmazonNodeManager amazonNodeManager;
    private JPPFManagementByJobsClient amazonManagementClient = null;
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
            case TEST:
                startTestSetup();
                break;
            default:
                throw new RuntimeException("Unknown provider");
        }
    }
    
    public void shutdown() {
        switch(provider) {
            case AMAZON:
                shutdownAmazon();
                break;
            case TORQUE:
                shutdownTorque();
                break;
            case TEST:
                break;
            default:
                throw new RuntimeException("Unknown provider");
        }
    }
    
    public void addNodes(int numberOfNodes, int numberOfCpusPerNode) {
        switch(provider) {
            case AMAZON:
                addAmazonNodes(numberOfCpusPerNode, numberOfNodes);
                break;
            case TORQUE:
                addTorqueNodes(numberOfCpusPerNode, numberOfNodes);
                break;
            case TEST:
                break;
            default:
                throw new RuntimeException("Unknown provider");
        }
    }

    private void addTorqueNodes(int numberOfCpusPerNode, int numberOfNodes) {
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
    
    private void addAmazonNodes(final int numberOfCpusPerNode, int numberOfNodes) {
        final AmazonNodeConfiguration amazonNodeConfiguration = new AmazonNodeConfiguration();
        amazonNodeConfiguration.setDriverAddress("localhost");
        amazonNodeConfiguration.setNodeCapabilities(new NodeCapabilities() {
            @Override
            public int getNumberOfProcessors() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public int getNumberOfProcessingThreads() {
                return numberOfCpusPerNode;
            }

            @Override
            public String getJppfConfig() {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        });
        amazonNodeConfiguration.setNodeType(NodeType.NODE);
        amazonNodeConfiguration.setRegion("eu-west-1c");
        
        logger.info("Starting {} nodes.", numberOfNodes);
        List<ListenableFuture<? extends Node>> futures = 
                new ArrayList<ListenableFuture<? extends Node>>(numberOfNodes);
        for (int i = 0; i < numberOfNodes; i++) {
            futures.add(amazonNodeManager.addNode(amazonNodeConfiguration, 
                    Optional.<String>absent()));
        }
        try {
            List<? extends Node> startedNodes = Futures.successfulAsList(futures).get();
            int failedNodes = Iterables.frequency(startedNodes, null);
            if(failedNodes > 0) {
                logger.warn("{} nodes failed to start.", failedNodes);
            }
        } catch (InterruptedException ex) {
            logger.warn("Interrupted while waiting for nodes to start. Nodes may not be started properly.", ex);
        } catch (ExecutionException ex) {
            logger.warn("Failed to wait for nodes to start.", ex);
        }
    }
    
    protected Provider getProvider() {
        return provider;
    }
    
    protected int getNumberOfRunningNodes() {
        switch(provider) {
            case AMAZON:
                return amazonNodeManager.getNodes().size();
            case TORQUE:
                return torqueNodeManager.getNodes().size();
            default:
                throw new RuntimeException("Unknown provider");
        }
    }
    
    private void readConfigFile() {
        if (configFilePath == null || !(new File(configFilePath).exists())) {
            logger.warn("Configuration file \""+configFilePath+"\" does not exist.");
        } else {
            configuration = new FileConfiguration(configFilePath);
        }
    }

    private void startAmazon() {
        amazonNodeManager = new AmazonNodeManager(configuration);
        amazonManagementClient = JPPFConfiguratedComponentFactory.getInstance().
                createManagementByJobsClient("localhost", 11111);
        amazonNodeManager.registerManagementClient(amazonManagementClient);
        jppfLocalDriver = new JPPFLocalDriver();
        jppfLocalDriver.execute();
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
    
    private void startTestSetup() {
        jppfLocalDriver = new JPPFLocalDriver();
        jppfLocalDriver.execute();
        jppfLocalDriver.update(null, "127.0.0.1");
        driverHost = "127.0.0.1";
    }
    
    private void shutdownTorque() {
        if (torqueNodeManager != null) {
            torqueNodeManager.removeAllNodes();
            torqueNodeManager.shutdown();
        }
        jppfLocalDriver.shutdown();
    }
    
    private void shutdownAmazon() {
        if (amazonNodeManager != null) {
            amazonNodeManager.removeAllNodes(AmazonInstanceShutdownMethod.TERMINATE);
            amazonNodeManager.close();
        }
        jppfLocalDriver.shutdown();
    }
    
    private TorqueNodeConfiguration getTorqueDriverConfiguration() {
        return new TorqueNodeConfiguration(NodeType.DRIVER, "", true, 1);
    }

    @VisibleForTesting
    public RmiInfrastructure getRmiInfrastructure() {
        return rmiInfrastructure;
    }



    
}
