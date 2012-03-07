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

import com.github.nethad.clustermeister.api.NodeConfiguration;
import com.github.nethad.clustermeister.api.NodeType;

/**
 *
 * @author daniel
 */
public class AmazonNodeConfiguration implements NodeConfiguration {

    private NodeType nodeType = NodeType.NODE;
    private String userName = "ec2-user";
    private String privateKey = "";
    private String driverAddress = "";
    private boolean driverDeployedLocally = false;
    private int managementPort = AmazonNodeManager.DEFAULT_MANAGEMENT_PORT;

    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    @Override
    public NodeType getType() {
        return nodeType;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public String getUserName() {
        return userName;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }
    
    public String getPrivateKey() {
        return privateKey;
    }

    public void setDriverAddress(String driverAddress) {
        this.driverAddress = driverAddress;
    }

    @Override
    public String getDriverAddress() {
        return driverAddress;
    }

    public void setDriverDeployedLocally(boolean driverDeployedLocally) {
        this.driverDeployedLocally = driverDeployedLocally;
    }

    @Override
    public boolean isDriverDeployedLocally() {
        return driverDeployedLocally;
    }

    public void setManagementPort(int managementPort) {
        this.managementPort = managementPort;
    }

    public int getManagementPort() {
        return managementPort;
    }
}
