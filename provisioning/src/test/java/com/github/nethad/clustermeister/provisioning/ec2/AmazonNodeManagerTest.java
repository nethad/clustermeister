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
import com.github.nethad.clustermeister.api.impl.PrivateKeyCredentials;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import java.io.File;
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

        FileConfiguration config = new FileConfiguration(settings);

        AmazonNodeManager nodeManager = new AmazonNodeManager(config);

//        Optional<String> instanceId = Optional.of("eu-west-1/i-68cdee21");
        Optional<String> absentInstanceId = Optional.absent();
        AmazonNodeConfiguration dc = new AmazonNodeConfiguration();
        dc.setNodeType(NodeType.DRIVER);
        dc.setCredentials(new PrivateKeyCredentials("ec2-user", new File(privateKeyFile)));
        final Future<? extends Node> d = nodeManager.addNode(dc, absentInstanceId);
        
        Optional<String> driverInstanceId = Optional.of(((AmazonNode)d.get()).getInstanceId());
        AmazonNodeConfiguration nc = new AmazonNodeConfiguration();
        nc.setNodeType(NodeType.NODE);
        nc.setDriverAddress(Iterables.getFirst(d.get().getPrivateAddresses(), null));
        nc.setCredentials(new PrivateKeyCredentials("ec2-user", new File(privateKeyFile)));
        final Future<? extends Node> n = nodeManager.addNode(nc, driverInstanceId);
        
        AmazonNodeConfiguration nc2 = new AmazonNodeConfiguration();
        nc2.setNodeType(NodeType.NODE);
        nc2.setDriverAddress(Iterables.getFirst(d.get().getPrivateAddresses(), null));
        nc2.setCredentials(new PrivateKeyCredentials("ec2-user", new File(privateKeyFile)));
        final Future<? extends Node> n2 = nodeManager.addNode(nc2, absentInstanceId);
        


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

        Future<Void> ns2 = nodeManager.removeNode((AmazonNode) jppfNode2, AmazonInstanceShutdownMethod.TERMINATE);
        Future<Void> ns = nodeManager.removeNode((AmazonNode) jppfNode, AmazonInstanceShutdownMethod.NO_SHUTDOWN);
        ns.get();
        Future<Void> ds = nodeManager.removeNode((AmazonNode) jppfDriver, AmazonInstanceShutdownMethod.TERMINATE);
        ds.get();
        ns2.get();

        //wait for them to shut down

        nodeManager.close();
    }
}
