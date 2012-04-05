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
package com.github.nethad.clustermeister.integration.sc02;

import com.github.nethad.clustermeister.api.Clustermeister;
import com.github.nethad.clustermeister.api.ExecutorNode;
import com.github.nethad.clustermeister.api.impl.ClustermeisterFactory;
import com.github.nethad.clustermeister.integration.Assertions;
import com.github.nethad.clustermeister.integration.ReturnStringCallable;
import com.github.nethad.clustermeister.provisioning.cli.Provider;
import com.github.nethad.clustermeister.provisioning.cli.Provisioning;
import com.github.nethad.clustermeister.provisioning.rmi.NodeConnectionListener;
import com.github.nethad.clustermeister.provisioning.rmi.RmiInfrastructure;
import com.github.nethad.clustermeister.provisioning.rmi.RmiServerForDriver;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.management.JPPFSystemInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas
 */
public class Scenario02 implements NodeConnectionListener {
    protected Provisioning provisioning;
    private final Logger logger = LoggerFactory.getLogger(Scenario02.class);
    
    private Map<String, JPPFManagementInfo> nodes = new HashMap<String, JPPFManagementInfo>();
    private int numberOfNodes;
    private final AtomicInteger actualNumberOfNodes = new AtomicInteger(0);
    
    public static void main(String... args) {
        new Scenario02().execute();
    }
    
    public void execute() {
        String configFilePath = System.getProperty("user.home") + "/.clustermeister/configuration.properties";
        provisioning = new Provisioning(configFilePath, Provider.TORQUE);
        RmiInfrastructure rmiInfrastructure = provisioning.getRmiInfrastructure();
        RmiServerForDriver rmiServerForDriver = rmiInfrastructure.getRmiServerForDriverObject();
        rmiServerForDriver.addNodeConnectionListener(this);
        //        IRmiServerForDriver rmiServerForDriver = rmiInfrastructure.getRmiServerForDriver();
        
//        rmiInfrastructure.
        provisioning.execute();
        numberOfNodes = 3;
        provisioning.addNodes(numberOfNodes, 1);
    }

    @Override
    public void onNodeConnected(JPPFManagementInfo jppfmi, JPPFSystemInformation jppfsi) {
        synchronized (this)  {
            if (!nodes.containsKey(jppfmi.getId())) {
                nodes.put(jppfmi.getHost(), jppfmi);
            }
            checkNumberOfNodes(actualNumberOfNodes.incrementAndGet());
        }
    }

    @Override
    public void onNodeDisconnected(JPPFManagementInfo jppfmi) {
        synchronized (this) {
            nodes.remove(jppfmi.getId());
        }
    }

    private void checkNumberOfNodes(final int currentNumber) {
        logger.info("Check number of nodes. currentNumber: {}; number of nodes: {}.", currentNumber, numberOfNodes);
        if (currentNumber >= numberOfNodes) {
            runScenario();
        }
    }

    private void runScenario() {
        logger.info("Running scenario {}", getClass().getName());
        try {
            Clustermeister clustermeister = ClustermeisterFactory.create();
            Collection<ExecutorNode> allNodes = clustermeister.getAllNodes();
//            Assertions.assertEquals(numberOfNodes, allNodes.size(), numberOfNodes+" nodes should have been added to torque.");
            final String returnString = "it worked!";
            List<ListenableFuture<String>> results = new ArrayList<ListenableFuture<String>>();
            for (ExecutorNode node : allNodes) {
                results.add(node.execute(new ReturnStringCallable(returnString)));
            }
            
            for (ListenableFuture<String> result : results) {
                try {
                    String resultString = result.get(10, TimeUnit.SECONDS);
                    Assertions.assertEquals(returnString, resultString, "Returned string was not as expected.");
                } catch (TimeoutException ex) {
                    throw new RuntimeException(ex);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                } catch (ExecutionException ex) {
                    throw new RuntimeException(ex);
                }
            }
        } finally {
            provisioning.shutdown();
        }
        logger.info("System.exit");
        System.exit(0);
    }
    
}
