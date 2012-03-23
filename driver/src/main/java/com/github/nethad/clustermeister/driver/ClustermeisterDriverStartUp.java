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

import com.github.nethad.clustermeister.node.Constants;
import com.github.nethad.clustermeister.node.ShutdownHandler;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import org.jppf.server.JPPFDriver;
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
        //make sure the UUID is printed to standard out in a well defined format.
        System.out.println(Constants.UUID_PREFIX + JPPFDriver.getInstance().getUuid());
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
        } else {
            //register a shutdown handler to allow for graceful termination.
            ShutdownHandler shutdownHandler = new ShutdownHandler(System.in);
            shutdownHandler.start();
        }
    }
}
