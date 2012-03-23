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
package com.github.nethad.clustermeister.node.common;

import org.jppf.process.ProcessLauncher;
import org.jppf.process.event.ProcessWrapperEvent;
import org.jppf.utils.JPPFConfiguration;
import org.jppf.utils.TypedProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A ProcessLauncher that allows to choose whether to destroy the spawned 
 * sub-process when the JVM is shut down.
 *
 * @author daniel
 */
public class ClustermeisterProcessLauncher extends ProcessLauncher {
    protected final static Logger logger =
            LoggerFactory.getLogger(ClustermeisterProcessLauncher.class);
    
    
    private boolean divertStreamsToLog = false;
    
    private boolean divertStreamsToFiles = false;
    
    private boolean launchAsChildProcess = false;
    
    /**
     * Handle on the spawned subprocess.
     */
    protected Process process = null;
    
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
    public Process buildProcess() throws Exception {
        TypedProperties config = JPPFConfiguration.getProperties();
        String jvmOptions = config.getString(Constants.JPPF_JVM_OPTIONS);
        if(jvmOptions == null) {
            jvmOptions = "";
        }
        if(isDivertStreamsToFiles()) {
            jvmOptions += " -D" + Constants.CLUSTERMEISTER_DIVERT_STREAMS_TO_FILE + "=true";
        } else {
            jvmOptions += " -D" + Constants.CLUSTERMEISTER_DIVERT_STREAMS_TO_FILE + "=false";
        }
        config.setProperty(Constants.JPPF_JVM_OPTIONS, jvmOptions.trim());
        this.process = super.buildProcess();
        
        return process;
    }

    @Override
    protected void createShutdownHook() {
        if(launchAsChildProcess) {
            super.createShutdownHook();
        } else {
            //nop, we want to keep the JVM running.
        }
    }

    @Override
    public void errorStreamAltered(ProcessWrapperEvent event) {
        String content = event.getContent();
        if(isDivertStreamsToLog()) {
            logger.error(content);
        } else {
            //don't deal with file logging here, 
            //the sub-process takes care of this.
            System.err.print(content);
        }
    }

    @Override
    public void outputStreamAltered(ProcessWrapperEvent event) {
        String content = event.getContent();
        if(isDivertStreamsToLog()) {
            logger.info(event.getContent());
        } else {
            //don't deal with file logging here, 
            //the sub-process takes care of this.
            System.out.print(content);
        }
    }
    
    /**
     * Switch output and error streams of the spawned process between standard 
     * out/err and logger.
     * 
     * Default setting: After initialization the output and error streams are written to 
     * System.out/System.err.
     */
    public void switchStreamsToLog() {
        if(divertStreamsToFiles) {
            divertStreamsToFiles = !divertStreamsToFiles;
        }
        divertStreamsToLog = !divertStreamsToLog;
    }

    /**
     * Checks if the sub-process' stdin/stderr streams are diverted to loggers.
     * 
     * @return Whether the streams are diverted to loggers.
     */
    public boolean isDivertStreamsToLog() {
        return divertStreamsToLog;
    }
    
    /**
     * Switch stdout and stderr streams of the spawned process between standard 
     * out/err and files.
     * 
     * Default setting: After initialization the output and error streams are written to 
     * System.out/System.err.
     */
    public void switchStreamsToFiles() {
        if(divertStreamsToLog) {
            divertStreamsToLog = !divertStreamsToLog;
        }
        divertStreamsToFiles = !divertStreamsToFiles;
    }
    
    /**
     * Checks if the sub-process' stdin/stderr streams are diverted to files.
     * 
     * @return Whether the streams are diverted to files.
     */
    public boolean isDivertStreamsToFiles() {
        return divertStreamsToFiles;
    }

    /**
     * Set whether the sub process is launched as a dependent child-process or 
     * as an independent process.
     * 
     * @param launchAsChildProcess 
     */
    public void setLaunchAsChildProcess(boolean launchAsChildProcess) {
        this.launchAsChildProcess = launchAsChildProcess;
    }
    
    /**
     * Checks if the launcher is set to launch the sub process as a child 
     * process or as an independent process. 
     * 
     * The former blocks until the child process dies, the second returns after 
     * initialization.
     * 
     * @return whether the launcher launches the sub process as a child process.
     */
    public boolean isLaunchAsChildProcess() {
        return launchAsChildProcess;
    }

    /**
     * Returns the spawned sub-process handle.
     * 
     * @return the sub-process or null if it has not been started yet.
     */
    public Process getProcess() {
        return process;
    }

}
