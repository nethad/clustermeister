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
    
    /**
     * Determines where to divert the sub-processes stdout and stderr streams to.
     * <ul>
     *  <li>STD - to the parent process's stdout/stderr streams.</li>
     *  <li>LOG - to the parent process's logging framework. Stdout as INFO and stderr as ERROR</li>
     *  <li>FILE - written to a file (stdout.log and stderr.log) in the sub-process's working directory.</li>
     * </ul>
     * 
     * Note that once the sub process is started with FILE as sink, 
     * it may not be possible to change the sink anymore.
     */
    public static enum StreamSink {STD, LOG, FILE};
    
    protected final static Logger logger =
            LoggerFactory.getLogger(ClustermeisterProcessLauncher.class);
    
    private StreamSink sink = StreamSink.STD;
    
    private boolean launchAsChildProcess = false;
    
    /**
     * Handle on the spawned subprocess.
     */
    protected Process process = null;
    private boolean useRmi;
    
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
        StringBuilder options = new StringBuilder(jvmOptions.length() + 
                Constants.CLUSTERMEISTER_DIVERT_STREAMS_TO_FILE.length() + 
                Constants.CLUSTERMEISTER_USE_RMI.length() + 16);
        options.append(jvmOptions);
        if(getStreamSink() == StreamSink.FILE) {
            options.append(" -D").
                    append(Constants.CLUSTERMEISTER_DIVERT_STREAMS_TO_FILE).
                    append("=true");
        } else {
            options.append(" -D").
                    append(Constants.CLUSTERMEISTER_DIVERT_STREAMS_TO_FILE).
                    append("=false");
        }
        
        if (isUseRmi()) {
            options.append(" -D").
                    append(Constants.CLUSTERMEISTER_USE_RMI).
                    append("=true");
        } else {
            options.append(" -D").
                    append(Constants.CLUSTERMEISTER_USE_RMI).
                    append("=false");
        }
        
        config.setProperty(Constants.JPPF_JVM_OPTIONS, options.toString().trim());
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
        if(getStreamSink() == StreamSink.LOG) {
            logger.error(content.substring(0, content.length() - 1));
        } else {
            //don't deal with file logging here, 
            //the sub-process takes care of this.
            System.err.print(content);
        }
    }

    @Override
    public void outputStreamAltered(ProcessWrapperEvent event) {
        String content = event.getContent();
        if(getStreamSink() == StreamSink.LOG) {
            logger.info(content.substring(0, content.length() - 1));
        } else {
            //don't deal with file logging here, 
            //the sub-process takes care of this.
            System.out.print(content);
        }
    }
    
    /**
     * Set the target of the sub-processes stdout and stderr streams.
     * 
     * @param sink the sink of the sub-processes streams.
     */
    public void setStreamSink(StreamSink sink) {
        this.sink = sink;
    }
    
    /**
     * Returns the current target of the sub-processes stdout and stderr streams.
     * 
     * @return the sink of the sub-processes streams.
     */
    public StreamSink getStreamSink() {
        return sink;
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
    
    public void setUseRmi(boolean useRmi) {
        this.useRmi = useRmi;
    }

    public boolean isUseRmi() {
        return useRmi;
    }

}
