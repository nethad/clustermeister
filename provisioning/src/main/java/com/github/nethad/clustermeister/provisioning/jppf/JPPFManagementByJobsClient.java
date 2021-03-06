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

import com.github.nethad.clustermeister.api.JPPFConstants;
import com.github.nethad.clustermeister.api.Loggers;
import com.github.nethad.clustermeister.node.common.Constants;
import com.github.nethad.clustermeister.provisioning.jppf.managementtasks.JPPFConfigReaderTask;
import com.github.nethad.clustermeister.provisioning.jppf.managementtasks.NopTask;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.util.*;
import org.jppf.JPPFException;
import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.node.policy.ExecutionPolicy;
import org.jppf.node.policy.OneOf;
import org.jppf.server.protocol.JPPFTask;
import org.slf4j.LoggerFactory;

/**
 *
 * @author daniel
 */
public class JPPFManagementByJobsClient {
    private final static org.slf4j.Logger logger =
            LoggerFactory.getLogger(Loggers.PROVISIONING);
    
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
     * This method is synchronous. It waits for the job to complete before it returns.
     *
     * @param nodeUuid the UUID of the node to shut down.
     */
    synchronized public void shutdownNode(String nodeUuid) throws Exception {
        logger.info("Shutting down {}...", nodeUuid);
        JPPFJob shutdownNodeJob = createShutdownJob(Arrays.asList(nodeUuid), false);
        submitJob(shutdownNodeJob);
    }
    
    /**
     * Restarts a single node (not driver) down.
     *
     * This method is synchronous. It waits for the job to complete before it returns.
     *
     * @param nodeUuid the UUID of the node to restart.
     */
    synchronized public void restartNode(String nodeUuid) throws Exception {
        logger.info("Restarting {}...", nodeUuid);
        JPPFJob restartNodeJob = createRestartJob(Arrays.asList(nodeUuid), false);
        submitJob(restartNodeJob);
    }

    /**
     * Shuts down nodes specified by their UUIDs.
     *
     * Sends a job that sends shutdown tasks to the specified nodes.
     *
     * This method is synchronous. It waits for the job to complete before it returns.
     *
     * @param nodeUuids the node UUIDs to shut down.
     * 
     * @throws Exception when the job can not be executed.
     */
    synchronized public void shutdownNodes(Collection<String> nodeUuids) throws Exception {
        logger.info("Shutting down {} nodes...", nodeUuids.size());
        JPPFJob shutdownNodesJob = createShutdownJob(nodeUuids, true);
        submitJob(shutdownNodesJob);
    }
    
    /**
     * Restarts nodes specified by their UUIDs.
     *
     * Sends restart tasks to the specified nodes.
     *
     * This method is synchronous. It waits for the job to complete before it returns.
     *
     * @param nodeUuids the node UUIDs to restart.
     * 
     * @throws Exception when the job can not be executed.
     */
    synchronized public void restartNodes(Collection<String> nodeUuids) throws Exception {
        logger.info("Restarting {} nodes...", nodeUuids.size());
        JPPFJob restartNodesJob = createRestartJob(nodeUuids, true);
        submitJob(restartNodesJob);
    }

    synchronized public void shutdownAllNodes() throws Exception {
        logger.info("Shutting down all nodes...");
        JPPFJob shutdownJobForAllNodes = createShutdownJobForAllNodes();        
        submitJob(shutdownJobForAllNodes);
    }
    
    synchronized public void restartAllNodes() throws Exception {
        logger.info("Restarting all nodes...");
        JPPFJob restartJobForAllNodes = createRestartJobForAllNodes();        
        submitJob(restartJobForAllNodes);
    }

    public void close() {
        if (jPPFClient != null) {
            jPPFClient.close();
            jPPFClient = null;
        }
    }

    private JPPFJob getJobSkeleton(String name, int maxNodes, boolean blocking, boolean suspended) {
        JPPFJob job = new JPPFJob();
        job.setName(name);
        job.getSLA().setMaxNodes(maxNodes);
        job.setBlocking(blocking);
        job.getSLA().setSuspended(suspended);
        return job;
    }

    private JPPFJob createShutdownJob(Collection<String> nodeUuids, boolean broadcast) throws JPPFException {
        JPPFJob job = getJobSkeleton(Constants.JOB_MARKER_SHUTDOWN + 
                Arrays.toString(nodeUuids.toArray()), nodeUuids.size(), true, false);
        job.getSLA().setExecutionPolicy(createExecutionPolicyFor(nodeUuids));
        job.getSLA().setBroadcastJob(broadcast);
        job.addTask(new NopTask());
        return job;
    }
    
    private JPPFJob createRestartJob(Collection<String> nodeUuids, boolean broadcast) throws JPPFException {
        JPPFJob job = getJobSkeleton(Constants.JOB_MARKER_RESTART + 
                Arrays.toString(nodeUuids.toArray()), nodeUuids.size(), true, false);
        job.getSLA().setExecutionPolicy(createExecutionPolicyFor(nodeUuids));
        job.getSLA().setBroadcastJob(broadcast);
        job.addTask(new NopTask());
        return job;
    }
    
    private JPPFJob createShutdownJobForAllNodes() throws JPPFException {
        JPPFJob job = getJobSkeleton(Constants.JOB_MARKER_SHUTDOWN, 0, true, false);
        job.getSLA().setBroadcastJob(true);
        job.addTask(new NopTask());
        return job;
    }
    
    private JPPFJob createRestartJobForAllNodes() throws JPPFException {
        JPPFJob job = getJobSkeleton(Constants.JOB_MARKER_RESTART, 0, true, false);
        job.getSLA().setBroadcastJob(true);
        job.addTask(new NopTask());
        return job;
    }
    
    private ExecutionPolicy createExecutionPolicyFor(Collection<String> nodeUuids) {
        return new OneOf(JPPFConstants.UUID, true, nodeUuids.toArray(new String[]{}));
    }
    
    /**
     * @throws Exception when the job submission failed.
     */
    private void submitJob(JPPFJob job) throws Exception {
        List<JPPFTask> tasks = jPPFClient.submit(job);
        if(job.isBlocking()) {
            if (tasks != null && !tasks.isEmpty()) {
                Iterable<JPPFTask> failedTasks = Iterables.filter(tasks, new Predicate<JPPFTask>() {
                    @Override
                    public boolean apply(JPPFTask task) {
                        return task != null && task.getException() != null;
                    }
                });
                if(!Iterables.isEmpty(failedTasks)) {
                    logger.warn("{} tasks raised an exception.", Iterables.size(failedTasks));
                    if(logger.isDebugEnabled()) {
                        for(JPPFTask failedTask : failedTasks) {
                            logger.debug("{} failed with: {}.", failedTask.getId(), 
                                    failedTask.getException().getMessage());
                        }
                    }
                }
            } else {
                logger.warn("Task list for job {} is empty.", job.getName());
            }
            logger.info("Job {} completed.", job.getName());
        } else {
            logger.info("Job {} submitted.", job.getName());
        }
    }
}
