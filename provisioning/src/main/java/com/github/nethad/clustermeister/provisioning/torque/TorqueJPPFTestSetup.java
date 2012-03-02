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
import com.github.nethad.clustermeister.provisioning.jppf.JPPFConfiguratedComponentFactory;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFDriverConfigurationSource;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFManagementByJobsClient;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
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
		
		System.out.println("Sleep 10s");
		try {
			Thread.sleep(10000);
		} catch (InterruptedException ex) {
			// nop
		}
		
		System.out.println("Number of nodes: "+torqueNodeManager.getNodes().size());
		
		System.out.println("Kill all nodes.");
		torqueNodeManager.removeAllNodes();
		
		System.out.println("All nodes removed.");
		torqueNodeManager.shutdown();
		
		System.out.println("Number of nodes: "+torqueNodeManager.getNodes().size());
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
		torqueNodeManager.addNode(nodeConfiguration);
    }

    private void startNodes() {
		
		torqueNodeManager.deployResources();
		NodeConfiguration nodeConfiguration = new TorqueNodeConfiguration(NodeType.NODE);
		for (int i = 0; i<NUMBER_OF_NODES; i++) {
			torqueNodeManager.addNode(nodeConfiguration);
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
