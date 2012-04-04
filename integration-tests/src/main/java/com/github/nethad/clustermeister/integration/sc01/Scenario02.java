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
package com.github.nethad.clustermeister.integration.sc01;

import com.github.nethad.clustermeister.api.Clustermeister;
import com.github.nethad.clustermeister.api.ExecutorNode;
import com.github.nethad.clustermeister.api.impl.ClustermeisterFactory;
import com.github.nethad.clustermeister.integration.Assertions;
import com.github.nethad.clustermeister.provisioning.cli.Provider;
import com.github.nethad.clustermeister.provisioning.cli.Provisioning;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFNodeConfiguration;
import com.github.nethad.clustermeister.provisioning.rmi.NodeConnectionListener;
import com.github.nethad.clustermeister.provisioning.rmi.RmiInfrastructure;
import com.github.nethad.clustermeister.provisioning.rmi.RmiServerForDriver;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.*;
import java.util.Collection;
import java.util.Enumeration;
import java.util.concurrent.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.management.JPPFSystemInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas
 */
public class Scenario02 implements NodeConnectionListener {
    private JPPFTestNode node;
    private Process nodeProcess;
    private final Logger logger = LoggerFactory.getLogger(Scenario02.class);
    
    public static void main(String... args) throws InterruptedException {
        new Scenario02().execute();
    }
    private Provisioning provisioning;
    private boolean scenarioStarted = false;
    private boolean shuttingDown = false;

    private void execute() throws InterruptedException {
        
        final Scenario02 that = this;
        
        new Thread(new Runnable() {

            @Override
            public void run() {
//                String configFilePath = System.getProperty("user.home") + "/.clustermeister/configuration.properties";
                provisioning = new Provisioning(null, Provider.TEST);
                RmiInfrastructure rmiInfrastructure = provisioning.getRmiInfrastructure();
                RmiServerForDriver rmiServerForDriver = rmiInfrastructure.getRmiServerForDriverObject();
                rmiServerForDriver.addNodeConnectionListener(that);
                provisioning.execute();
            }
        }).start();
        node = new JPPFTestNode();
        node.prepare();
        node.startNode();
//                startNode();
//            }
//        }).start();
    }

    private void runScenario() throws InterruptedException, RuntimeException {
        System.out.println("###### RUN SCENARIO!!");

        Clustermeister clustermeister = ClustermeisterFactory.create();
        try {
            logger.info("Start Clustermeister.");
            Collection<ExecutorNode> allNodes = clustermeister.getAllNodes();
            logger.info("nodes size = {}", allNodes.size());
            Assertions.assertEquals(1, allNodes.size(), "Number of nodes not as expected");
            if (allNodes.size() > 0) {
                ListenableFuture<String> result = allNodes.iterator().next().execute(new SampleCallable());
                try {
                    String resultString = result.get();
                    Assertions.assertEquals("It works!", resultString, "Result string is not as expected.");
                } catch (ExecutionException ex) {
                    throw new RuntimeException(ex);
                }
            }
        } finally {
            clustermeister.shutdown();
            shutdown();
        }
    }
    
    private void shutdown() {
        shuttingDown = true;
        node.shutdown();
        provisioning.shutdown();
        System.exit(0);
    }

    @Override
    public void onNodeConnected(JPPFManagementInfo jppfmi, JPPFSystemInformation jppfsi) {
        if (!scenarioStarted) {
            new Thread(new Runnable() {

                @Override
                public void run() {
                try {
                    runScenario();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                } catch (RuntimeException ex) {
                    ex.printStackTrace();
                }
                }
            }).start();
            scenarioStarted = true;
        }
    }

    @Override
    public void onNodeDisconnected(JPPFManagementInfo jppfmi) {
        if (!shuttingDown) {
            shutdown();
        }
    }

}
