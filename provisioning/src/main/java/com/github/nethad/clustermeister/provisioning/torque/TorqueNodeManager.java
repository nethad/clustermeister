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
package com.github.nethad.clustermeister.provisioning.torque;

import com.github.nethad.clustermeister.provisioning.torque.commands.TorqueCommandLineEvaluation;
import com.github.nethad.clustermeister.api.Node;
import com.github.nethad.clustermeister.api.NodeConfiguration;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFConfiguratedComponentFactory;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFManagementByJobsClient;
import com.github.nethad.clustermeister.api.utils.NodeManagementConnector;
import com.github.nethad.clustermeister.provisioning.CommandLineEvaluation;
import com.github.nethad.clustermeister.provisioning.CommandLineHandle;
import com.github.nethad.clustermeister.provisioning.injection.SSHModule;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFLocalDriver;
import com.github.nethad.clustermeister.provisioning.rmi.RmiInfrastructure;
import com.github.nethad.clustermeister.provisioning.rmi.RmiServerForApi;
import com.github.nethad.clustermeister.provisioning.utils.SSHClient;
import com.github.nethad.clustermeister.provisioning.utils.SSHClientException;
import com.github.nethad.clustermeister.provisioning.utils.SSHClientImpl;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.Monitor;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.apache.commons.configuration.Configuration;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.management.JMXNodeConnectionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: - functionality like add(deploy)/remove JPPD nodes. - starting/deploying drivers - this is not yet to be
 * directly used by the user but by a higher layer of abstraction (to be defined) - this will likely need to implement a
 * yet to be defined interface together with AmazonNodeManager
 *
 * @author daniel
 */
public class TorqueNodeManager implements TorqueNodeManagement {

	public static final int THREAD_POOL_SIZE = 2;
	private Logger logger = LoggerFactory.getLogger(TorqueNodeManager.class);
	
	private final Monitor managedNodesMonitor = new Monitor(false);
    private TorqueCommandLineEvaluation commandLineEvaluation;

	private class AddNormalNodeTask implements Callable<TorqueNode> {

		private final TorqueNodeConfiguration nodeConfiguration;
		private final TorqueNodeManagement torqueNodeManagement;

		public AddNormalNodeTask(TorqueNodeConfiguration nodeConfiguration, TorqueNodeManagement torqueNodeManagement) {
			this.nodeConfiguration = nodeConfiguration;
			this.torqueNodeManagement = torqueNodeManagement;
		}

		@Override
		public TorqueNode call() throws Exception {
			return nodeDeployer.submitJob(nodeConfiguration, torqueNodeManagement);
		}
	}
	
	private class RemoveNormalNodeTask implements Callable<Void> {
		private String host;
		private int managementPort;

		public RemoveNormalNodeTask(String host, int managementPort) {
			this.host = host;
			this.managementPort = managementPort;
		}

		@Override
		public Void call() throws Exception {
			JMXNodeConnectionWrapper wrapper = 
					NodeManagementConnector.openNodeConnection(host, managementPort);
			wrapper.shutdown();
			wrapper.close();
			return null;
		}
	}
    
    private class RemoveNormalNodeTask2 implements Callable<Void> {
        private final Collection<String> nodeUuids;

		public RemoveNormalNodeTask2(Collection<String> nodeUuids) {
			this.nodeUuids = nodeUuids;
		}

		@Override
		public Void call() throws Exception {
			String driverHost = "localhost";
            int serverPort = JPPFLocalDriver.SERVER_PORT;

            JPPFManagementByJobsClient client = null;
            try {
                client = JPPFConfiguratedComponentFactory.getInstance().createManagementByJobsClient(
                        driverHost, serverPort);
                for (String uuid : nodeUuids) {
                    client.shutdownNode(uuid);
                }
            } catch (Exception ex) {
                logger.warn("Not all nodes could be shut down.", ex);
            } finally {
                if (client != null) {
                    client.close();
                }
            }
            return null;
		}
	}
	
	private final Configuration configuration;
	private ListeningExecutorService executorService;
	private Set<TorqueNode> nodes = new HashSet<TorqueNode>();
	private TorqueJPPFNodeDeployer nodeDeployer = null;

