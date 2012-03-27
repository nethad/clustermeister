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
package com.github.nethad.clustermeister.provisioning.jppf;

import com.github.nethad.clustermeister.provisioning.jppf.managementtasks.JPPFConfigReaderTask;
import com.github.nethad.clustermeister.provisioning.jppf.managementtasks.ShutdownExecutingNode;
import com.github.nethad.clustermeister.provisioning.jppf.managementtasks.ShutdownSingleNodeTask;
import com.github.nethad.clustermeister.provisioning.utils.IPSocket;
import com.github.nethad.clustermeister.api.utils.NodeManagementConnector;
import com.github.nethad.clustermeister.node.common.Constants;
import com.github.nethad.clustermeister.provisioning.jppf.managementtasks.DummyTask;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.management.Notification;
import javax.management.NotificationListener;
import org.jppf.JPPFException;
import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.client.event.JobEvent;
import org.jppf.client.event.JobListener;
import org.jppf.job.JobEventType;
import org.jppf.job.JobNotification;
import org.jppf.management.JMXConnectionWrapper;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.node.policy.Equal;
import org.jppf.node.policy.ExecutionPolicy;
import org.jppf.node.policy.OneOf;
import org.jppf.server.job.management.DriverJobManagementMBean;
import org.jppf.server.protocol.JPPFTask;
import org.slf4j.LoggerFactory;

/**
 *
 * @author daniel
 */
public class JPPFManagementByJobsClient {

    private final static org.slf4j.Logger logger =
            LoggerFactory.getLogger(JPPFManagementByJobsClient.class);
    private JPPFClient jPPFClient;

    /**
     * Default constructor: intended to be used only by JPPFConfiguratedComponentFactory.
     */
    JPPFManagementByJobsClient() {
        jPPFClient = new JPPFClient();
    }

