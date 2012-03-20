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

import org.jppf.process.ProcessLauncher;
import org.jppf.process.event.ProcessWrapperEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A ProcessLauncher that does not close the spawned sub-process when the JVM 
 * is shut down.
 *
 * @author daniel
 */
public class ClustermeisterProcessLauncher extends ProcessLauncher {
    protected final static Logger logger =
            LoggerFactory.getLogger(ClustermeisterProcessLauncher.class);
    
    
    private boolean divertStreams = false;
    
    /**
     * Constructor.
     * 
     * @param mainClass 
     *      The fully qualified class name of the class to run in a new process. 
     */
    public ClustermeisterProcessLauncher(String mainClass) {
        super(mainClass);
    }

    @Override
    protected void createShutdownHook() {
        //nop, we want to keep the JVM running.
    }

    @Override
    public void errorStreamAltered(ProcessWrapperEvent event) {
        String content = event.getContent();
        if(divertStreams) {
            logger.error(content);
        } else {
            System.err.print(content);
        }
    }

    @Override
    public void outputStreamAltered(ProcessWrapperEvent event) {
        String content = event.getContent();
        if(divertStreams) {
            logger.info(event.getContent());
        } else {
            System.out.print(content);
        }
    }
    
    /**
     * Switch output and error streams from the spawned process between standard 
     * out/err and logger.
     * 
     * After initialization the output and error streams are written to 
     * System.out/System.err.
     */
    public void switchStreams() {
        divertStreams = !divertStreams;
    }
}
