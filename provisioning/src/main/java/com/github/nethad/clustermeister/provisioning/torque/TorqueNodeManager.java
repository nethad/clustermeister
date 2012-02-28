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
import com.github.nethad.clustermeister.api.NodeType;
import com.github.nethad.clustermeister.provisioning.ec2.AmazonInstanceManager;
import com.github.nethad.clustermeister.provisioning.ec2.AmazonNode;
import com.github.nethad.clustermeister.provisioning.utils.SSHClientExcpetion;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jppf.server.nio.nodeserver.NodeNioServer;

/**
 * TODO: - functionality like add(deploy)/remove JPPD nodes. - starting/deploying drivers - this is not yet to be
 * directly used by the user but by a higher layer of abstraction (to be defined) - this will likely need to implement a
 * yet to be defined interface together with AmazonNodeManager
 *
 * @author daniel
 */
public class TorqueNodeManager {
	public static final int THREAD_POOL_SIZE = 2;

	private class AddNormalNodeTask implements Callable<TorqueNode> {

		private final NodeConfiguration nodeConfiguration;
		
		public AddNormalNodeTask(NodeConfiguration nodeConfiguration) {
			this.nodeConfiguration = nodeConfiguration;
		}

		@Override
		public TorqueNode call() throws Exception {
			return nodeDeployer.submitJob(nodeConfiguration);
		}
	}

	private class AddDriverNodeTask implements Callable<TorqueNode> {

		public AddDriverNodeTask() {
		}

		@Override
		public TorqueNode call() throws Exception {
			return driverDeployer.execute();
		}
	}

	private final Configuration configuration;
	private Set<TorqueNode> drivers = new HashSet<TorqueNode>();
	private ListeningExecutorService executorService;
	private Set<TorqueNode> nodes = new HashSet<TorqueNode>();
	private TorqueJPPFNodeDeployer nodeDeployer;
	private final TorqueJPPFDriverDeployer driverDeployer;

	public TorqueNodeManager(Configuration configuration) {
		this.configuration = configuration;
		executorService = MoreExecutors.listeningDecorator(
				Executors.newCachedThreadPool());
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
		if (nodeConfiguration.getType() == NodeType.DRIVER) {
			return addDriverNode(nodeConfiguration);
		} else {
			return addNormalNode(nodeConfiguration);
		}
	}

	private ListenableFuture<? extends Node> addDriverNode(NodeConfiguration nodeConfiguration) {
		if (!nodeConfiguration.isDriverDeployedLocally()) {
			driverDeployer.runExternally();
		}
		return executorService.submit(new AddDriverNodeTask());
	}

	private ListenableFuture<? extends Node> addNormalNode(NodeConfiguration nodeConfiguration) {
//		try {
			return executorService.submit(new AddNormalNodeTask(nodeConfiguration));
//		} catch (SSHClientExcpetion ex) {
//			Logger.getLogger(TorqueNodeManager.class.getName()).log(Level.SEVERE, null, ex);
//		}
	}
	
}
