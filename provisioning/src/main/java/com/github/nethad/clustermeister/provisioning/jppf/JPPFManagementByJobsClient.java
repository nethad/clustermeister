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
import com.github.nethad.clustermeister.provisioning.jppf.managementtasks.ShutdownSingleNodeTask;
import com.github.nethad.clustermeister.provisioning.utils.IPSocket;
import com.github.nethad.clustermeister.provisioning.utils.NodeManagementConnector;
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
import java.util.logging.Level;
import java.util.logging.Logger;
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

    public void shutdownDriver(String driverHost, int managementPort) {
        JMXDriverConnectionWrapper wrapper = null;
        try {
            wrapper =
                    NodeManagementConnector.openDriverConnection(driverHost, managementPort);
            // TODO remove 1s shutdown
            wrapper.restartShutdown(1 * 1000L, -1L);

        } catch (TimeoutException ex) {
            logger.warn("Timed out waiting for driver management connection.", ex);
        } catch (Exception ex) {
            logger.warn("Could not shut down driver.", ex);
        } finally {
            if (wrapper != null) {
                try {
                    wrapper.close();
                } catch (Exception ex) {
                    logger.error("Could not close JMX wrapper", ex);
                }
            }
        }
    }

    public void close() {
        if (jPPFClient != null) {
            jPPFClient.close();
            jPPFClient = null;
        }
    }

    private void shutdownNodes(Collection<JPPFManagementInfo> nodeInfos,
            JMXDriverConnectionWrapper wrapper) throws InterruptedException, Exception {
        JPPFManagementInfo chosenEntry =
                Iterables.getFirst(nodeInfos, null);
        String executorHost = chosenEntry.getHost();
        int executorPort = chosenEntry.getPort();
        logger.debug("Chose node {}:{} as executor.", executorHost, executorPort);
        final JPPFJob shutdownExecutorJob = getShutdownNodeJob(executorHost, executorPort);
        final Lock lock = new ReentrantLock();
        final Condition lastJobFinished = lock.newCondition();
        if (nodeInfos.isEmpty()) {
            logger.debug("No nodes to shut down.");
            return;
        } else if (nodeInfos.size() == 1) {
            sendSignalOnJobReturned(wrapper, lock, lastJobFinished,
                    shutdownExecutorJob);
            jPPFClient.submit(shutdownExecutorJob);
        } else {
            final JMXConnectionWrapper finalWrapper = wrapper;
            JPPFJob job = createShutdownJob(nodeInfos, executorHost, executorPort);
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
        logger.debug("All nodes are shut down. Canceling Job: {}", shutdownExecutorJob.getName());
        wrapper.cancelJob(shutdownExecutorJob.getUuid());
    }

    private JPPFJob getShutdownNodeJob(String nodeHost, int managementPort)
            throws JPPFException, Exception {

        JPPFJob job = new JPPFJob();
        job.setName("Shutdown " + nodeHost + ":" + managementPort);
        job.addTask(new ShutdownSingleNodeTask(), nodeHost, managementPort);
        job.getSLA().setMaxNodes(1);
        job.setBlocking(false);
        job.getSLA().setSuspended(false);
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
        } finally {
            lock.unlock();
        }
    }

    private JPPFJob createShutdownJob(Collection<JPPFManagementInfo> nodeInfos,
            String executorHost, int executorPort) throws JPPFException, NumberFormatException {

        JPPFJob job = new JPPFJob();
        job.setName("Shutdown nodes");
        String uuid = null;
        for (JPPFManagementInfo nodeInfo : nodeInfos) {
            if (nodeInfo.getPort() != executorPort || !nodeInfo.getHost().equals(executorHost)) {
                job.addTask(new ShutdownSingleNodeTask(), nodeInfo.getHost(), nodeInfo.getPort());
            } else {
                uuid = nodeInfo.getId();
            }
        }
        job.getSLA().setMaxNodes(1);
        job.getSLA().setExecutionPolicy(new Equal("jppf.uuid", false, uuid));
        job.setBlocking(false);
        return job;
    }
}
