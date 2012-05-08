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
package com.github.nethad.clustermeister.provisioning.torque.commands;

import com.github.nethad.clustermeister.api.Node;
import com.github.nethad.clustermeister.api.NodeInformation;
import com.github.nethad.clustermeister.api.impl.NodeInformationImpl;
import com.github.nethad.clustermeister.provisioning.Command;
import com.github.nethad.clustermeister.provisioning.CommandLineHandle;
import com.github.nethad.clustermeister.provisioning.CommandRegistry;
import com.github.nethad.clustermeister.provisioning.rmi.RmiServerForApi;
import com.github.nethad.clustermeister.provisioning.torque.ConfigurationForTesting;
import com.github.nethad.clustermeister.provisioning.torque.TorqueNode;
import com.github.nethad.clustermeister.provisioning.torque.TorqueNodeConfiguration;
import com.github.nethad.clustermeister.provisioning.torque.TorqueNodeManager;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.StringTokenizer;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import org.jppf.management.JPPFSystemInformation;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.eq;


/**
 *
 * @author thomas
 */
public class TorqueCommandLineEvaluationTest {
    private TorqueCommandLineEvaluation commandLineEvaluation;
    private CommandLineHandle commandLineHandle;
    private JPPFSystemInformation jppfSystemInformation;
    private RmiServerForApi rmiServerForApi;
    private TorqueNodeManager torqueNodeManager;
    
    @Before
    public void setup() {
        torqueNodeManager = mock(TorqueNodeManager.class);
        when(torqueNodeManager.getConfiguration())
                .thenReturn(new ConfigurationForTesting(new HashMap<String, Object>()));
        commandLineHandle = mock(CommandLineHandle.class);
        when(commandLineHandle.getCommandRegistry()).thenReturn(new CommandRegistry() {

            @Override
            public void registerCommand(Command command) {
                
            }

            @Override
            public void unregisterCommand(Command command) {
                
            }

            @Override
            public Command getCommand(String commandName) {
                return null;
            }
        });
        rmiServerForApi = mock(RmiServerForApi.class);
        commandLineEvaluation = new TorqueCommandLineEvaluation(torqueNodeManager, commandLineHandle, rmiServerForApi);
    }
    
    @Test
    public void addNodes() {
        SettableFuture<TorqueNode> settableFuture = SettableFuture.create();
        settableFuture.set(new TorqueNode("", "", "", 11111, 11198));
        doReturn(settableFuture).when(torqueNodeManager).addNode(any(TorqueNodeConfiguration.class));
        
        commandLineEvaluation.handleCommand("addnodes", new StringTokenizer("1 1"));
        
        verify(torqueNodeManager).addNode(argThat(new MatchesTorqueNodeConfiguration(null, 1)));
    }
    
    @Test
    public void state() {
        Collection<NodeInformation> allNodes = new LinkedList<NodeInformation>();
        allNodes.add(new NodeInformationImpl("1", createSystemInformation()));
        allNodes.add(new NodeInformationImpl("2", createSystemInformation()));
        when(rmiServerForApi.getAllNodes()).thenReturn(allNodes);
        
        commandLineEvaluation.state(null);
        
        verify(commandLineHandle).print(contains("running nodes: "), eq(2));
        verify(commandLineHandle).print(contains("node %s:"), eq("1"), any(String.class));
        verify(commandLineHandle).print(contains("node %s:"), eq("2"), any(String.class));
    }
    
    @Test
    public void shutdown() {
        commandLineEvaluation.shutdown(null);
        
        verify(torqueNodeManager).removeAllNodes();
        verify(torqueNodeManager).shutdown();
    }
    
    private JPPFSystemInformation createSystemInformation() {
        if (jppfSystemInformation == null) {
            jppfSystemInformation = new JPPFSystemInformation("");
            jppfSystemInformation.populate();
        }
        return jppfSystemInformation;
    }
    
    class MatchesTorqueNodeConfiguration extends ArgumentMatcher<TorqueNodeConfiguration> {
        private final String driverAddress;
        private final int numberOfCpus;

        public MatchesTorqueNodeConfiguration(String driverAddress, int numberOfCpus) {
            this.driverAddress = driverAddress;
            this.numberOfCpus = numberOfCpus;
        }
        
        @Override
        public boolean matches(Object argument) {
            if (!(argument instanceof TorqueNodeConfiguration)) {
                return false;
            }
            TorqueNodeConfiguration tnc = (TorqueNodeConfiguration)argument;
            if (tnc.getDriverAddress() == null) {
                if (driverAddress == null) { // both are null
                    return true;
                } else {
                    return false;
                }
            } 
            return tnc.getDriverAddress().equals(driverAddress) 
                    && tnc.getNumberOfCpus() == numberOfCpus;
        }
        
    }
}
