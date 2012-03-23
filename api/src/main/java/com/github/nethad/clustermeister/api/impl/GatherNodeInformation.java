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

import com.github.nethad.clustermeister.api.ExecutorNode;
import com.github.nethad.clustermeister.api.NodeCapabilities;
import com.github.nethad.clustermeister.api.utils.NodeManagementConnector;
import com.github.nethad.clustermeister.sample.GatherNodeInformationTask;
import com.github.nethad.clustermeister.sample.GatherNodeInformationTaskInBroadcastJob;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.*;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jppf.JPPFException;
import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.client.JPPFResultCollector;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.node.policy.Equal;
import org.jppf.server.JPPFStats;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.TypedProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas
 */
class GatherNodeInformation {

    public static final String DRIVER_HOST = "localhost";
    public static final int DRIVER_MGMT_PORT = 11198;
    private Logger logger = LoggerFactory.getLogger(GatherNodeInformation.class);
    private final Lock lock = new ReentrantLock();
    private final Condition lastJobFinished = lock.newCondition();
    private JMXDriverConnectionWrapper wrapper;
    private final JPPFClient client;
    private AtomicInteger nodeCounter = new AtomicInteger(0);
    private Collection<ExecutorNode> nodes;
    private final ThreadsExecutorService threadsExecutorService;

    public GatherNodeInformation(JPPFClient client, ThreadsExecutorService threadsExecutorService) {
        this.client = client;
        this.nodes = new LinkedList<ExecutorNode>();
        this.threadsExecutorService = threadsExecutorService;
    }

    public Collection<ExecutorNode> getNodes() {
        // TODO centralize executor service?

        logger.info("collect node information");
        List<JPPFResultCollector> collectorList = new ArrayList<JPPFResultCollector>();
        wrapper = null;
        try {
            wrapper = NodeManagementConnector.openDriverConnection(DRIVER_HOST, DRIVER_MGMT_PORT);
            JPPFManagementInfo firstNodeInfo = wrapper.nodesInformation().iterator().next();
            JPPFStats statistics = wrapper.statistics();
            for (JPPFManagementInfo node : wrapper.nodesInformation()) {
                JPPFJob job = createJobForNode(node);
                JPPFResultCollector collector = new JPPFResultCollector(job);
                job.setResultListener(collector);
                collectorList.add(collector);
                client.submit(job);
                nodeCounter.incrementAndGet();
                nonBlockingResultCollector(collector);
            }
            logger.info("Submitted all jobs, wait for results...");

            awaitTermination(lock, lastJobFinished);
        } catch (Exception ex) {
            logger.error("Error while getting node information", ex);
        } finally {
            if (wrapper != null) {
                try {
                    wrapper.close();
                } catch (Exception ex) {
                    // ignore
                }
            }
        }
        return nodes;
    }
    
    public Collection<ExecutorNode> getNodesWithBroadcastJob() {
                logger.info("Collect node information (broadcast job)");
        List<JPPFResultCollector> collectorList = new ArrayList<JPPFResultCollector>();
        wrapper = null;
        try {
            wrapper = NodeManagementConnector.openDriverConnection(DRIVER_HOST, DRIVER_MGMT_PORT);
            int numberOfNodes = wrapper.nodesInformation().size();
            
            JPPFJob job = new JPPFJob();
            job.addTask(new GatherNodeInformationTaskInBroadcastJob());
            job.setBlocking(false);
            job.getSLA().setBroadcastJob(true);
            JPPFResultCollector collector = new JPPFResultCollector(job);
            job.setResultListener(collector);
            nodeCounter.set(numberOfNodes);
            client.submit(job);
            nonBlockingResultCollector(collector);
            
            logger.info("Submitted all jobs, wait for results...");
            awaitTermination(lock, lastJobFinished);
        } catch (Exception ex) {
            logger.error("Error while getting node information", ex);
        } finally {
            if (wrapper != null) {
                try {
                    wrapper.close();
                } catch (Exception ex) {
                    // ignore
                }
            }
        }
        return nodes;
    }

