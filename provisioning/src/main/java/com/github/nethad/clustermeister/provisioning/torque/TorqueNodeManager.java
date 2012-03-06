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

import com.github.nethad.clustermeister.api.Configuration;
import com.github.nethad.clustermeister.api.Node;
import com.github.nethad.clustermeister.api.NodeConfiguration;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFConfiguratedComponentFactory;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFManagementByJobsClient;
import com.github.nethad.clustermeister.provisioning.utils.NodeManagementConnector;
import com.github.nethad.clustermeister.provisioning.utils.SSHClientExcpetion;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.Monitor;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.management.JMXNodeConnectionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: - functionality like add(deploy)/remove JPPD nodes. - starting/deploying drivers - this is not yet to be
 * directly used by the user but by a higher layer of abstraction (to be defined) - this will likely need to implement a
 * yet to be defined interface together with AmazonNodeManager
 *
 * @author daniel
 */
public class TorqueNodeManager implements TorqueNodeManagement {

	public static final int THREAD_POOL_SIZE = 2;
	private Logger logger = LoggerFactory.getLogger(TorqueNodeManager.class);
	
	private final Monitor managedNodesMonitor = new Monitor(false);

	private class AddNormalNodeTask implements Callable<TorqueNode> {

		private final NodeConfiguration nodeConfiguration;
		private final TorqueNodeManagement torqueNodeManagement;

		public AddNormalNodeTask(NodeConfiguration nodeConfiguration, TorqueNodeManagement torqueNodeManagement) {
			this.nodeConfiguration = nodeConfiguration;
			this.torqueNodeManagement = torqueNodeManagement;
		}

		@Override
		public TorqueNode call() throws Exception {
			return nodeDeployer.submitJob(nodeConfiguration, torqueNodeManagement);
		}
	}

	private class AddDriverNodeTask implements Callable<TorqueNode> {

		private final TorqueNodeManagement torqueNodeManagement;

		public AddDriverNodeTask(TorqueNodeManagement torqueNodeManagement) {
			this.torqueNodeManagement = torqueNodeManagement;
		}

		@Override
		public TorqueNode call() throws Exception {
			return driverDeployer.execute(torqueNodeManagement);
		}
	}
	
	private class RemoveDriverNodeTask implements Callable<Void> {
		private String host;
		private int managementPort;

		public RemoveDriverNodeTask(String host, int managementPort) {
			this.host = host;
			this.managementPort = managementPort;
		}

		@Override
		public Void call() throws Exception {
			JMXDriverConnectionWrapper wrapper = 
					NodeManagementConnector.openDriverConnection(host, managementPort);
			wrapper.restartShutdown(0L, -1L);
			wrapper.close();
			return null;
		}
	}
	
	private class RemoveNormalNodeTask implements Callable<Void> {
		private String host;
		private int managementPort;

		public RemoveNormalNodeTask(String host, int managementPort) {
			this.host = host;
			this.managementPort = managementPort;
		}

		@Override
		public Void call() throws Exception {
			JMXNodeConnectionWrapper wrapper = 
					NodeManagementConnector.openNodeConnection(host, managementPort);
			wrapper.shutdown();
			wrapper.close();
			return null;
		}
	}
	
	private final Configuration configuration;
	private Set<TorqueNode> drivers = new HashSet<TorqueNode>();
	private ListeningExecutorService executorService;
	private Set<TorqueNode> nodes = new HashSet<TorqueNode>();
	private final TorqueJPPFNodeDeployer nodeDeployer;
	private final TorqueJPPFDriverDeployer driverDeployer;

