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
package com.github.nethad.clustermeister.api.impl;

import com.github.nethad.clustermeister.api.*;
import com.github.nethad.clustermeister.api.rmi.IRmiServerForApi;
import com.github.nethad.clustermeister.api.utils.JPPFProperties;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterators;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import org.jppf.client.JPPFClient;
import org.jppf.client.concurrent.JPPFExecutorService;
import org.jppf.server.protocol.JPPFTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas
 */
public class ClustermeisterImpl implements Clustermeister {

    private JPPFExecutorService executorService;
    private JPPFClient jppfClient;
    private Logger logger = LoggerFactory.getLogger(ClustermeisterImpl.class);
    private Collection<ExecutorNode> nodes;
    private Collection<NodeInformation> nodeInformationCollection;
    private IRmiServerForApi rmiServerForApi;
    private ThreadsExecutorService threadsExecutorService;


    public ClustermeisterImpl() {
        jppfClient = new JPPFClient("clustermeister_" + System.currentTimeMillis());
        executorService = new JPPFExecutorService(jppfClient);
//        nodes = new LinkedList<ExecutorNode>();
        threadsExecutorService = new ThreadsExecutorService();
        setupRmi();
    }

    private void setupRmi() throws RuntimeException {
        final String policyUrl = ClustermeisterImpl.class.getResource("/cm.policy").toString();
        logger.info("Policy file URL: {}", policyUrl);
        System.setProperty("java.security.policy", policyUrl);
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        try {
            Registry registry = LocateRegistry.getRegistry(61111);
            rmiServerForApi = (IRmiServerForApi)registry.lookup(IRmiServerForApi.NAME);
        } catch (NotBoundException ex) {
            throw new RuntimeException(ex);
        } catch (AccessException ex) {
            throw new RuntimeException(ex);
        } catch (RemoteException ex) {
            throw new RuntimeException(ex);
        }
    }

    protected void gatherNodeInformation() {
        //        nodes = new GatherNodeInformation(jppfClient, threadsExecutorService).getNodes();
        try {
            nodeInformationCollection = rmiServerForApi.getAllNodes();
            logger.info("Provisioning returned {} nodes.", nodeInformationCollection.size());
            nodes = new LinkedList<ExecutorNode>();
            for(NodeInformation nodeInfo : nodeInformationCollection) {
                ExecutorNodeImpl executorNode = new ExecutorNodeImpl(jppfClient, threadsExecutorService);
                executorNode.setId(nodeInfo.getID());
                int availableProcessors = Integer.valueOf(nodeInfo.getJPPFSystemInformation().getRuntime().getProperty(JPPFProperties.AVAILABLE_PROCESSORS));
                int numberOfProcessingThreads = Integer.valueOf(nodeInfo.getJPPFSystemInformation().getJppf().getProperty(JPPFProperties.PROCESSING_THREADS));
                final String jppfInfo = nodeInfo.getJPPFSystemInformation().getJppf().asString();
                NodeCapabilities nodeCapabilities = new NodeCapabilitiesImpl(availableProcessors, numberOfProcessingThreads, jppfInfo);
                executorNode.setNodeCapabilities(nodeCapabilities);
                nodes.add(executorNode);
            }
        } catch (RemoteException ex) {
            logger.error("Could not get list of nodes via RMI.", ex);
        }
    }

    @Override
    public ExecutorService getExecutorService() {
        return executorService;
    }

    @Override
    public JPPFClient getJppfClient() {
        return jppfClient;
    }

    @Override
    public void shutdown() {
        executorService.shutdown();
        List<Runnable> runnables = executorService.shutdownNow();
        if (runnables != null) {
            System.out.println("Runnables, size = " + runnables.size());
        }
        threadsExecutorService.shutdown();
        jppfClient.close();
    }

    @Override
    public Collection<ExecutorNode> getAllNodes() {
        return this.nodes;
    }

    @Override
    public <T> List<T> executeJob(Job<T> job) throws Exception {
        List<T> resultObjects = new LinkedList<T>();
        List<JPPFTask> results = jppfClient.submit(job.getJppfJob());
        for (JPPFTask jppfTask : results) {
            Exception exception = jppfTask.getException();
            if (exception == null) {
                Object result = jppfTask.getResult();
                if (result != null) {
                    resultObjects.add((T)result);
                } else {
                    System.out.println("Result was null");
                }
            } else {
                System.out.println("Exception: "+exception.getMessage());
                exception.printStackTrace();
            }
        }
//        Collections2.transform(results, new Function<JPPFTask, Object>() {
//            @Override
//            public Object apply(JPPFTask input) {
//                return input.getResult();
//            }
//        });
        return resultObjects;
    }

}
