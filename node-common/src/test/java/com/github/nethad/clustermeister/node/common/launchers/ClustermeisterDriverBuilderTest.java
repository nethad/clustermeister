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

import com.github.nethad.clustermeister.node.common.ClientConnectionAwaiter;
import com.github.nethad.clustermeister.node.common.builders.JPPFDriverBuilder;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author daniel
 */
public class ClustermeisterDriverBuilderTest {
    
    public ClustermeisterDriverBuilderTest() {
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

    @Test
    public void testDriverBuilder() throws InterruptedException {
        int managementPort = 12000;
        int serverPort = 11112;
        DriverBuilderThread driverBuilderThread = new DriverBuilderThread(serverPort, managementPort);
        driverBuilderThread.start(); 
        driverBuilderThread.join();

        new ClientConnectionAwaiter("localhost", serverPort).await();
        assertTrue(true);
    }
    
    private class DriverBuilderThread extends Thread {

        private final int serverPort;
        private final int managementPort;
        ClustermeisterJPPFServer server;

        private DriverBuilderThread(int serverPort, int managementPort) {
            this.serverPort = serverPort;
            this.managementPort = managementPort;
        }
        
        @Override
        public void run() {
            JPPFDriverBuilder serverBuilder = new JPPFDriverBuilder();
            serverBuilder.setProperty("jppf.management.host", "localhost");
            serverBuilder.setProperty("jppf.management.port", String.valueOf(this.managementPort));
            serverBuilder.setProperty("jppf.server.host", "localhost");
            serverBuilder.setProperty("jppf.server.port", String.valueOf(this.serverPort));
            serverBuilder.setProperty("jppf.discovery.enabled", "false");
            serverBuilder.setProperty("jppf.management.enabled", "true");
        
            server = serverBuilder.build();
        }
    }
}
