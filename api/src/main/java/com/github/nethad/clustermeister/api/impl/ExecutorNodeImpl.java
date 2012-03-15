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
package com.github.nethad.clustermeister.api.impl;

import com.github.nethad.clustermeister.api.ExecutorNode;
import com.github.nethad.clustermeister.api.NodeCapabilities;
import com.github.nethad.clustermeister.api.NodeType;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 *
 * @author thomas
 */
public class ExecutorNodeImpl implements ExecutorNode {
    
    private NodeCapabilities nodeCapabilities;
    private String uuid;
    private Set<String> publicAddresses = new HashSet<String>();
    private Set<String> privateAddresses = new HashSet<String>();

    @Override
    public NodeCapabilities getCapabilities() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public void setNodeCapabilities(NodeCapabilities nodeCapabilities) {
        this.nodeCapabilities = nodeCapabilities;
    }

    @Override
    public <T> Future<T> execute(Callable<T> callable) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getID() {
        return uuid;
    }
    
    public void setId(String id) {
        this.uuid = id;
    }

    @Override
    public NodeType getType() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<String> getPublicAddresses() {
        return this.publicAddresses;
    }
    
    public void addPublicAddresses(Set<String> ipAddresses) {
        publicAddresses.addAll(ipAddresses);
    }

    @Override
    public Set<String> getPrivateAddresses() {
        return this.privateAddresses;
    }
    
    public void addPrivateAddresses(Set<String> ipAddresses) {
        this.privateAddresses.addAll(ipAddresses);
    }

    @Override
    public int getManagementPort() {
        throw new UnsupportedOperationException("Not supported yet.");
    }


    
}
