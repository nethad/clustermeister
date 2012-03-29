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
import com.github.nethad.clustermeister.node.common.ShutdownHandler;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import org.jppf.job.JobEventType;
import org.jppf.job.JobNotification;
import org.jppf.server.JPPFDriver;
import org.jppf.server.job.management.DriverJobManagementMBean;
import org.jppf.startup.JPPFDriverStartupSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom Clustermeister JPPF-Driver startup class.
 *
 * @author daniel
 */
public class ClustermeisterDriverStartUp implements JPPFDriverStartupSPI {

    protected final static Logger logger =
            LoggerFactory.getLogger(ClustermeisterDriverStartUp.class);
    
    @Override
    public void run() {
        printUUIDToSystemOut();
        
        if(isDivertStreamsToFile()) {
            divertStreamsToFile();
            logger.info("Output and Error streams diverted to files.");
        } else {
            //register a shutdown handler to allow for graceful termination.
            ShutdownHandler shutdownHandler = new ShutdownHandler(System.in);
            shutdownHandler.start();
            logger.info("System.in shutdown command handler started.");
        }
        
        final MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
        registerJobListener(platformMBeanServer);
        logger.info("Job listener registered.");
        
    }

    /**
     * Register an MBean job NotificationListener.
     * 
     * @param mBeanServer the MBeanserver
     */
    protected void registerJobListener(final MBeanServer mBeanServer) {
        try {
            ObjectName jobManagementName = new ObjectName(DriverJobManagementMBean.MBEAN_NAME);
            mBeanServer.addNotificationListener(jobManagementName, 
                    new DriverJobListener(mBeanServer, jobManagementName), null, null);
        } catch (MalformedObjectNameException ex) {
            logger.warn("Invalid Object name.", ex);
        } catch (NullPointerException ex) {
            logger.warn("Invalid Object name.", ex);
        } catch (InstanceNotFoundException ex) {
            logger.warn("Can not find MBean.", ex);
        }
    }

    /**
     * Check whether to divert streams to files.
     * 
     * @return true if the corresponding System property is set to "true", false otherwise.
     */
    protected boolean isDivertStreamsToFile() {
        return Boolean.parseBoolean(System.getProperty(
                Constants.CLUSTERMEISTER_DIVERT_STREAMS_TO_FILE));
    }

    /**
     * Divert stdout and stderr to files.
     */
    protected void divertStreamsToFile() {
        try {
            System.setOut(new PrintStream(new FileOutputStream(Constants.STDOUT_LOG)));
            System.setErr(new PrintStream(new FileOutputStream(Constants.STDERR_LOG)));
        } catch (FileNotFoundException ex) {
            logger.warn("Could not create log file.", ex);
        }
    }

    /**
     * Print the UUID to stdout.
     */
    protected void printUUIDToSystemOut() {
        //make sure the UUID is printed to standard out in a well defined format.
        System.out.println(Constants.UUID_PREFIX + JPPFDriver.getInstance().getUuid());
        System.out.flush();
    }
    
    /**
     * Utility class to listen to job life-cycle events.
     */
    protected class DriverJobListener implements NotificationListener {
        
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
        public void handleNotification(Notification notification, Object handback) {
            JobNotification jobNotif = (JobNotification) notification;
            JobEventType eventType = jobNotif.getEventType();
            final String jobName = jobNotif.getJobInformation().getJobName();
            final String jobUUID = jobNotif.getJobInformation().getJobUuid();
            switch(eventType) {
                case JOB_QUEUED: {
                    //nop
                    break;
                }
                case JOB_UPDATED: {
                    //nop
                    break;
                }
                case JOB_DISPATCHED: {
                    //nop
                    break;
                }
                case JOB_RETURNED: {
                    if(jobName.contains(Constants.JOB_MARKER_SHUTDOWN)) {
                        cancelJob(jobUUID);
                    }
                    break;
                }
                case JOB_ENDED: {
                    //nop
                    break;
                }
            }
        }

        /**
         * Cancels the job with specified UUID.
         * 
         * @param jobNotif the job UUID to cancel.
         */
        protected void cancelJob(String jobUUID) {
            try {
                logger.debug("Canceling shutdown job {}.", jobUUID);
                mBeanServer.invoke(jobManagementName, "cancelJob", 
                        new Object[]{jobUUID}, new String[]{String.class.getCanonicalName()});
            } catch (InstanceNotFoundException ex) {
                logger.warn("No MBean instance found.", ex);
            } catch (MBeanException ex) {
                logger.warn("MBean raised exception during cancelJob method invocation.", ex);
            } catch (ReflectionException ex) {
                logger.warn("Method not found.", ex);
            }
        }
    }
}
