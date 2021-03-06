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

import java.util.Properties;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author daniel
 */
@Ignore("Depends on local configuration.")
public class JPPFManagementByJobsClientTest {

    @Test
    public void testSomeMethod() throws InterruptedException {

        ManagementByJobsClientBuilder builder = 
                new ManagementByJobsClientBuilder("localhost", 11111);
        JPPFManagementByJobsClient client = builder.build();

        Properties props = client.getJPPFConfig("localhost", 11198);

        System.out.println(props);
        
        client.close();
    }
}
