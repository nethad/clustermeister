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
import com.github.nethad.clustermeister.node.common.ShutdownHandler;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
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
        
        JPPFDriver.getInstance().getJobManager().addJobListener(
                new DriverJobListener(ManagementFactory.getPlatformMBeanServer(),
                MBeanUtils.objectNameFor(logger, DriverJobManagementMBean.MBEAN_NAME)));
        logger.info("Job listener registered.");
        
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
}
