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

import com.github.nethad.clustermeister.api.Configuration;
import com.github.nethad.clustermeister.api.Node;
import com.github.nethad.clustermeister.api.NodeConfiguration;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.Monitor;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.NodeMetadata;

/**
 *
 * @author daniel
 */
public class AmazonNodeManager {
	
	final AmazonInstanceManager amazonInstanceManager;
	final Configuration configuration;
	
	//TODO: make sure this will not cause a memory leak
	private Set<AmazonNode> drivers = new HashSet<AmazonNode>();
	private Set<AmazonNode> nodes = new HashSet<AmazonNode>();
	private final Monitor managedNodesMonitor = new Monitor(false);
	
	private final ListeningExecutorService executorService;

	public AmazonNodeManager(Configuration configuration) {
		this.configuration = configuration;
		executorService = MoreExecutors.listeningDecorator(
				Executors.newCachedThreadPool());
		this.amazonInstanceManager = 
				new AmazonInstanceManager(configuration, executorService);
	}
	
	public Collection<? extends Node> getNodes() {
		managedNodesMonitor.enter();
		try {
			List<AmazonNode> allNodes = 
					new ArrayList<AmazonNode>(drivers.size() + nodes.size());
			allNodes.addAll(nodes);
			allNodes.addAll(drivers);
			return Collections.unmodifiableCollection(allNodes);
		} finally {
			managedNodesMonitor.leave();
		}
	}

	public ListenableFuture<? extends Node> addNode(NodeConfiguration nodeConfiguration, 
			Optional<String> instanceId) {
		return executorService.submit(new AddNodeTask(nodeConfiguration, instanceId));
	}

	public void close() {
		if(amazonInstanceManager != null) {
			managedNodesMonitor.enter();
			try {
				drivers.clear();
				nodes.clear();
			} finally {
				managedNodesMonitor.leave();
			}
			amazonInstanceManager.close();
		}
	}
	
	private void addManagedNode(AmazonNode node) {
		managedNodesMonitor.enter();
		try {
			switch(node.getType()) {
				case NODE: {
					nodes.add(node);
					break;
				}
				case DRIVER:  {
					drivers.add(node);
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
	
	private class AddNodeTask implements Callable<AmazonNode> {

		private final NodeConfiguration nodeConfiguration;
		private final Optional<String> instanceId;

		public AddNodeTask(NodeConfiguration nodeConfiguration, Optional<String> instanceId) {
			this.nodeConfiguration = nodeConfiguration;
			this.instanceId = instanceId;
		}
		
		@Override
		public AmazonNode call() throws Exception {
			NodeMetadata instanceMetadata;
			if (!instanceId.isPresent()) {
				try {
					Optional<Map<String, String>> noMap = Optional.absent();
					instanceMetadata = amazonInstanceManager.createInstance(noMap);
				} catch (RunNodesException ex) {
					return null;
				}
			} else {
				instanceMetadata = amazonInstanceManager.getInstanceMetadata(instanceId.get());
			}
			AmazonNode node;
			try {
				node = amazonInstanceManager.deploy(instanceMetadata, nodeConfiguration);
			} catch (Throwable ex) {
				if (instanceId.isPresent()) {
					amazonInstanceManager.suspendInstance(instanceMetadata.getId());
				} else {
					amazonInstanceManager.terminateInstance(instanceMetadata.getId());
				}
				return null;
			}

			addManagedNode(node);

			return node;
		}
		
	}
}
