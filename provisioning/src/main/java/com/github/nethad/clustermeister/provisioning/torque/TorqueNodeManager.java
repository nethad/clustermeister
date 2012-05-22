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

import com.github.nethad.clustermeister.api.Loggers;
import com.github.nethad.clustermeister.provisioning.CommandLineEvaluation;
import com.github.nethad.clustermeister.provisioning.CommandLineHandle;
import com.github.nethad.clustermeister.provisioning.ConfigurationKeys;
import com.github.nethad.clustermeister.provisioning.injection.SSHModule;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFConfiguratedComponentFactory;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFLocalDriver;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFManagementByJobsClient;
import com.github.nethad.clustermeister.provisioning.rmi.RmiServerForApi;
import com.github.nethad.clustermeister.provisioning.torque.commands.TorqueCommandLineEvaluation;
import com.github.nethad.clustermeister.provisioning.utils.SSHClient;
import com.github.nethad.clustermeister.provisioning.utils.SSHClientException;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.util.Collection;
import java.util.List;
import java.util.Observer;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: - functionality like add(deploy)/remove JPPD nodes. - starting/deploying drivers - this is not yet to be
 * directly used by the user but by a higher layer of abstraction (to be defined) - this will likely need to implement a
 * yet to be defined interface together with AmazonNodeManager
 *
 * @author daniel
 */
public class TorqueNodeManager {

	public static final int THREAD_POOL_SIZE = 2;
//    private static final String JVM_OPTIONS_NODE_KEY = "jvm_options.node";
	private Logger logger = LoggerFactory.getLogger(Loggers.PROVISIONING);
	


	private class AddNodeTask implements Callable<Void> {

		private final TorqueNodeConfiguration nodeConfiguration;

		public AddNodeTask(TorqueNodeConfiguration nodeConfiguration) {
			this.nodeConfiguration = nodeConfiguration;
		}

		@Override
		public Void call() throws Exception {
			nodeDeployer.deployNewNode(nodeConfiguration);
            return null;
		}
	}
    
    private class RemoveNodeTask implements Callable<Void> {
        private final Collection<String> nodeUuids;

		public RemoveNodeTask(Collection<String> nodeUuids) {
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
//	private Set<TorqueNode> nodes = new HashSet<TorqueNode>();
	private TorqueJPPFNodeDeployer nodeDeployer = null;
//	private final Monitor managedNodesMonitor = new Monitor(false);
    private TorqueCommandLineEvaluation commandLineEvaluation;

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

	public ListenableFuture<Void> addNode(TorqueNodeConfiguration nodeConfiguration) {
        if (!nodeConfiguration.getJvmOptions().isPresent()) {
            nodeConfiguration.setJvmOptions(configuration.getString(ConfigurationKeys.JVM_OPTIONS_NODE, null));
        }
        return executorService.submit(new AddNodeTask(nodeConfiguration));
	}
    
    public ListenableFuture<Void> removeNodes(Collection<String> nodeUuids) {
        Preconditions.checkNotNull(nodeUuids, "nodeUuids must not be null.");
        return executorService.submit(new RemoveNodeTask(nodeUuids));
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
        } finally {
            if (client != null) {
                client.close();
            }
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
