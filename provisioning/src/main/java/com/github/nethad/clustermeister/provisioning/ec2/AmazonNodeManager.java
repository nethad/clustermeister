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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jclouds.compute.domain.NodeMetadata;

/**
 *
 * @author daniel
 */
public class AmazonNodeManager {
	
	AmazonInstanceManager amazonInstanceManager;
	Configuration configuration;
	
	//TODO: make sure this will not cause a memory leak
	private Set<AmazonNode> drivers = new HashSet<AmazonNode>();
	private Set<AmazonNode> nodes = new HashSet<AmazonNode>();

	public AmazonNodeManager(Configuration configuration) {
		this.configuration = configuration;
		this.amazonInstanceManager = new AmazonInstanceManager(configuration);
		amazonInstanceManager.init();
	}
	
	public Collection<? extends Node> getNodes() {
		List<AmazonNode> allNodes = 
				new ArrayList<AmazonNode>(drivers.size() + nodes.size());
		allNodes.addAll(nodes);
		allNodes.addAll(drivers);
		return Collections.unmodifiableCollection(allNodes);
	}

	public Node addNode(NodeConfiguration nodeConfiguration, Optional<String> instanceId) {
		AmazonNode node;
		if(!instanceId.isPresent()) {
			node = createNewInstance(nodeConfiguration, null);
		} else {
			node = new AmazonNode(nodeConfiguration.getType(), 
					amazonInstanceManager.getInstanceMetadata(instanceId.get()));
		}
		amazonInstanceManager.deploy(node, nodeConfiguration);
		
		switch(nodeConfiguration.getType()) {
			case NODE: {
				nodes.add(node);
				break;
			}
			case DRIVER:  {
				drivers.add(node);
				break;
			}
		}
		
		return node;
	}
	
	public void close() {
		if(amazonInstanceManager != null) {
			amazonInstanceManager.close();
		}
	}
	
	private AmazonNode createNewInstance(NodeConfiguration config, Map<String, String> tags) {
		NodeMetadata metadata = amazonInstanceManager.createInstance(tags);
		return new AmazonNode(config.getType(), metadata);
	}
}
