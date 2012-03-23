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

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Launches a JPPF-Node in a new spawned process (JVM) and is able 
 * to react (by returning or notifying) when the process is initialized.
 * 
 * A dependent child process will keep this JVM running until the child process 
 * or this JVM dies. Observers will be notified when the initialization is 
 * complete.
 * 
 * An independent process will kill this JVM (and thus "return") upon successful 
 * initialization of the sub process.
 *
 * @author daniel
 */
public abstract class ClustermeisterLauncher extends Observable {
    protected final static Logger logger =
            LoggerFactory.getLogger(ClustermeisterLauncher.class);
    
    /**
     * Custom Process Launcher.
     */
    protected ClustermeisterProcessLauncher processLauncher = null;

    /**
     * Performs the launching of a new JVM using the runner from {@link #getRunner()}.
     * 
     * @param launchAsChildProcess 
     *      Whether to launch a child process or independent process. 
     */
    public void doLaunch(boolean launchAsChildProcess) {
        PipedInputStream in = new PipedInputStream();
        PrintStream sout = System.out;
        PipedOutputStream out = null;
        try {
            out = new PipedOutputStream(in);
            //prepare to capture spawned processes output stream.
            System.setOut(new PrintStream(out));
            try {
                //Spawn a new JVM
                startUp(launchAsChildProcess);
                waitForUUID(in, sout);
            } catch (Exception ex) {
                logger.warn("Exception while launching.", ex);
            }
        } catch (IOException ex) {
            logger.warn("Can not read from pipe output stream of the JPPF sub-process.", ex);
        } finally {
            //restore output stream.
            System.setOut(sout);
            closeStream(in);
            closeStream(out);
            sout.flush();
        }
    }
    
     /**
     * Get the fully qualified class name of the runner to use.
     * 
     * @return the runner.
     */
    abstract protected String getRunner();
    
    /**
     * Starts the JPPF process (a new JVM).
     * 
     * @throws Exception when any exception occurs process spawning preparation.
     */
    protected void startUp(boolean launchAsChildProcess) throws Exception {
        processLauncher = new ClustermeisterProcessLauncher(getRunner());
        processLauncher.setLaunchAsChildProcess(launchAsChildProcess);
        if(!launchAsChildProcess) {
            processLauncher.switchStreamsToFiles();
        }
        Thread jppfThread = new Thread(new Runnable() {
            @Override
            public void run() {
                processLauncher.run();
            }
        });
        jppfThread.start();
    }
    
    private void waitForUUID(InputStream in, PrintStream sout) 
            throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, Constants.UTF8));
        System.out.println("Waiting for UUID.");
        String line;
        while((line = reader.readLine()) != null) {
            sout.println(line);
            if(line.startsWith(Constants.UUID_PREFIX)) {
                sout.println("Got UUID.");
                break;
            }
        }
    }
    
    private void closeStream(Closeable in) {
        if(in != null) {
            try {
                in.close();
            } catch (IOException ex) {
                logger.warn("Can not close stream.", ex);
            }
        }
    }
}
