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

import com.github.nethad.clustermeister.node.common.ClustermeisterProcessLauncher.StreamSink;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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
    
    private static final String JPPF_THREAD_NAME = "CMLauncherThread";
    
    protected final static Logger logger =
            LoggerFactory.getLogger(ClustermeisterLauncher.class);
    
    /**
     * Custom Process Launcher.
     */
    protected ClustermeisterProcessLauncher processLauncher = null;
    
    private Thread jppfThread = null;
    
    private boolean printUUIDtoStdOut = false;
    
    /**
     * Performs the launching of a new JVM using the runner from {@link #getRunner()}.
     * 
     * @param launchAsChildProcess 
     *      Whether to launch a child process or independent process. 
     */
    synchronized public void doLaunch(boolean launchAsChildProcess) {
        PipedInputStream in = new PipedInputStream();
        PrintStream sout = System.out;
        PipedOutputStream out = null;
        String uuidLine = null;
        try {
            out = new PipedOutputStream(in);
            //prepare to capture spawned processes output stream.
            System.setOut(new PrintStream(out));
            try {
                //Spawn a new JVM
                startUp(launchAsChildProcess);
                uuidLine = waitForUUID(in, sout);
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
        if(printUUIDtoStdOut) {
            System.out.println(uuidLine);
        }
        setChanged();
        notifyObservers(uuidLine);
    }
    
    /**
     * Stops the sub-process (JVM).
     * 
     * This method is as graceful as possible.
     * 
     * @throws Exception when any exception occurs.
     */
    synchronized public void shutdownProcess() throws Exception {
        if(processLauncher != null) {
            Process process = processLauncher.getProcess();
            if(process != null) {
                final OutputStream outputStream = process.getOutputStream();
                outputStream.write(ShutdownHandler.SHUTDOWN_STRING.getBytes(Constants.UTF8));
                outputStream.flush();
            }
        }
    }

    /**
     * Whether the UUID is printed to stdout.
     * 
     * @return True if the UUID is printed to stdout. False otherwise.
     */
    public boolean isPrintUUIDtoStdOut() {
        return printUUIDtoStdOut;
    }

    /**
     * Set whether the UUID should be printed to stdout.
     * 
     * By default this is set to false.
     * 
     * @param printUUIDtoStdOut true to print the UUID to stdout, false to not 
     * print the UUID to stdout.
     */
    public void setPrintUUIDtoStdOut(boolean printUUIDtoStdOut) {
        this.printUUIDtoStdOut = printUUIDtoStdOut;
    }
    
    /**
     * Divert the sub-processes stdout and stderr streams to the logging framework.
     * 
     * @param divert 
     *      true to divert to logging, false to divert to parent 
     *      process's stdout/stderr.
     */
    public void divertStreamsToLog(boolean divert) {
        if(divert) {
            processLauncher.setStreamSink(StreamSink.LOG);
        } else {
            processLauncher.setStreamSink(StreamSink.STD);
        }
    }

    protected ClustermeisterProcessLauncher createProcessLauncher() {
       return new ClustermeisterProcessLauncher(getRunner());
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
        processLauncher = createProcessLauncher();
        processLauncher.setLaunchAsChildProcess(launchAsChildProcess);
        if(!launchAsChildProcess) {
            processLauncher.setStreamSink(StreamSink.FILE);
        }
        jppfThread = new Thread(new Runnable() {
            @Override
            public void run() {
                processLauncher.run();
            }
        });
        jppfThread.setName(String.format("%s-%s", JPPF_THREAD_NAME, jppfThread.getId()));
        jppfThread.start();
    }
    
    /**
     * Parse a boolean from given arguments and index. 
     * If the argument array does not contain the index return the specified 
     * default value.
     * 
     * @param args  the arguments.
     * @param index the argument index to parse.
     * @param defaultValue  the default value.
     * @return the parsed boolean or the default value.
     */
    protected static boolean getBooleanArgument(String[] args, int index, boolean defaultValue) {
        boolean value = defaultValue;
        if(args != null && args.length > index) {
            value = Boolean.parseBoolean(args[index]);
        }
        return value;
    }
    
    private String waitForUUID(InputStream in, PrintStream sout) 
            throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, Constants.UTF8));
        logger.info("Waiting for UUID.");
        String line;
        while((line = reader.readLine()) != null) {
            if(line.startsWith(Constants.UUID_PREFIX)) {
                logger.info("Got {}.", line);
                return line;
            } else {
                sout.println(line);
            }
        }
        
        return null;
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
