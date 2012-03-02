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
package com.github.nethad.clustermeister.provisioning.jppf.managementtasks;

import com.github.nethad.clustermeister.provisioning.utils.NodeManagementConnector;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jppf.management.JMXConnectionWrapper;
import org.jppf.management.JMXNodeConnectionWrapper;
import org.jppf.server.protocol.JPPFRunnable;

/**
 *
 * @author thomas
 */
public class JPPFShutdownTask implements Serializable {

	@JPPFRunnable
	public void shutdownNodes(List<String> sockets, int myPort) throws TimeoutException, Exception {
		System.out.println("running");

		System.out.println("list size: " + sockets.size());
		List<Thread> runningThreads = startShutdownTaskThreads(sockets, myPort);
		
		waitForAllThreadsToComplete(runningThreads);
		shutdownThisRunningNode(myPort);
	}

	private void shutdownThisRunningNode(int myPort) throws Exception, TimeoutException {
		System.out.println("shutting down myself");
		JMXNodeConnectionWrapper wrapper;
		wrapper = NodeManagementConnector.connectToNodeManagement_node(new JMXNodeConnectionWrapper("localhost", myPort));
		wrapper.shutdown();
	}

	private void waitForAllThreadsToComplete(List<Thread> runningThreads) {
		for (Thread thread : runningThreads) {
			try {
				thread.join();
			} catch (InterruptedException ex) {
				continue;
			}
		}
	}

	private List<Thread> startShutdownTaskThreads(List<String> sockets, int myPort) throws NumberFormatException {
		List<Thread> runningThreads = new ArrayList<Thread>();
		for (String socket : sockets) {
			String[] split = socket.split(":");
			String ip = split[0];
			int port = Integer.parseInt(split[1]);

			System.out.println("current socket: " + ip + ":" + port);
			if (port != myPort) {
				Thread t = new ShutdownTaskThread(ip, port);
				runningThreads.add(t);
				t.start();
			}
		}
		return runningThreads;
	}

	private class ShutdownTaskThread extends Thread {

		private final String ip;
		private final int port;

		public ShutdownTaskThread(String ip, int port) {
			this.ip = ip;
			this.port = port;
		}

		@Override
		public void run() {
			try {
				System.out.println("Shutting down " + ip + ":" + port);
				JMXNodeConnectionWrapper wrapper = NodeManagementConnector.connectToNodeManagement_node(
						new JMXNodeConnectionWrapper(ip, port));
				wrapper.shutdown();
				wrapper.close();
			} catch (TimeoutException ex) {
				Logger.getLogger(JPPFShutdownTask.class.getName()).log(Level.SEVERE, null, ex);
			} catch (Exception ex) {
				Logger.getLogger(JPPFShutdownTask.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}
}
