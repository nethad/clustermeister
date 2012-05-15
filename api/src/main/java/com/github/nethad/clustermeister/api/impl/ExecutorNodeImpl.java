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
import com.github.nethad.clustermeister.api.Node;
import com.github.nethad.clustermeister.api.NodeCapabilities;
import com.github.nethad.clustermeister.api.NodeType;
import com.github.nethad.clustermeister.api.utils.JPPFProperties;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import org.jppf.JPPFException;
import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.client.JPPFResultCollector;
import org.jppf.node.policy.Equal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas
 */
public class ExecutorNodeImpl implements ExecutorNode {
    
    private NodeCapabilities nodeCapabilities;
    private String uuid;
    private Set<String> publicAddresses = new HashSet<String>();
    private Set<String> privateAddresses = new HashSet<String>();
    private final JPPFClient client;
    private Logger logger = LoggerFactory.getLogger(ExecutorNodeImpl.class);
    private final ThreadsExecutorService threadsExecutorService;

    public ExecutorNodeImpl(JPPFClient client, ThreadsExecutorService threadsExecutorService) {
        this.client = client;
        this.threadsExecutorService = threadsExecutorService;
    }

    @Override
    public NodeCapabilities getCapabilities() {
        return nodeCapabilities;
    }
    
    public void setNodeCapabilities(NodeCapabilities nodeCapabilities) {
        this.nodeCapabilities = nodeCapabilities;
    }

    @Override
    public <T> ListenableFuture<T> execute(Callable<T> callable) {
//        JPPFTaskFuture<T> taskFuture = new JPPFTaskFuture<T>(null, 0);
        
        
        JPPFJob job = new JPPFJob();
        ExecutorNodeTask<T> task = new ExecutorNodeTask<T>(callable);
        try {
            job.addTask(task);
            job.setBlocking(false);
//            job.getSLA().setCancelUponClientDisconnect(true);
            job.getSLA().setMaxNodes(1);
            job.getSLA().setExecutionPolicy(new Equal(JPPFProperties.UUID, true, getID()));

            JPPFResultCollector collector = new JPPFResultCollector(job);
            job.setResultListener(collector);
            client.submit(job);
            
            ListenableFuture<T> submit = threadsExecutorService.submit(new ResultCollectorCallable<T>(collector));
            return submit;
        } catch (JPPFException ex) {
            logger.error("Could not execute task on node "+getID(), ex);
        } catch (Exception ex) {
            logger.error("Could not execute task on node "+getID(), ex);
        } 
        return null;
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

    @Override
    public <T extends Node> T as(Class<T> clazz) {
        return clazz.cast(this);
    }
    
}
