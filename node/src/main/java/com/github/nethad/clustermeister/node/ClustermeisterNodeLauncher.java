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
import java.io.UnsupportedEncodingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Launches a JPPF-Node in a new spawned process (independent JVM) and returns
 * when the node is initialized.
 *
 * @author daniel
 */
public class ClustermeisterNodeLauncher {
    protected final static Logger logger =
            LoggerFactory.getLogger(ClustermeisterNodeLauncher.class);
    
    /**
     * JPPF Class to use for launching the node.
     */
    protected  static final String NODE_RUNNER = "org.jppf.node.NodeRunner";
    
    /**
     * Well-defined UUID prefix.
     */
    protected static final String UUID_PREFIX = "UUID=";
    
    /**
     * UTF-8 charset string ("UTF-8").
     */
    protected static final String UTF8 = "UTF-8";
    
    
    /**
     * Custom Process Launcher.
     */
    protected static ClustermeisterProcessLauncher processLauncher = null;

    /**
     * The thread that handles launching of the JPPF-node.
     */
    protected static Thread jppfNodeThread = null; 
    
    /**
     * Start a JPPF-node.
     * 
     * The main method will spawn a new process for the JPPF-node and return as 
     * soon as it obtained the UUID and initialization of JMX management is complete.
     * 
     * @param args not used.
     */
    public static void main(String... args) throws UnsupportedEncodingException {
        PipedInputStream in = new PipedInputStream();
        PrintStream sout = System.out;
        PipedOutputStream out = null;
        try {
            out = new PipedOutputStream(in);
            //prepare to capture spawned processes output stream.
            System.setOut(new PrintStream(out));
            try {
                //Spawn a new JVM
                startUp();
                waitForUUID(in, sout);
            } catch (Exception ex) {
                logger.warn("Exception while starting node.", ex);
            }
        } catch (IOException ex) {
            logger.warn("Can not pipe output stream.", ex);
        } finally {
            closeStream(in);
            closeStream(out);
            //divert the spawned processes error and output streams to a logger.
            if(processLauncher != null) {
                processLauncher.switchStreams();
            }
            //restore output stream.
            System.setOut(sout);
            sout.flush();
        }
        
        //Exit from this JVM. The spawned process continues to run.
        System.exit(0);
    }
    
    private static void waitForUUID(InputStream in, PrintStream sout) 
            throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, UTF8));
        System.out.println("Waiting for UUID.");
        String line;
        while((line = reader.readLine()) != null) {
            sout.println(line);
            if(line.startsWith(UUID_PREFIX)) {
                sout.println("Got UUID.");
                break;
            }
        }
    }
    
    private static void closeStream(Closeable in) {
        if(in != null) {
            try {
                in.close();
            } catch (IOException ex) {
                logger.warn("Can not close pipe.", ex);
            }
        }
    }

    /**
     * 
     * @throws Exception 
     */
    protected static void startUp() throws Exception {
        processLauncher = new ClustermeisterProcessLauncher(NODE_RUNNER);
        jppfNodeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                processLauncher.run();
            }
        });
        jppfNodeThread.start();
    }
}
