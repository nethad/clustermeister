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
package com.github.nethad.clustermeister.node.common.launchers;

import com.github.nethad.clustermeister.node.common.builders.JPPFDriverBuilder;
import java.io.IOException;
import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFClientConnectionStatus;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author daniel
 */
public class ClustermeisterDriverLauncherTest {
    
    public ClustermeisterDriverLauncherTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of main method, of class ClustermeisterDriverLauncher.
     */
    @Test
    public void testMain() throws InterruptedException, ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
        JPPFDriverBuilder serverBuilder = new JPPFDriverBuilder();
        serverBuilder.setProperty("jppf.management.port", "12000");
        serverBuilder.setProperty("jppf.server.port", "11111");
        serverBuilder.setProperty("jppf.discovery.enabled", "false");
        
        TestClientBuilder clientBuilder = new TestClientBuilder();
        clientBuilder.setProperty("jppf.drivers", "testDriver");
        clientBuilder.setProperty("testDriver.jppf.server.host", "localhost");
        clientBuilder.setProperty("testDriver.jppf.server.port", "11111");
        clientBuilder.setProperty("jppf.discovery.enabled", "false");
        
        TestClientBuilder clientBuilder2 = new TestClientBuilder();
        clientBuilder2.setProperty("jppf.drivers", "testDriver2");
        clientBuilder2.setProperty("testDriver2.jppf.server.host", "localhost");
        clientBuilder2.setProperty("testDriver2.jppf.server.port", "11112");
        clientBuilder2.setProperty("jppf.discovery.enabled", "false");
        
        ClustermeisterJPPFServer server = serverBuilder.build();
        
        JPPFClient client = clientBuilder.build();
        JPPFClient client2 = clientBuilder2.build();
        
        Thread.sleep(3000);
        
        assertEquals(JPPFClientConnectionStatus.ACTIVE, client.getClientConnection().getStatus());
        assertEquals(JPPFClientConnectionStatus.DISCONNECTED, client2.getClientConnection().getStatus());
        
    }
}
