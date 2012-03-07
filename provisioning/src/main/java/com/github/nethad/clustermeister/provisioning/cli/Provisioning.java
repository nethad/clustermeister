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
import com.github.nethad.clustermeister.provisioning.torque.TorqueNodeConfiguration;
import com.github.nethad.clustermeister.provisioning.torque.TorqueNodeManager;
import com.github.nethad.clustermeister.provisioning.utils.PublicIp;
import com.google.common.util.concurrent.ListenableFuture;

/**
 *
 * @author thomas
 */
public class Provisioning {
    
    private String configFilePath;
    private int numberOfNodes;
    private Provider provider;
    private Configuration configuration;
    private TorqueNodeManager torqueNodeManager;

    public Provisioning(String configFilePath, int numberOfNodes, Provider provider) {
        this.configFilePath = configFilePath;
        this.numberOfNodes = numberOfNodes;
        this.provider = provider;
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
    
    private void readConfigFile() {
        configuration = new FileConfiguration(configFilePath);
    }

    private void startAmazon() {
        throw new UnsupportedOperationException("Amazon provisioning not yet implemented");
    }

    private void startTorque() {
        torqueNodeManager = new TorqueNodeManager(configuration);
        ListenableFuture<? extends Node> driver = torqueNodeManager.addNode(getTorqueDriverConfiguration());
        String driverHost = PublicIp.getPublicIp();
        for (int i = 0; i < numberOfNodes; i++) {
            torqueNodeManager.addNode(getTorqueNodeConfiguration(driverHost));
        }
    }
    
    private void shutdownTorque() {
        torqueNodeManager.removeAllNodes();
        torqueNodeManager.shutdown();
    }
    
    private TorqueNodeConfiguration getTorqueDriverConfiguration() {
        return new TorqueNodeConfiguration(NodeType.DRIVER, "", true);
    }
    
    private TorqueNodeConfiguration getTorqueNodeConfiguration(String driverHost) {
        return new TorqueNodeConfiguration(NodeType.NODE, driverHost, true);
    }


    
}
