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

import com.github.nethad.clustermeister.api.JPPFConstants;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.management.JMXNodeConnectionWrapper;
import org.jppf.management.JPPFManagementInfo;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author daniel
 */
@Ignore("Depends on local configuration.")
public class AmazonJMXTest {
	
	private final static Logger logger = 
			LoggerFactory.getLogger(AmazonJMXTest.class);
	
	@Test
	public void testSomeMethod() throws InterruptedException, Exception {
		JMXNodeConnectionWrapper wrapper = new JMXNodeConnectionWrapper("localhost", 11199);
		System.out.println("Attempting to connect to Node...");
		wrapper.connect();
		while (!wrapper.isConnected()) {
			Thread.sleep(1000);
		}
		System.out.println("Connected!");
		System.out.println(wrapper.systemInformation().getUuid().asString());
		
		JMXDriverConnectionWrapper wrapper2 = new JMXDriverConnectionWrapper("176.34.218.168", 11198);
		System.out.println("Attempting to connect to Management...");
		wrapper2.connect();
		while (!wrapper2.isConnected()) {
			Thread.sleep(1000);
		}

		System.out.println("Connected!");
		System.out.println(wrapper2.systemInformation().getUuid().getProperty(JPPFConstants.UUID));
		for (JPPFManagementInfo nodeInfo : wrapper2.nodesInformation()) {
			System.out.println("Node Management: " + nodeInfo.toString());
			System.out.println("ID-: " + nodeInfo.getId());
		}
	}
}