    public Properties getJPPFConfig(String host, int port) {
        JPPFJob job = new JPPFJob();
        try {
            job.addTask(new JPPFConfigReaderTask(), host, port);
        } catch (JPPFException ex) {
            throw new RuntimeException(ex);
        }
        job.getSLA().setMaxNodes(1);
        job.setBlocking(true);
        job.getSLA().setSuspended(false);
        List<JPPFTask> results = Collections.EMPTY_LIST;
        try {
            results = jPPFClient.submit(job);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        JPPFTask task = Iterables.getOnlyElement(results);
        return (Properties) task.getResult();
    }

    /**
     * Shuts a single node (not driver) down.
     *
     * This sends a job to shut down the specified node to an arbitrary node connected to the driver this
     * JPPFManagementByJobsClient is associated to.
     *
     * This method is synchronous. It waits for the job to complete before it returns.
     *
     * @deprecated Use {@link #shutdownNodes(java.util.Collection) }
     * @param driverHost The driver that the node to shut down is connected to.
     * @param driverManagementPort The JMX management port of the driver the node to shut down is connected to.
     * @param nodeHost The hostname or IP of the node to shut down.
     * @param nodeManagementPort The JMX management port of the node to shut down.
     */
    public void shutdownNode(String driverHost, int driverManagementPort,
            final String nodeHost, final int nodeManagementPort) {
        JMXDriverConnectionWrapper wrapper = null;
        try {
            wrapper = NodeManagementConnector.openDriverConnection(
                    driverHost, driverManagementPort);
            Collection<JPPFManagementInfo> nodesInfo = new ArrayList<JPPFManagementInfo>(1);
            JPPFManagementInfo info = Iterables.find(wrapper.nodesInformation(),
                    new Predicate<JPPFManagementInfo>() {

                        @Override
                        public boolean apply(JPPFManagementInfo info) {
                            return info.getHost().equals(nodeHost)
                                    && info.getPort() == nodeManagementPort;
                        }
                    }, null);
            if (info != null) {
                nodesInfo.add(info);
                shutdownNodes(nodesInfo, wrapper);
            } else {
                logger.warn("No such node ({}:{}) connected to driver.", nodeHost, nodeManagementPort);
            }
        } catch (Exception ex) {
            logger.warn("Failed to shut down node.", ex);
        } finally {
            if (wrapper != null) {
                try {
                    wrapper.close();
                } catch (Exception ex) {
                    logger.warn("Could not close wrapper.", ex);
                }
            }
        }
    }

    /**
     * Shuts down nodes specified by their Management sockets (host and management port).
     *
     * This sends a job to shut down the specified nodes to an arbitrary node connected to the driver this
     * JPPFManagementByJobsClient is associated to.
     *
     * This method is synchronous. It waits for the job to complete before it returns.
     *
     * @deprecated Use {@link #shutdownNodes(java.util.Collection) }
     * @param driverHost The driver that the node to shut down is connected to.
     * @param driverManagementPort The JMX management port of the driver the node to shut down is connected to.
     * @param sockets Contains all node sockets to shut down. Note that the port must be the management port of the
     * node.
     */
    public void shutdownNodes(String driverHost, int driverManagementPort,
            final Collection<IPSocket> sockets) {
        JMXDriverConnectionWrapper wrapper = null;
        try {
            wrapper = NodeManagementConnector.openDriverConnection(
                    driverHost, driverManagementPort);
            Iterable<JPPFManagementInfo> matchingNodes = Iterables.filter(wrapper.nodesInformation(),
                    new Predicate<JPPFManagementInfo>() {

                        @Override
                        public boolean apply(JPPFManagementInfo info) {
                            return sockets.contains(
                                    new IPSocket(info.getHost(), info.getPort()));
                        }
                    });
            shutdownNodes(Lists.newArrayList(matchingNodes), wrapper);
        } catch (Exception ex) {
            logger.warn("Failed to shut down node.", ex);
        } finally {
            if (wrapper != null) {
                try {
                    wrapper.close();
                } catch (Exception ex) {
                    logger.warn("Could not close wrapper.", ex);
                }
            }
        }
    }

    /**
     * @deprecated This method is deprecated, because it will be migrated to a solution that won't require a JMX
     * connection and uses node UUIDs.
     * @param driverHost
     * @param driverManagementPort 
     */
    public void shutdownAllNodes(String driverHost, int driverManagementPort) {
        JMXDriverConnectionWrapper wrapper = null;
        try {
            wrapper = NodeManagementConnector.openDriverConnection(
                    driverHost, driverManagementPort);
            final Collection<JPPFManagementInfo> nodes = wrapper.nodesInformation();
            if (!nodes.isEmpty()) {
                shutdownNodes(nodes, wrapper);
            }
        } catch (Exception e) {
            logger.warn("Failed to shut down all nodes.", e);
        } finally {
            if (wrapper != null) {
                try {
                    wrapper.close();
                } catch (Exception ex) {
                    logger.warn("Could not close wrapper.", ex);
                }
            }
        }
    }
    
    public void shutdownNodes(Collection<String> nodeUuids) {
        logger.info("Shutting down "+nodeUuids.size()+" nodes...");
        try {
            JPPFJob shutdownAllNodesJob = createShutdownJob(nodeUuids);
            List<JPPFTask> tasks = jPPFClient.submit(shutdownAllNodesJob);
            if (!tasks.isEmpty()) {
                Exception taskException = tasks.get(0).getException();
                if (taskException != null) {
                    logger.error("Job submission failed.", taskException);
                }
            } else {
                logger.error("Task list was empty.");
            }
        } catch (Exception ex) {
            logger.error("Job submission failed.", ex);
        }
        logger.info("Job submission completed.");
    }
    
    public void shutdownAllNodes() {
        logger.info("Shutting down all nodes...");
        try {
            JPPFJob shutdownJobForAllNodes = createShutdownJobForAllNodes();        
            submitJob(shutdownJobForAllNodes);
        } catch (JPPFException ex) {
            logger.error("Exception while creating shutdown job.", ex);
        }
    }
    
    public void submitJob(JPPFJob job) {
        try {
            List<JPPFTask> tasks = jPPFClient.submit(job);
            if (!tasks.isEmpty()) {
                Exception taskException = tasks.get(0).getException();
                if (taskException == null) {
                    return;
                } else {
                    logger.error("Job submission failed.", taskException);
                }
            } else {
                logger.error("Task list was empty.");
            }
        } catch (Exception ex) {
            logger.error("Job submission failed.", ex);
        }
        logger.info("Job submission completed.");
    }

//    public void shutdownDriver(String driverHost, int managementPort) {
//        JMXDriverConnectionWrapper wrapper = null;
//        try {
//            wrapper =
//                    NodeManagementConnector.openDriverConnection(driverHost, managementPort);
//            // TODO remove 1s shutdown
//            wrapper.restartShutdown(1 * 1000L, -1L);
//
//        } catch (TimeoutException ex) {
//            logger.warn("Timed out waiting for driver management connection.", ex);
//        } catch (Exception ex) {
//            logger.warn("Could not shut down driver.", ex);
//        } finally {
//            if (wrapper != null) {
//                try {
//                    wrapper.close();
//                } catch (Exception ex) {
//                    logger.error("Could not close JMX wrapper", ex);
//                }
//            }
//        }
//    }

    public void close() {
        if (jPPFClient != null) {
            jPPFClient.close();
            jPPFClient = null;
        }
    }

    /**
     * @deprecated Use {@link #shutdownNodes(java.util.Collection)}
     * @param nodeInfos
     * @param wrapper
     * @throws InterruptedException
     * @throws Exception 
     */
    private void shutdownNodes(Collection<JPPFManagementInfo> nodeInfos,
            JMXDriverConnectionWrapper wrapper) throws InterruptedException, Exception {
        
        if (nodeInfos.isEmpty()) {
            logger.debug("No nodes to shut down.");
            return;
        }
        JPPFManagementInfo chosenEntry = Iterables.getFirst(nodeInfos, null);
        logger.debug("Chose node {} as executor.", chosenEntry.getId());
        final JPPFJob shutdownExecutorJob = getShutdownExecutingNodeJob(chosenEntry.getId());
        final Lock lock = new ReentrantLock();
        final Condition lastJobFinished = lock.newCondition();
        if (nodeInfos.size() == 1) {
            sendSignalOnJobReturned(wrapper, lock, lastJobFinished,
                    shutdownExecutorJob);
            jPPFClient.submit(shutdownExecutorJob);
        } else {
            final JMXConnectionWrapper finalWrapper = wrapper;
            JPPFJob job = createShutdownJob(nodeInfos, chosenEntry.getId());
            job.addJobListener(new JobListener() {

                @Override
                public void jobStarted(JobEvent event) {
                    //nop
                }

                @Override
                public void jobEnded(JobEvent event) {
                    if (event.getJob().getPendingTasks().isEmpty()) {
                        try {
                            sendSignalOnJobReturned(finalWrapper, lock, lastJobFinished,
                                    shutdownExecutorJob);
                            jPPFClient.submit(shutdownExecutorJob);
                        } catch (Exception ex) {
                            logger.error("Could not shut down shutdown executor node.", ex);
                        }
                    }
                }
            });
            jPPFClient.submit(job);
        }
        logger.debug("Waiting for all nodes to shut down...");
        awaitSignal(lock, lastJobFinished);
        logger.debug("All nodes are shut down.");
    }
    
//    private void shutdownNodes(Collection<String> nodeUuids) {
//        shutdown
//    }

    private JPPFJob getShutdownNodeJob(String nodeHost, int managementPort) throws JPPFException {

        JPPFJob job = getJobSkeleton("Shutdown " + nodeHost + ":" + managementPort,
                1, false, false);
        job.addTask(new ShutdownSingleNodeTask(), nodeHost, managementPort);
        return job;
    }
    
    private JPPFJob getShutdownExecutingNodeJob(String nodeUUID) throws JPPFException {
        JPPFJob job = getJobSkeleton("Shutdown " + nodeUUID, 1, false, false);
        job.addTask(new ShutdownExecutingNode(), job.getUuid());
        job.getSLA().setExecutionPolicy(new Equal("jppf.uuid", false, nodeUUID));
        return job;
    }
    
    private JPPFJob getJobSkeleton(String name, int maxNodes, boolean blocking, boolean suspended) {
        JPPFJob job = new JPPFJob();
        job.setName(name);
        job.getSLA().setMaxNodes(maxNodes);
        job.setBlocking(blocking);
        job.getSLA().setSuspended(suspended);
        return job;
    }

    private void sendSignalOnJobReturned(JMXConnectionWrapper wrapper, final Lock lock,
            final Condition condition, final JPPFJob job) throws IllegalArgumentException, Exception {
        DriverJobManagementMBean proxy = wrapper.getProxy(
                DriverJobManagementMBean.MBEAN_NAME, DriverJobManagementMBean.class);
        proxy.addNotificationListener(new NotificationListener() {

            @Override
            public void handleNotification(Notification notification, Object handback) {
                JobNotification jobNotification = (JobNotification) notification;
                if (jobNotification.getEventType().equals(JobEventType.JOB_RETURNED)) {
                    if (jobNotification.getJobInformation().getJobUuid().
                            equals(job.getUuid())) {
                        lock.lock();
                        try {
                            condition.signal();
                        } finally {
                            lock.unlock();
                        }
                    }
                }
            }
        }, null, null);
    }

    private void awaitSignal(final Lock lock, final Condition lastJobFinished)
            throws InterruptedException {
        lock.lock();
        try {
            lastJobFinished.await();
            Thread.sleep(50); //just wait a wee bit more.
        } finally {
            lock.unlock();
        }
    }

    /**
     * @deprecated Use {@link #createShutdownJob(java.util.Collection)}
     * @param nodeInfos
     * @param executorUUID
     * @return
     * @throws JPPFException
     * @throws NumberFormatException 
     */
    private JPPFJob createShutdownJob(Collection<JPPFManagementInfo> nodeInfos,
            String executorUUID) throws JPPFException, NumberFormatException {

        JPPFJob job = getJobSkeleton("Shutdown nodes", 1, false, false);
        for (JPPFManagementInfo nodeInfo : nodeInfos) {
            if (!nodeInfo.getId().equals(executorUUID)) {
                job.addTask(new ShutdownSingleNodeTask(), nodeInfo.getHost(), nodeInfo.getPort());
            }
        }
        job.getSLA().setExecutionPolicy(new Equal("jppf.uuid", false, executorUUID));
        return job;
    }
    
    private JPPFJob createShutdownJob(Collection<String> nodeUuids) throws JPPFException {
        JPPFJob job = getJobSkeleton("Shutdown nodes", nodeUuids.size(), true, false);
        job.getSLA().setExecutionPolicy(createExecutionPolicyFor(nodeUuids));
        job.getSLA().setBroadcastJob(true);
        job.addTask(new DummyTask());
        job.setName(Constants.JOB_MARKER_SHUTDOWN);      
        return job;
    }
    
    private JPPFJob createShutdownJobForAllNodes() throws JPPFException {
        JPPFJob job = new JPPFJob();
        job.setName("Shutdown nodes");
        job.setBlocking(true);
        job.getSLA().setSuspended(false);
        job.getSLA().setBroadcastJob(true);
        job.addTask(new DummyTask());
        job.setName(Constants.JOB_MARKER_SHUTDOWN);       
        return job;
    }
    
    private ExecutionPolicy createExecutionPolicyFor(Collection<String> nodeUuids) {
        return new OneOf("jppf.uuid", true, nodeUuids.toArray(new String[]{}));
    }
}