    private JPPFJob createJobForNode(JPPFManagementInfo node) {
        try {
            JPPFJob job = new JPPFJob();
            final GatherNodeInformationTask task = new GatherNodeInformationTask(node.getPort());
            job.addTask(task);
            job.setBlocking(false);
//            job.getSLA().setCancelUponClientDisconnect(true);
            job.getSLA().setMaxNodes(1);
            job.getSLA().setExecutionPolicy(new Equal(GatherNodeInformationTask.UUID, true, node.getId()));
            return job;
        } catch (JPPFException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void nonBlockingResultCollector(JPPFResultCollector collector) {
        ListenableFuture<TypedProperties> result = threadsExecutorService.submit(new ResultCollectorCallable<TypedProperties>(collector));
        Futures.addCallback(result, new FutureCallback<TypedProperties>() {

            @Override
            public void onSuccess(TypedProperties result) {
                addExecutorNode(result);
            }

            @Override
            public void onFailure(Throwable t) {
                logger.error("Node information query was not successful.", t);
                onNodeResultReturned();
            }
        });
    }
    
    @VisibleForTesting
    void addExecutorNode(TypedProperties result) {
        ExecutorNodeImpl executorNode = new ExecutorNodeImpl(client, threadsExecutorService);
        executorNode.setId(result.getProperty(GatherNodeInformationTask.UUID));
        NodeCapabilities nodeCapabilities = new NodeCapabilitiesImpl(
                result.getInt(GatherNodeInformationTask.AVAILABLE_PROCESSORS),
                result.getInt(GatherNodeInformationTask.PROCESSING_THREADS),
                result.getString("jppfconfig"));
        executorNode.setNodeCapabilities(nodeCapabilities);
//        System.out.println(result.getProperty(GatherNodeInformationTask.IPV4_ADDRESSES));
        List<String> allIpAddresses = extractAddressesFromString(result.getProperty(GatherNodeInformationTask.IPV4_ADDRESSES));
        executorNode.addPrivateAddresses(getAllPrivateAddresses(allIpAddresses));
        executorNode.addPublicAddresses(getAllPublicAddresses(allIpAddresses));
        nodes.add(executorNode);
        onNodeResultReturned();
    }

    private void awaitTermination(final Lock lock, final Condition lastJobFinished) throws RuntimeException {
        System.out.println("Await termination.");
        lock.lock();
        try {
            lastJobFinished.await();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        } finally {
            lock.unlock();
        }
    }

    private void onNodeResultReturned() {
        int nodeCount = nodeCounter.decrementAndGet();
        if (nodeCount == 0) {
            lock.lock();
            try {
                logger.info("All node information is ready.");
                lastJobFinished.signal();
            } finally {
                lock.unlock();
            }
        } else {
            logger.info("Still waiting for " + nodeCount + " nodes.");
        }
    }

    @VisibleForTesting
    Set<String> getAllPrivateAddresses(List<String> ipStrings) {
//        InetAddresses
        Set<String> addressList = new HashSet<String>();
        for (String ipAddress : ipStrings) {
            if (isSiteLocalButNotLoopbackIpAddress(ipAddress)) {
                addressList.add(ipAddress);
            }
        }
        return addressList;
    }
    
    private boolean isSiteLocalButNotLoopbackIpAddress(String ipAddress) {
        InetAddress inetAddress = InetAddresses.forString(ipAddress);
        return inetAddress.isSiteLocalAddress() && !inetAddress.isLoopbackAddress();
    }

    @VisibleForTesting
    List<String> extractAddressesFromString(String ipAddressesString) {
        // IP addresses are returned as a String, in the form of:
        // 192.168.2.4|192.168.2.4 node1.my.domain|100.2.3.4 localhost|127.0.0.1
        List<String> list = new LinkedList<String>();
        String[] split = ipAddressesString.split(" ");
        for (String addressPair : split) {
            String[] addresses = addressPair.split("\\|");
            if (addresses.length != 2) {
                throw new RuntimeException("Malformed IP address string.");
            }
            list.add(addresses[1]);
        }
        return list;
    }

    @VisibleForTesting
    Set<String> getAllPublicAddresses(List<String> ipStrings) {
        Set<String> addressList = new HashSet<String>();
        for (String ipAddress : ipStrings) {
            if (isPublicAddress(ipAddress)) {
                addressList.add(ipAddress);
            }
        }
        return addressList;
    }
    
    private boolean isPublicAddress(String ipAddress) {
        InetAddress inetAddress = InetAddresses.forString(ipAddress);
        return !inetAddress.isSiteLocalAddress() && !inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress();
    }
}
