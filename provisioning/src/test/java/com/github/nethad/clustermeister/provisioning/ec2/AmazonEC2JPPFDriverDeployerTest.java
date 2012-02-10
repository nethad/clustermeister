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

import com.github.nethad.clustermeister.provisioning.FileConfiguration;
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
public class AmazonEC2JPPFDriverDeployerTest {
	static final String AMAZON_SETTINGS = "/home/daniel/clustermeister-amazonapi.properties";
	static final String nodeID = "eu-west-1/i-28b4f361";
	static final String privateKeyFile = "/home/daniel/Desktop/EC2/EC2_keypair.pem";
	static final String userName = "ec2-user";
	
	static AmazonAPIManageNodes nodeManager;
	static AmazonEC2JPPFDriverDeployer driverDeployer;
	static NodeMetadata metadata;
	static LoginCredentials loginCredentials;
	
	private final static Logger logger = 
			LoggerFactory.getLogger(AmazonEC2JPPFDriverDeployerTest.class);
	
	@BeforeClass
	public static void setUpClass() throws Exception {
		FileConfiguration config = 
				new FileConfiguration(AMAZON_SETTINGS);
		nodeManager = new AmazonAPIManageNodes(config);
		nodeManager.init();
		logger.info("Resuming node {}", nodeID);
		metadata = nodeManager.resumeNode(nodeID);
		logger.info("Node {} resumed at {}", nodeID, metadata.getPublicAddresses().iterator().next());
		
		loginCredentials = new LoginCredentials(userName, null, getPrivateKey(), true);
		
		driverDeployer = new AmazonEC2JPPFDriverDeployer(nodeManager.getContext(), 
				metadata, loginCredentials);
	}

	

	@AfterClass
	public static void tearDownClass() throws Exception {
//		nodeManager.suspendNode(nodeID);
	}
	
	@Ignore("Depends on local configuration.")
	@Test
	public void testSomeMethod() {
		driverDeployer.run();
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
