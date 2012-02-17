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

import com.github.nethad.clustermeister.api.impl.FileConfiguration;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.domain.LoginCredentials;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author daniel
 */
@Ignore("Depends on local configuration.")
public class AmazonEC2JPPFDeployerTest {
	static final String AMAZON_SETTINGS = "/home/daniel/clustermeister-amazonapi.properties";
	static final String driverNodeID = "eu-west-1/i-28b4f361";
	static final String nodeNodeID = "eu-west-1/i-14c98e5d";
	static final String privateKeyFile = "/home/daniel/Desktop/EC2/EC2_keypair.pem";
	static final String userName = "ec2-user";
	
	static AmazonInstanceManager nodeManager;
	static AmazonEC2JPPFDriverDeployer driverDeployer;
	static AmazonEC2JPPFNodeDeployer nodeDeployer1;
	static AmazonEC2JPPFNodeDeployer nodeDeployer2;
	static NodeMetadata metadata;
	static LoginCredentials loginCredentials;
	
	private final static Logger logger = 
			LoggerFactory.getLogger(AmazonEC2JPPFDeployerTest.class);
	
	@BeforeClass
	public static void setUpClass() throws Exception {
		FileConfiguration config = 
				new FileConfiguration(AMAZON_SETTINGS);
		nodeManager = new AmazonInstanceManager(config);
		nodeManager.init();
		logger.info("Resuming node {}", driverNodeID);
		metadata = nodeManager.resumeNode(driverNodeID);
		logger.info("Node {} resumed at {}", driverNodeID, 
				metadata.getPublicAddresses().iterator().next());
		
		loginCredentials = new LoginCredentials(userName, null, getPrivateKey(), true);
		
		driverDeployer = new AmazonEC2JPPFDriverDeployer(nodeManager.getContext(), 
				metadata, loginCredentials);
		String driverPublicIP = metadata.getPublicAddresses().iterator().next();
		String driverPrivateIP = metadata.getPrivateAddresses().iterator().next();
		
		nodeDeployer1 = new AmazonEC2JPPFNodeDeployer(nodeManager.getContext(), 
				metadata, loginCredentials, driverPrivateIP);
		
		logger.info("Resuming node {}", nodeNodeID);
		metadata = nodeManager.resumeNode(nodeNodeID);
		logger.info("Node {} resumed at {}", nodeNodeID, 
				metadata.getPublicAddresses().iterator().next());
		nodeDeployer2 = new AmazonEC2JPPFNodeDeployer(nodeManager.getContext(), 
				metadata, loginCredentials, driverPublicIP);
		
	}

	

	@AfterClass
	public static void tearDownClass() throws Exception {
//		nodeManager.suspendNode(nodeNodeID);
//		nodeManager.suspendNode(driverNodeID);
		nodeManager.close();
	}
	
	@Test
	public void testSomeMethod() {
		driverDeployer.run();
		logger.info("Driver running");
		nodeDeployer1.run();
		logger.info("Node1 running");
		nodeDeployer2.run();
		logger.info("Node2 running");
	}
	
	static String getPrivateKey() throws FileNotFoundException, IOException {
		BufferedReader reader = new BufferedReader(new FileReader(privateKeyFile));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			sb.append(line);
			sb.append("\n");
		}
		reader.close();
		return sb.toString().trim();
	}
}
