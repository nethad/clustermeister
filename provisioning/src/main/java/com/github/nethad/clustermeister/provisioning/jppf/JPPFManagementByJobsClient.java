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
import java.util.Collections;
import java.util.List;
import org.jppf.JPPFException;
import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
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
	
	public void  test() {
		JPPFJob job = new JPPFJob();
		try {
			job.addTask(new JPPFConfigReaderTask(), "localhost", 11198);
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
		
		for (JPPFTask jPPFTask : results) {
			System.out.println(jPPFTask.getResult());
		}
	}
	
	public void close() {
		if(jPPFClient != null) {
			jPPFClient.close();
		}
	}
}
