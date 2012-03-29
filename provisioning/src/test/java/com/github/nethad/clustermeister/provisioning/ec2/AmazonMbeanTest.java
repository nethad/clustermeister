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

import com.github.nethad.clustermeister.provisioning.jppf.JPPFConfiguratedComponentFactory;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFManagementByJobsClient;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author daniel
 */
@Ignore("Depends on local configuration.")
public class AmazonMbeanTest {
	
	private final static Logger logger = 
			LoggerFactory.getLogger(AmazonMbeanTest.class);
	
	@Test
	public void testSomeMethod() throws InterruptedException, Exception {
            
            JPPFManagementByJobsClient client = JPPFConfiguratedComponentFactory.getInstance().
                    createManagementByJobsClient("localhost", 11111);
            client.shutdownNode("057E551CF16A6AEF5DFBCC52F8E415D8");
            client.shutdownNode("057E551CF16A6AEF5DFBCC52F8E415D8");
            
	}
}