    public TorqueNodeManager(Configuration configuration) {
        this.configuration = configuration;
        try {
            TorqueConfiguration torqueConfiguration = buildTorqueConfiguration();
            String privateKeyPath = torqueConfiguration.getPrivateKeyPath();
            Injector injector = Guice.createInjector(new SSHModule());
            SSHClient sshClient = injector.getInstance(SSHClient.class);
//            SSHClient sshClient = new SSHClientImpl(privateKeyPath);
            sshClient.setPrivateKey(privateKeyPath);
            nodeDeployer = new TorqueJPPFNodeDeployer(buildTorqueConfiguration(), sshClient);
            executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(THREAD_POOL_SIZE));
        } catch (ConfigurationValueMissingException ex) {
            logger.error("Configuration value is missing.", ex);
        } catch (SSHClientException ex) {
            logger.error("Could not start ssh client. Something is wrong with your private key or its path.", ex);
        }
    }
    
    private TorqueConfiguration buildTorqueConfiguration() throws ConfigurationValueMissingException {
        return TorqueConfiguration.buildFromConfig(configuration);
    }

	public Collection<? extends Node> getNodes() {
		return Collections.unmodifiableCollection(nodes);
	}
    
    public static CommandLineEvaluation commandLineEvaluation(Configuration configuration, CommandLineHandle handle, 
            Observer observer, RmiServerForApi rmiServerForApi) {
        TorqueNodeManager nodeManager = new TorqueNodeManager(configuration);
        nodeManager.addPublicIpListener(observer);
        return nodeManager.getCommandLineEvaluation(handle, rmiServerForApi);
    }
    
    public CommandLineEvaluation getCommandLineEvaluation(CommandLineHandle handle, RmiServerForApi rmiServerForApi) {
        if (commandLineEvaluation == null) {
            commandLineEvaluation = new TorqueCommandLineEvaluation(this, handle, rmiServerForApi);
        }
        return commandLineEvaluation;
    }

	public ListenableFuture<? extends Node> addNode(TorqueNodeConfiguration nodeConfiguration) {
        return executorService.submit(new AddNormalNodeTask(nodeConfiguration, this));
	}
	
//	public ListenableFuture<Void> removeNode(TorqueNode torqueNode) {
//		Preconditions.checkNotNull(torqueNode, "torqueNode must not be null.");
//        return executorService.submit(
//                new RemoveNormalNodeTask(
//                    Iterables.getFirst(torqueNode.getPublicAddresses(), null), torqueNode.getManagementPort()));
//	}
    
    public ListenableFuture<Void> removeNodes(Collection<String> nodeUuids) {
        Preconditions.checkNotNull(nodeUuids, "nodeUuids must not be null.");
        return executorService.submit(new RemoveNormalNodeTask2(nodeUuids));
    }
	
	public void removeAllNodes() {
        logger.info("Remove all nodes.");
        
        String driverHost = "localhost";
        int serverPort = JPPFLocalDriver.SERVER_PORT;
        
        JPPFManagementByJobsClient client = null;
        try {
    		client = JPPFConfiguratedComponentFactory.getInstance()
				.createManagementByJobsClient(
					driverHost, serverPort);
            try {
                client.shutdownAllNodes();
            } catch (Exception ex) {
                logger.warn("Not all nodes could be shut down.", ex);
            }
            nodes.clear();
        } finally {
            if (client != null) {
                client.close();
            }
        }
	}

//	public void deployResources() {
//		try {
//			nodeDeployer.deployInfrastructure();
//		} catch (SSHClientException ex) {
//			logger.error(null, ex);
//		}
//	}

	@Override
	public void addManagedNode(TorqueNode torqueNode) {
        managedNodesMonitor.enter();
        try {
            nodes.add(torqueNode);
        } finally {
            managedNodesMonitor.leave();
        }
	}

	@Override
	public void removeManagedNode(TorqueNode torqueNode) {
        managedNodesMonitor.enter();
        try {
            nodes.remove(torqueNode);
        } finally {
            managedNodesMonitor.leave();
        }
	}
	
	public void shutdown() {
		nodeDeployer.disconnectSshConnection();
		executorService.shutdown();
		List<Runnable> stillRunningThreads = executorService.shutdownNow();
		System.out.println("stillRunningThreads.size() = " + stillRunningThreads.size());
	}
    
    public void addPublicIpListener(Observer publicIpListener) {
        this.nodeDeployer.addListener(publicIpListener);
    }
    
    public Configuration getConfiguration() {
        return this.configuration;
    }
	
}
