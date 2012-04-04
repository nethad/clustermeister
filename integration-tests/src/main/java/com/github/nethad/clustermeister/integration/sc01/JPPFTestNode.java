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
package com.github.nethad.clustermeister.integration.sc01;

import com.github.nethad.clustermeister.provisioning.jppf.*;
import java.io.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas
 */
public class JPPFTestNode {
    
    private final Logger logger = LoggerFactory.getLogger(JPPFTestNode.class);
    private Process nodeProcess;
    private NodeProcessOutputter nodeProcessOutputter;
    private File tempDir;
    
    public void prepare() {
                String currentDirPath = System.getProperty("user.dir");
        File nodeDir = getNodeDir(new File(currentDirPath));
        File jppfNodeZip = new File(nodeDir, "target/jppf-node.zip");
        if (!jppfNodeZip.exists()) {
            throw new RuntimeException("Could not find jppf-node.zip.");
        }
        tempDir = new File(System.getProperty("java.io.tmpdir"));
        if (!tempDir.exists()) {
            throw new RuntimeException("Temp directory does not exist");
        }
        unzipNode(jppfNodeZip, tempDir);
        JPPFNodeConfiguration nodeConfiguration = new JPPFNodeConfiguration();
        nodeConfiguration.setProperty("jppf.server.host", "localhost")
                .setProperty("jppf.management.port", String.valueOf(12001))
                .setProperty("jppf.resource.cache.dir", "/tmp/.jppf/node-" + System.currentTimeMillis())
                .setProperty("processing.threads", String.valueOf(4));
        
            final File propertiesFile = new File(tempDir, "/jppf-node/config/jppf-node.properties");
        try {
            InputStream propertyStream = nodeConfiguration.getPropertyStream();
            copyInputStream(propertyStream, new FileOutputStream(propertiesFile));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void startNode() throws RuntimeException {
        File startNodeScript = new File(tempDir, "jppf-node/startNode.sh");
        startNodeScript.setExecutable(true);
        try {
//            String jppfNodePath = startNodeScript.getParentFile().getAbsolutePath();
            final String command = String.format("%s %s %s",
                    "./" + startNodeScript.getName(), 
                    "config/jppf-node.properties",
                    "true");
            logger.info("Start node with {}", command);
            nodeProcess = Runtime.getRuntime().exec(command, new String[]{}, startNodeScript.getParentFile());
//            Process exec = Runtime.getRuntime().exec(command);
            InputStream inputStream = nodeProcess.getInputStream();
            //            CharStreams.class;
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            nodeProcessOutputter = new NodeProcessOutputter(reader);
            nodeProcessOutputter.start();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public void shutdown() {
        JPPFManagementByJobsClient client = JPPFConfiguratedComponentFactory.getInstance().createManagementByJobsClient("localhost", 11111);
        try {
            client.shutdownAllNodes();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            client.close();
        }
    }
    
    private void unzipNode(File fileToUnzip, File targetDir) {
        Enumeration entries;
        ZipFile zipFile;
        try {
            zipFile = new ZipFile(fileToUnzip);
            entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                if (entry.isDirectory()) {
                    // Assume directories are stored parents first then children.
                    System.err.println("Extracting directory: " + entry.getName());
                    // This is not robust, just for demonstration purposes.
                    (new File(targetDir, entry.getName())).mkdir();
                    continue;
                }
                System.err.println("Extracting file: " + entry.getName());
                File targetFile = new File(targetDir, entry.getName());
                copyInputStream(zipFile.getInputStream(entry),
                        new BufferedOutputStream(new FileOutputStream(targetFile)));
            }
            zipFile.close();
        } catch (IOException ioe) {
            System.err.println("Unhandled exception:");
            ioe.printStackTrace();
        }
    }
        
    public void copyInputStream(InputStream in, OutputStream out)
            throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) >= 0) {
            out.write(buffer, 0, len);
        }
        in.close();
        out.close();
    }
    
    private File getNodeDir(File startDir) {
        if (startDir.isDirectory()) {
            File subDir = new File(startDir, "node");
            if (subDir.exists() && subDir.isDirectory()) {
                return subDir;
            } else {
                return getNodeDir(subDir.getParentFile().getParentFile());
            }
        } else {
            throw new RuntimeException("startDir is not a directory.");
        }
    }
    
    public class NodeProcessOutputter extends Thread {
        
        private BufferedReader reader;

        private NodeProcessOutputter(BufferedReader reader) {
            this.reader = reader;
        }

        @Override
        public void run() {
            System.out.println("### THREAD STARTED ###");
            try {
                String nextLine;
                while ((nextLine = reader.readLine()) != null) {
                    System.out.println("process line ==> " + nextLine);
                }
            } catch (IOException ex) {
                System.err.println("Exception while printing node output.");
                ex.printStackTrace();
            }
        }

        private void close() {
            try {
                this.reader.close();
            } catch (IOException ex) {
                // ignore
            }
        }
        
    }
    
}
