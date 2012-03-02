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

import com.github.nethad.clustermeister.api.Node;
import com.github.nethad.clustermeister.api.NodeType;
import com.github.nethad.clustermeister.api.impl.FileConfiguration;
import com.google.common.base.Optional;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Collection;
import java.util.concurrent.Future;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author daniel
 */
@Ignore("Depends on local configuration.")
public class AmazonNodeManagerTest {

    private final static Logger logger =
            LoggerFactory.getLogger(AmazonNodeManagerTest.class);

    @Test
    public void testSomeMethod() throws InterruptedException, Exception {
        final String settings = "/home/daniel/clustermeister-amazonapi.properties";
        final String privateKeyFile = "/home/daniel/Desktop/EC2/EC2_keypair.pem";
        final String userName = "ec2-user";

        FileConfiguration config = new FileConfiguration(settings);

        AmazonNodeManager nodeManager = new AmazonNodeManager(config);

//        Optional<String> instanceId = Optional.of("eu-west-1/i-62c5e12b");
        Optional<String> absentInstanceId = Optional.absent();
        final Future<? extends Node> d = nodeManager.addNode(new AmazonNodeConfiguration() {

            @Override
            public NodeType getType() {
                return NodeType.DRIVER;
            }

            @Override
            public String getUserName() {
                return userName;
            }

            @Override
            public String getPrivateKey() {
                return AmazonNodeManagerTest.getPrivateKey(privateKeyFile);
            }

            @Override
            public int getManagementPort() {
                return 11198;
            }
        }, absentInstanceId);
        
        Optional<String> driverInstanceId = Optional.of(((AmazonNode)d.get()).getInstanceId());

        final Future<? extends Node> n = nodeManager.addNode(new AmazonNodeConfiguration() {

            @Override
            public NodeType getType() {
                return NodeType.NODE;
            }

            @Override
            public String getUserName() {
                return userName;
            }

            @Override
            public String getPrivateKey() {
                return AmazonNodeManagerTest.getPrivateKey(privateKeyFile);
            }

            @Override
            public String getDriverAddress() {
                try {
                    return d.get().getPrivateAddresses().iterator().next();
                } catch (Throwable ex) {
                    logger.error("Failed to get Driver IP.", ex);
                    return null;
                }
            }
        }, driverInstanceId);

        final Future<? extends Node> n2 = nodeManager.addNode(new AmazonNodeConfiguration() {

            @Override
            public NodeType getType() {
                return NodeType.NODE;
            }

            @Override
            public String getUserName() {
                return userName;
            }

            @Override
            public String getPrivateKey() {
                return AmazonNodeManagerTest.getPrivateKey(privateKeyFile);
            }

            @Override
            public String getDriverAddress() {
                try {
                    return d.get().getPrivateAddresses().iterator().next();
                } catch (Throwable ex) {
                    logger.error("Failed to get Driver IP.", ex);
                    return null;
                }
            }

        }, driverInstanceId);

        //wait for all nodes to be online
        Node jppfDriver = d.get();
        Node jppfNode = n.get();
        Node jppfNode2 = n2.get();

        Collection<? extends Node> nodes = nodeManager.getNodes();
        for (Node node : nodes) {
            System.out.println(node);
        }
        
        System.out.println("waiting...");
        Thread.sleep(20000);

        Future<Void> ns2 = nodeManager.removeNode((AmazonNode) jppfNode2, AmazonInstanceShutdownMethod.NO_SHUTDOWN);
        Future<Void> ns = nodeManager.removeNode((AmazonNode) jppfNode, AmazonInstanceShutdownMethod.NO_SHUTDOWN);
        Future<Void> ds = nodeManager.removeNode((AmazonNode) jppfDriver, AmazonInstanceShutdownMethod.TERMINATE);

        //wait for them to shut down
        ds.get();
        ns.get();
        ns2.get();

        nodeManager.close();
    }

    synchronized public static String getPrivateKey(String path) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            reader.close();
            return sb.toString().trim();
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}