	public TorqueNodeManager(Configuration configuration) {
		this.configuration = configuration;
		nodeDeployer = new TorqueJPPFNodeDeployer();
		driverDeployer = new TorqueJPPFDriverDeployer();
		executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(THREAD_POOL_SIZE));
	}

	public Collection<? extends Node> getNodes() {
		List<TorqueNode> allNodes =
				new ArrayList<TorqueNode>(drivers.size() + nodes.size());
		allNodes.addAll(nodes);
		allNodes.addAll(drivers);
		return Collections.unmodifiableCollection(allNodes);
	}

	public ListenableFuture<? extends Node> addNode(NodeConfiguration nodeConfiguration) {
		switch (nodeConfiguration.getType()) {
			case DRIVER:
				return addDriverNode(nodeConfiguration);
			case NODE:
				return addNormalNode(nodeConfiguration);
			default:
				throw new IllegalArgumentException("Invalid Node Type.");
		}
	}
	
	public ListenableFuture<Void> removeNode(TorqueNode torqueNode) {
		Preconditions.checkNotNull(torqueNode, "torqueNode must not be null.");
		switch (torqueNode.getType()) {
			case DRIVER:
				return removeDriverNode(torqueNode);
			case NODE:
				return removeNormalNode(torqueNode);
			default:
				throw new IllegalArgumentException("Invalid Node Type.");
		}
	}
	
	public void removeAllNodes() {
		TorqueNode firstDriver = drivers.iterator().next();
		String driverHost = firstDriver.getPrivateAddresses().iterator().next();
		int serverPort = firstDriver.getServerPort();
		int managementPort = firstDriver.getManagementPort();
		
		JPPFManagementByJobsClient client = JPPFConfiguratedComponentFactory.getInstance()
				.createManagementByJobsClient(
					firstDriver.getPrivateAddresses().iterator().next(), serverPort);
		client.shutdownAllNodes(driverHost, managementPort);
		nodes.clear();
        client.close();
		client.shutdownDriver(driverHost, managementPort);
		drivers.clear();
	}
	
	private ListenableFuture<Void> removeDriverNode(TorqueNode torqueNode) {
		return executorService.submit(new RemoveDriverNodeTask(
				Iterables.getFirst(torqueNode.getPublicAddresses(), null), torqueNode.getManagementPort()));
	}
	
	private ListenableFuture<Void> removeNormalNode(TorqueNode torqueNode) {
		return executorService.submit(new RemoveNormalNodeTask(
				Iterables.getFirst(torqueNode.getPublicAddresses(), null), torqueNode.getManagementPort()));
	}

	private ListenableFuture<? extends Node> addDriverNode(NodeConfiguration nodeConfiguration) {
		if (!nodeConfiguration.isDriverDeployedLocally()) {
			driverDeployer.runExternally();
		}
		return executorService.submit(new AddDriverNodeTask(this));
	}

	private ListenableFuture<? extends Node> addNormalNode(NodeConfiguration nodeConfiguration) {
		return executorService.submit(new AddNormalNodeTask(nodeConfiguration, this));
	}

	public void deployResources() {
		try {
			nodeDeployer.deployInfrastructure();
		} catch (SSHClientExcpetion ex) {
			logger.error(null, ex);
		}
	}

	@Override
	public void addManagedNode(TorqueNode torqueNode) {
		managedNodesMonitor.enter();
		try {
			switch (torqueNode.getType()) {
				case DRIVER:
					drivers.add(torqueNode);
					break;
				case NODE:
					nodes.add(torqueNode);
					break;
				default:
					throw new IllegalArgumentException("Invalid Node Type.");
			}
		} finally {
			managedNodesMonitor.leave();
		}
	}

	@Override
	public void removeManagedNode(TorqueNode torqueNode) {
		managedNodesMonitor.enter();
		try {
			switch(torqueNode.getType()) {
				case NODE: {
					nodes.remove(torqueNode);
					break;
				}
				case DRIVER:  {
					drivers.remove(torqueNode);
//					managementClients.remove(node);
					break;
				}
				default: {
					throw new IllegalArgumentException("Invalid Node Type.");
				}
			}
		} finally {
			managedNodesMonitor.leave();
		}
	}
	
	public void shutdown() {
		nodeDeployer.disconnectSshConnection();
		executorService.shutdown();
		List<Runnable> stillRunningThreads = executorService.shutdownNow();
		System.out.println("stillRunningThreads.size() = " + stillRunningThreads.size());
	}
	
}
