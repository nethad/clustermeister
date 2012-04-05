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
package com.github.nethad.clustermeister.integration;

import com.github.nethad.clustermeister.provisioning.jppf.JPPFConfiguratedComponentFactory;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFManagementByJobsClient;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFNodeConfiguration;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
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
//    private File tempDir;
    private File targetDir;
    
    public void prepare() {
        String currentDirPath = System.getProperty("user.dir");
        targetDir = getTargetDir(currentDirPath);
        InputStream jppfNodeZipStream = JPPFTestNode.class.getResourceAsStream("/jppf-node.zip");
        if (jppfNodeZipStream == null) {
            throw new RuntimeException("Could not find jppf-node.zip.");
        }
        unzipNode(jppfNodeZipStream, targetDir);
        JPPFNodeConfiguration nodeConfiguration = new JPPFNodeConfiguration();
        nodeConfiguration.setProperty("jppf.server.host", "localhost")
                .setProperty("jppf.management.port", String.valueOf(12001))
                .setProperty("jppf.resource.cache.dir", "/tmp/.jppf/node-" + System.currentTimeMillis())
                .setProperty("processing.threads", String.valueOf(4));
        
        final File propertiesFile = new File(targetDir, "/jppf-node/config/jppf-node.properties");
        try {
            InputStream propertyStream = nodeConfiguration.getPropertyStream();
            copyInputStream(propertyStream, new FileOutputStream(propertiesFile));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void startNode() throws RuntimeException {
        File startNodeScript = new File(targetDir, "jppf-node/startNode.sh");
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
            new File(targetDir, "jppf-node").deleteOnExit();
        }
    }
    
    private void unzipNode(InputStream fileToUnzip, File targetDir) {
//        Enumeration entries;
        ZipInputStream zipFile;
        try {
            zipFile = new ZipInputStream(fileToUnzip);
            ZipEntry entry;
            while ((entry = zipFile.getNextEntry()) != null) {
//                ZipEntry entry = (ZipEntry) entries.nextElement();
                if (entry.isDirectory()) {
                    // Assume directories are stored parents first then children.
                    System.err.println("Extracting directory: " + entry.getName());
                    // This is not robust, just for demonstration purposes.
                    (new File(targetDir, entry.getName())).mkdir();
                    continue;
                }
                System.err.println("Extracting file: " + entry.getName());
                File targetFile = new File(targetDir, entry.getName());
                copyInputStream_notClosing(zipFile,
                        new BufferedOutputStream(new FileOutputStream(targetFile)));
//                zipFile.closeEntry();
            }
            zipFile.close();
        } catch (IOException ioe) {
            System.err.println("Unhandled exception:");
            ioe.printStackTrace();
        }
    }
    
    private void copyInputStream_notClosing(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) >= 0) {
            out.write(buffer, 0, len);
        }
        out.close();
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

    private File getTargetDir(String currentDirPath) {
        File currentDir = new File(currentDirPath);
        if (!currentDir.isDirectory()) {
            throw new RuntimeException("user.dir is not a directory.");
        }
        File[] listFiles = currentDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.equals("target");
            }
        });
        for (File file : listFiles) {
            if (file.getName().equals("target") && file.isDirectory()) {
                return file;
            }
        }
        File targetDir = new File(currentDir, "target");
        if (targetDir.mkdir()) {
            return targetDir;
        } else {
            throw new RuntimeException("Could not create target directory.");
        }
    }
    
    public class NodeProcessOutputter extends Thread {
        
        private BufferedReader reader;

        private NodeProcessOutputter(BufferedReader reader) {
            this.reader = reader;
        }

        @Override
        public void run() {
            logger.info("{} started.", getClass().getName());
            try {
                String nextLine;
                while ((nextLine = reader.readLine()) != null) {
                    logger.info("STDOUT: {}", nextLine);
                }
            } catch (IOException ex) {
                logger.warn("IOException while printing STDOUT.", ex);
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
