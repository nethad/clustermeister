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
package com.github.nethad.clustermeister.driver;

import com.github.nethad.clustermeister.node.common.Constants;
import com.github.nethad.clustermeister.node.common.MBeanUtils;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.jppf.job.JobListener;
import org.jppf.job.JobNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to listen to job life-cycle events.
 * 
 * @author daniel
 */
public class DriverJobListener implements JobListener {
    protected final static Logger logger =
            LoggerFactory.getLogger(DriverJobListener.class);
    
    /**
     * The MBean server used to issue the cancelJob method invocation.
     */
    protected final MBeanServer mBeanServer;
    /**
     * Name of the job management MBean.
     */
    protected final ObjectName jobManagementName;

    /**
     * Creates a new job listener.
     *
     * @param mBeanServer the MBean server to use.
     * @param jobManagementName the name of the job management MBean.
     */
    public DriverJobListener(MBeanServer mBeanServer, ObjectName jobManagementName) {
        this.mBeanServer = mBeanServer;
        this.jobManagementName = jobManagementName;
    }

    @Override
    public void jobQueued(JobNotification event) {
        //nop
    }

    @Override
    public void jobEnded(JobNotification event) {
        //nop
    }

    @Override
    public void jobUpdated(JobNotification event) {
        //nop
    }

    @Override
    public void jobDispatched(JobNotification event) {
        //nop
    }

    @Override
    public void jobReturned(JobNotification event) {
        if (event.getJobInformation().getJobName().contains(Constants.JOB_MARKER_SHUTDOWN)) {
            MBeanUtils.invoke(logger, mBeanServer, jobManagementName, "cancelJob", 
                    event.getJobInformation().getJobUuid());
        }
    }
}
