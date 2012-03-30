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
package com.github.nethad.clustermeister.node;

import com.github.nethad.clustermeister.node.common.Constants;
import com.github.nethad.clustermeister.node.common.MBeanUtils;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.jppf.management.JPPFNodeAdminMBean;
import org.jppf.node.NodeExecutionManager;
import org.jppf.node.NodeRunner;
import org.jppf.node.event.NodeLifeCycleEvent;
import org.jppf.node.event.NodeLifeCycleListener;
import org.jppf.node.protocol.JPPFDistributedJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom Clustermeister JPPF Node Life Cycle Listener.
 *
 * @author daniel
 */
public class ClustermeisterNodeLifeCycleListener implements NodeLifeCycleListener {

    protected final static Logger logger =
            LoggerFactory.getLogger(ClustermeisterNodeLifeCycleListener.class);
    
    @Override
    public void nodeStarting(NodeLifeCycleEvent event) {
        //make sure the UUID is printed to standard out in a well defined format.
        System.out.println(Constants.UUID_PREFIX + NodeRunner.getUuid());
        System.out.flush();
        
        boolean divertStreamsToFile = Boolean.parseBoolean(System.getProperty(
                Constants.CLUSTERMEISTER_DIVERT_STREAMS_TO_FILE));
        if(divertStreamsToFile) {
            try {
                System.setOut(new PrintStream(new FileOutputStream(Constants.STDOUT_LOG)));
                System.setErr(new PrintStream(new FileOutputStream(Constants.STDERR_LOG)));
            } catch (FileNotFoundException ex) {
                logger.warn("Could not create log file.", ex);
            }
        }
    }

    @Override
    public void nodeEnding(NodeLifeCycleEvent event) {
        //nop
    }

    @Override
    public void jobStarting(NodeLifeCycleEvent event) {
        //nop
    }

    @Override
    public void jobEnding(NodeLifeCycleEvent event) {
        JPPFDistributedJob job = event.getJob();
        if (job.getName().contains(Constants.JOB_MARKER_SHUTDOWN) && 
                isCurrentJobShutdownJob(event.getSource(), job.getUuid())) {
            shutdownNode(job);
        }
    }

    /**
     * Shuts down this node.
     * 
     * @param triggeringJob the job that triggered the shutdown.
     */
    protected void shutdownNode(final JPPFDistributedJob triggeringJob) {
        logger.info("Node shutdown requested.");
        final MBeanServer platformMBeanServer = 
                ManagementFactory.getPlatformMBeanServer();
            ObjectName nodeAdminName = MBeanUtils.objectNameFor(
                    logger, JPPFNodeAdminMBean.MBEAN_NAME);
            MBeanUtils.invoke(logger, platformMBeanServer, nodeAdminName, "shutdown");
    }
    
    private boolean isCurrentJobShutdownJob(Object eventSource, String jobId) {
        if (eventSource instanceof NodeExecutionManager) {
            NodeExecutionManager manager = (NodeExecutionManager) eventSource;
                if(manager.getCurrentJobId().equals(jobId)) {
                    return true;
                }
        }
        return false;
    }
}
