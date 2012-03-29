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
package com.github.nethad.clustermeister.provisioning.rmi;

import com.github.nethad.clustermeister.api.NodeInformation;
import com.github.nethad.clustermeister.api.rmi.IRmiServerForApi;
import com.github.nethad.clustermeister.driver.rmi.IRmiServerForDriver;
import java.rmi.RemoteException;
import java.util.Collection;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.management.JPPFSystemInformation;
import org.junit.AfterClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;

import static org.hamcrest.CoreMatchers.*;

/**
 *
 * @author thomas
 */
public class RmiIntegrationTest {

    private static RmiInfrastructure rmiInfrastructure;
    private static IRmiServerForApi rmiServerForApi;
    private static IRmiServerForDriver rmiServerForDriver;

    @BeforeClass
    public static void setupClass() throws Exception {
        rmiInfrastructure = new RmiInfrastructure();
        rmiInfrastructure.initialize();
        rmiServerForApi = rmiInfrastructure.getRmiServerForApi();
        rmiServerForDriver = rmiInfrastructure.getRmiServerForDriver();
    }

    @Test
    public void driverAddsNodeAndApiGetsAllNodes() throws Exception {
        Collection<NodeInformation> allNodes = rmiServerForApi.getAllNodes();
        assertThat(allNodes.size(), is(0));
        final JPPFManagementInfo managementInfo = getManagementInfoForNodeId("node1");
        rmiServerForDriver.onNodeConnected(managementInfo, managementInfo.getSystemInfo());

        allNodes = rmiServerForApi.getAllNodes();
        assertThat(allNodes.size(), is(1));
    }

    @Test
    public void driverRemovesNodeAndApiGetsAllNodes() throws Exception {
        final JPPFManagementInfo managementInfo = getManagementInfoForNodeId("node1");
        rmiServerForDriver.onNodeConnected(managementInfo, managementInfo.getSystemInfo());
        Collection<NodeInformation> allNodes = rmiServerForApi.getAllNodes();
        assertThat(allNodes.size(), is(1));
        
        rmiServerForDriver.onNodeDisconnected(managementInfo);
        allNodes = rmiServerForApi.getAllNodes();
        assertThat(allNodes.size(), is(0));
    }

    private JPPFManagementInfo getManagementInfoForNodeId(String id) {
        JPPFSystemInformation jppfSystemInformation = new JPPFSystemInformation(id);
        JPPFManagementInfo managementInfo = new JPPFManagementInfo("", 0, id);
        managementInfo.setSystemInfo(jppfSystemInformation);
        return managementInfo;
    }
}
