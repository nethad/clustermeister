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

import java.io.IOException;
import java.io.InputStream;

/**
 * Reads the input stream and reacts to {@link #SHUTDOWN_STRING} being read on 
 * a single line by terminating the the process with exit code 0.
 *
 * @author daniel
 */
public class ShutdownHandler extends StreamGobbler {
    
    /**
     * The exact string that triggers the shutdown.
     */
    public static final String SHUTDOWN_STRING = "CMShutdownCommand";
    
    /**
     * Creates a handler for a specified input stream.
     * 
     * @param in the input stream to read from (e.g. System.in).
     */
    public ShutdownHandler(InputStream in) {
        super("INPUT", in);
    }
    
    @Override
    protected void onOutput(String line) {
        //shutdown process when shutdown string is read.
        if(line.equals(SHUTDOWN_STRING)) {
            System.exit(0);
        }
    }

    @Override
    protected void onException(IOException ex) {
        ex.printStackTrace(System.err);
    }
}
