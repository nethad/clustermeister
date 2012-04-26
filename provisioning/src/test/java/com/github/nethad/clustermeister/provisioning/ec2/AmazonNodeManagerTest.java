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
import com.github.nethad.clustermeister.api.NodeCapabilities;
import com.github.nethad.clustermeister.api.NodeType;
import com.github.nethad.clustermeister.api.impl.AmazonConfiguredKeyPairCredentials;
import com.github.nethad.clustermeister.api.impl.FileConfiguration;
import com.github.nethad.clustermeister.api.impl.KeyPairCredentials;
import com.google.common.base.Optional;
import java.io.File;
import java.util.Collection;
import java.util.concurrent.Future;
import org.apache.commons.configuration.Configuration;
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
    public static final String EU_WEST_1C = "eu-west-1c";
    public static final String KEYPAIR = "EC2_keypair";
    public static final String PRIVATE_KEY = "/home/daniel/EC2/EC2_keypair.pem";
    public static final String OTHER_PRIVATE_KEY = "/home/daniel/EC2/otherkey_rsa";
    public static final String OTHER_PUBLIC_KEY = "/home/daniel/EC2/otherkey_rsa.pub";

    private final static Logger logger =
            LoggerFactory.getLogger(AmazonNodeManagerTest.class);

    @Test
    public void testSomeMethod() throws InterruptedException, Exception {
        final String settings = "/home/daniel/clustermeister-amazonapi.properties";

        Configuration config = new FileConfiguration(settings);

        AmazonNodeManager nodeManager = new AmazonNodeManager(config);

        Optional<String> instanceId = Optional.of("eu-west-1/i-b07f47f9");
//        Optional<String> absentInstanceId = Optional.absent();
//        AmazonNodeConfiguration dc = new AmazonNodeConfiguration();
//        dc.setRegion(EU_WEST_1C);
//        dc.setNodeType(NodeType.DRIVER);
//        dc.setCredentials(getCredentials());
//        dc.setNodeCapabilities(getCapabilities());
//        final Future<? extends Node> d = nodeManager.addNode(dc, instanceId);
        
//        Optional<String> driverInstanceId = Optional.of(((AmazonNode)d.get()).getInstanceId());
        AmazonNodeConfiguration nc = new AmazonNodeConfiguration();
        nc.setRegion(EU_WEST_1C);
        nc.setNodeType(NodeType.NODE);
//        nc.setDriverAddress(Iterables.getFirst(d.get().getPrivateAddresses(), null));
        nc.setDriverAddress("localhost");
        nc.setCredentials(getCredentials());
        nc.setNodeCapabilities(getCapabilities());
        final Future<? extends Node> n = nodeManager.addNode(nc, instanceId);
        
        AmazonNodeConfiguration nc2 = new AmazonNodeConfiguration();
        nc2.setRegion(EU_WEST_1C);
        nc2.setNodeType(NodeType.NODE);
//        nc2.setDriverAddress(Iterables.getFirst(d.get().getPrivateAddresses(), null));
        nc.setDriverAddress("localhost");
        nc2.setCredentials(getCredentials());
        nc2.setNodeCapabilities(getCapabilities());
        final Future<? extends Node> n2 = nodeManager.addNode(nc2, instanceId);
        


        //wait for all nodes to be online
//        Node jppfDriver = d.get();
        Node jppfNode = n.get();
        Node jppfNode2 = n2.get();

        Collection<? extends Node> nodes = nodeManager.getNodes();
        for (Node node : nodes) {
            System.out.println(node);
        }
        
        System.out.println("waiting...");
        Thread.sleep(20000);

//        Future<Void> ns = nodeManager.removeNode((AmazonNode) jppfNode, AmazonInstanceShutdownMethod.NO_SHUTDOWN);
//        Future<Void> ns2 = nodeManager.removeNode((AmazonNode) jppfNode2, AmazonInstanceShutdownMethod.NO_SHUTDOWN);
//
//        ns.get();
//        ns2.get();
        
//        Future<Void> ds = nodeManager.removeNode((AmazonNode) jppfDriver, AmazonInstanceShutdownMethod.NO_SHUTDOWN);
//        ds.get();
        
        nodeManager.close();
    }
    
    private static NodeCapabilities getCapabilities() {
        return new NodeCapabilities() {

            @Override
            public int getNumberOfProcessors() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public int getNumberOfProcessingThreads() {
                return 1;
            }

            @Override
            public String getJppfConfig() {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        };
    }
    
    private static KeyPairCredentials getCredentials() {
        return new AmazonConfiguredKeyPairCredentials(new File(PRIVATE_KEY), KEYPAIR);
    }
    
    private static KeyPairCredentials getOtherCredentials() {
        return new KeyPairCredentials("ec2-user", new File(OTHER_PRIVATE_KEY), 
                new File(OTHER_PUBLIC_KEY));
    }
}
