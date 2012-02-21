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

import com.github.nethad.clustermeister.api.Node;
import com.github.nethad.clustermeister.api.NodeConfiguration;
import com.github.nethad.clustermeister.api.NodeType;
import com.github.nethad.clustermeister.api.impl.FileConfiguration;
import java.util.List;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author daniel
 */
@Ignore("Depends on local configuration.")
public class AmazonNodeManagerTest {
	
	private final static Logger logger = 
			LoggerFactory.getLogger(AmazonNodeManagerTest.class);
	
	@Test
	public void testSomeMethod() throws InterruptedException {
		String settings = "/home/daniel/clustermeister-amazonapi.properties";
		String privateKeyFile = "/home/daniel/Desktop/EC2/EC2_keypair.pem";
		String userName = "ec2-user";
		
		FileConfiguration config = new FileConfiguration(settings);
		
		AmazonNodeManager nodeManager = new AmazonNodeManager(config);
		
		Node d = nodeManager.addNode(new NodeConfiguration() {
			@Override
			public NodeType getType() {
				return NodeType.DRIVER;
			}
		});
		Node n = nodeManager.addNode(new NodeConfiguration() {
			@Override
			public NodeType getType() {
				return NodeType.NODE;
			}
		});
		
		List<? extends Node> nodes = nodeManager.getNodes();
		for (Node node : nodes) {
			System.out.println(node + " Public Addresses: " + node.getPublicAddresses()
					+ " Private Addresses: " + node.getPrivateAddresses());
		}
		
		nodeManager.close();
	}
}
