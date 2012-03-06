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
package com.github.nethad.clustermeister.provisioning.torque;

import com.github.nethad.clustermeister.api.Node;
import com.github.nethad.clustermeister.api.NodeConfiguration;
import com.github.nethad.clustermeister.api.NodeType;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author thomas
 */
public class TorqueJPPFTestSetup {

    private static final int NUMBER_OF_NODES = 6;

    public static void main(String... args) {
        new TorqueJPPFTestSetup().execute();
    }
//	private ListenableFuture<? extends Node> driver;
//	private List<ListenableFuture<? extends Node>> nodes;
    private TorqueNodeManager torqueNodeManager;
//	private Node driverNode;

    private void execute() {

        torqueNodeManager = new TorqueNodeManager(null);

        startDriver();
        startNodes();
        try {
            Thread.sleep(200);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        System.out.print("Press ENTER to shutdown: ");
        try {
            int read = System.in.read();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        torqueNodeManager.shutdown();

        torqueNodeManager.removeAllNodes();

        torqueNodeManager = null;

        System.out.print("Press ENTER to kill the JVM: ");
        try {
            int read = System.in.read();
            System.exit(0);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

//		for (ListenableFuture<? extends Node> node : nodes) {
//			torqueNodeManager.removeNode((TorqueNode)node);
//		}

//		JPPFManagementByJobsClient client = JPPFConfiguratedComponentFactory.getInstance().createManagementByJobsClient("localhost", 11111);
//		client.shutdownAllNodes("localhost", 11198);
//		client.shutdownDriver("localhost", 11198);
    }

    private void startDriver() {
//        TorqueLocalRunner runner = new TorqueLocalRunner();
//        runner.start();
        NodeConfiguration nodeConfiguration = new TorqueNodeConfiguration(NodeType.DRIVER);
        ListenableFuture<? extends Node> driver = torqueNodeManager.addNode(nodeConfiguration);
        try {
            driver.get();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        } catch (ExecutionException ex) {
            ex.printStackTrace();
        }
    }

    private void startNodes() {

        torqueNodeManager.deployResources();
        NodeConfiguration nodeConfiguration = new TorqueNodeConfiguration(NodeType.NODE);
        for (int i = 0; i < NUMBER_OF_NODES; i++) {
            ListenableFuture<? extends Node> node = torqueNodeManager.addNode(nodeConfiguration);
            try {
                node.get();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            } catch (ExecutionException ex) {
                ex.printStackTrace();
            }
        }
//		nodes = new ArrayList<ListenableFuture<? extends Node>>();
//			nodes.add(torqueNodeManager.addNode(nodeConfiguration));
    }
//    private class TorqueLocalRunner extends Thread {
//
//        private TorqueJPPFDriverDeployer deployer;
//
//        @Override
//        public void run() {
//            System.out.println("Start driver");
//            deployer = new TorqueJPPFDriverDeployer();
//            deployer.execute();
//        }
//
//        public void stopDriver() {
//            deployer.stopLocalDriver();
//        }
//    }
}
