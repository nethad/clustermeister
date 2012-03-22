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
 * distributed under the License in distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.nethad.clustermeister.node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * A thread reading from an InputStream to empty it continously.
 *
 * The StreamGobbler type identified by a type string.
 * 
 * This class is adjusted from the example provided in the 
 * <a href='http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps.html'>
 * "When Runtime.exec() won't"</a> 
 * article. 
 *
 * @author daniel
 * @see http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps.html
 */
public class StreamGobbler extends Thread {

    /**
     * The type of the StreamGobbler.
     */
    protected String type;
    
    /**
     * The InputStream to read from.
     */
    protected InputStream in;
    
    /**
     * An output stream to redirect the input to.
     */
    protected OutputStream out;

    /**
     * Creates a StreamGobbler identified by type for the purpose of continously 
     * reading from and emptying the specified input stream and redirecting the 
     * content to an output stream.
     * 
     * @param type the type of the StreamGobbler (e.g. ERROR).
     * @param in the input stream to read from.
     * @param redirect the output stream to redirect the input to.
     */
    public StreamGobbler(String type, InputStream in, OutputStream redirect) {
        this.type = type;
        this.in = in;
        this.out = redirect;
    }
    
    /**
     * Creates a StreamGobbler identified by type for the purpose of continously 
     * reading from and emptying the specified input stream.
     * 
     * @param type the type of the StreamGobbler (e.g. ERROR).
     * @param in the input stream to read from.
     */
    public StreamGobbler(String type, InputStream in) {
        this(type, in, null);
    }

    @Override
    public void run() {
        PrintWriter pw = null;
        if (out != null) {
            pw = new PrintWriter(out);
        }
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = br.readLine()) != null) {
                if (pw != null) {
                    pw.println(line);
                }
                System.out.println(type + ">" + line);
            }
            if (pw != null) {
                pw.flush();
            }
            //else discard
        } catch (IOException ex) {
            if(pw != null) {
                ex.printStackTrace(pw);
            } else {
                ex.printStackTrace(System.out);
            }
        }
    }
}