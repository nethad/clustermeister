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
package com.github.nethad.clustermeister.provisioning.jppf;

import com.github.nethad.clustermeister.provisioning.jppf.managementtasks.JPPFConfigReaderTask;
import com.github.nethad.clustermeister.provisioning.jppf.managementtasks.JPPFShutdownTask;
import com.github.nethad.clustermeister.provisioning.utils.NodeManagementConnector;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jppf.JPPFException;
import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.client.JPPFResultCollector;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.node.policy.Equal;
import org.jppf.server.protocol.JPPFTask;

/**
 *
 * @author daniel
 */
public class JPPFManagementByJobsClient {
	private final JPPFClient jPPFClient;

	/**
	 * Default constructor: intended to be used only by JPPFConfiguratedComponentFactory.
	 */
	JPPFManagementByJobsClient() {
		jPPFClient = new JPPFClient();
	}
	
	public Properties getJPPFConfig(String host, int port) {
		JPPFJob job = new JPPFJob();
		try {
			job.addTask(new JPPFConfigReaderTask(), host, port);
		} catch (JPPFException ex) {
			throw new RuntimeException(ex);
		}
		job.getSLA().setMaxNodes(1);
		job.setBlocking(true);
		job.getSLA().setSuspended(false);
		List<JPPFTask> results = Collections.EMPTY_LIST;
		try {
			results = jPPFClient.submit(job);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		
		JPPFTask task = Iterables.getOnlyElement(results);
		return (Properties) task.getResult();
	}
	
	public void shutdownAllNodes(String driverHost, int managementPort) {
				try {
					
//			ShutdownRunner runner = new ShutdownRunner();
			JMXDriverConnectionWrapper wrapper = NodeManagementConnector.openDriverConnection(driverHost, managementPort);
			List<String> sockets = new ArrayList<String>();
			for (JPPFManagementInfo nodeInfo : wrapper.nodesInformation()) {
//				System.out.println("M Port: " + nodeInfo.getPort());
				sockets.add(nodeInfo.getHost() + ":" + nodeInfo.getPort());
			}
			
			JPPFJob job = new JPPFJob();
			System.out.println("UUID: " + job.getUuid());
			job.setName("Shut down job");
			String[] split = sockets.get(0).split(":");
			int port = Integer.parseInt(split[1]);
			System.out.println("chose port: " + port);
			System.out.println("list size: " + sockets.size());
			job.addTask(new JPPFShutdownTask(), sockets, port);
			job.getSLA().setMaxNodes(1);
			job.getSLA().setExecutionPolicy(new Equal("jppf.management.port", port));
			
			job.setBlocking(false);
			JPPFResultCollector collector = new JPPFResultCollector(job);
			job.setResultListener(collector);
			jPPFClient.submit(job);
			
			System.out.println("waiting...");
			while(wrapper.nodesInformation().size() > 0) {
				Thread.sleep(1000);
			}
			System.out.println("canceling job");
			wrapper.cancelJob(job.getUuid());
			wrapper.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (jPPFClient != null) {
				jPPFClient.close();
			}
		}
	}
	
	public void shutdownDriver(String driverHost, int managementPort) {
		try {
			JMXDriverConnectionWrapper wrapper = NodeManagementConnector.openDriverConnection(driverHost, managementPort);
			wrapper.restartShutdown(0L, -1L);
		} catch (TimeoutException ex) {
			Logger.getLogger(JPPFManagementByJobsClient.class.getName()).log(Level.SEVERE, null, ex);
		} catch (Exception ex) {
			Logger.getLogger(JPPFManagementByJobsClient.class.getName()).log(Level.SEVERE, null, ex);
		} 
	}
	
	public void close() {
		if(jPPFClient != null) {
			jPPFClient.close();
		}
	}

}
