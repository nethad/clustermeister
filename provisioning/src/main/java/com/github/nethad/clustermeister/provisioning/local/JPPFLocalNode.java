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
package com.github.nethad.clustermeister.provisioning.local;

import com.github.nethad.clustermeister.api.JPPFConstants;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFConfiguratedComponentFactory;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFManagementByJobsClient;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFNodeConfiguration;
import com.google.common.base.Optional;
import com.google.common.io.Files;
import java.io.*;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas
 */
public class JPPFLocalNode {
    private final Logger logger = LoggerFactory.getLogger(JPPFLocalNode.class);
    
    private Process nodeProcess;
//    private NodeProcessOutputter nodeProcessOutputterStdOut;
    private NodeProcessOutputterToFile nodeProcessOutputterStdOutToFile;
//    private NodeProcessOutputter nodeProcessOutputterStdErr;
    private NodeProcessOutputterToFile nodeProcessOutputterStdErrToFile;
//    private File tempDir;
    private File targetDir;
    
    private int managementPort = 12001;
    private int currentNodeNumber = 0;
    private String currentNodeConfig;
    private File libDir;

    public JPPFLocalNode() {
    }
    
    public void prepare(Collection<File> artifactsToPreload) {
        unpackNodeZip();
        libDir = new File(targetDir, "jppf-node/lib/");
        preloadLibraries(artifactsToPreload);
    }
    
//    public void prepare() {
//        unpackNodeZip();
//        libDir = new File(targetDir, "jppf-node/lib/");
////        preloadLibraries();
//    }
    
    private void preloadLibraries(Collection<File> artifactsToPreload) {
        for (File artifact : artifactsToPreload) {
            File destinationFile = new File(libDir, artifact.getName());
            try {
                logger.info("Copy {} to {}", artifact.getName(), destinationFile.getAbsolutePath());
                Files.copy(artifact, destinationFile);
            } catch (IOException ex) {
                logger.warn("Could not copy artifact {} to {}", artifact.getAbsolutePath(), destinationFile.getAbsolutePath());
                logger.warn("Exception: ", ex);
            }
        }
    }
    
    public void startNewNode(LocalNodeConfiguration nodeConfiguration) {
        prepareNodeConfiguration(nodeConfiguration);
        startNode(nodeConfiguration);
    }

    private void prepareNodeConfiguration(LocalNodeConfiguration localNodeConfiguration) throws RuntimeException {
        JPPFNodeConfiguration nodeConfiguration = new JPPFNodeConfiguration();
        nodeConfiguration.setProperty(JPPFConstants.SERVER_HOST, "localhost")
                .setProperty(JPPFConstants.MANAGEMENT_PORT, String.valueOf(managementPort++))
                .setProperty(JPPFConstants.RESOURCE_CACHE_DIR, "/tmp/.jppf/node-" + System.currentTimeMillis())
                .setProperty(JPPFConstants.PROCESSING_THREADS, String.valueOf(localNodeConfiguration.getNumberOfProcessingThreads()));
        
        currentNodeConfig = "jppf-node-"+(currentNodeNumber++)+".properties";
        File configDir = new File(targetDir, "/jppf-node/config/");
        final File propertiesFile = new File(configDir, currentNodeConfig);
        try {
            InputStream propertyStream = nodeConfiguration.getPropertyStream();
            copyInputStream(propertyStream, new FileOutputStream(propertiesFile));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void unpackNodeZip() throws RuntimeException {
//        String currentDirPath = System.getProperty("user.dir");
        targetDir = Files.createTempDir();
        logger.info("Created temp dir {}", targetDir.getAbsolutePath());
        InputStream jppfNodeZipStream = JPPFLocalNode.class.getResourceAsStream("/jppf-node.zip");
        if (jppfNodeZipStream == null) {
            throw new RuntimeException("Could not find jppf-node.zip.");
        }
        unzipNode(jppfNodeZipStream, targetDir);
    }

    private void startNode(LocalNodeConfiguration localNodeConfiguration) throws RuntimeException {
        File startNodeScript = new File(targetDir, "jppf-node/startNode.sh");
        startNodeScript.setExecutable(true);
        try {
            //            String jppfNodePath = startNodeScript.getParentFile().getAbsolutePath();
            String jvmOptions = localNodeConfiguration.getJvmOptions().or("").replaceAll("\\s", "\\ ");
            System.out.println("jvmOptions = "+jvmOptions);
            
            final String command = String.format("%s %s %s %s %s",
                    "./" + startNodeScript.getName(), 
                    currentNodeConfig,
                    "true", "false", jvmOptions);
            logger.info("Start node with {}", command);
            nodeProcess = Runtime.getRuntime().exec(command, new String[]{}, startNodeScript.getParentFile());
//            Process exec = Runtime.getRuntime().exec(command);
            InputStream stdOutInputStream = nodeProcess.getInputStream();
            InputStream stdErrInputStream = nodeProcess.getErrorStream();
            //            CharStreams.class;
            BufferedReader stdOutReader = new BufferedReader(new InputStreamReader(stdOutInputStream));
            BufferedReader stdErrReader = new BufferedReader(new InputStreamReader(stdErrInputStream));
//            nodeProcessOutputterStdOut = new NodeProcessOutputter(stdOutReader, "STDOUT");
            nodeProcessOutputterStdOutToFile = new NodeProcessOutputterToFile(stdOutReader, "STDOUT", createLogFile("node.stdout"));
            nodeProcessOutputterStdOutToFile.start();
//            nodeProcessOutputterStdErr = new NodeProcessOutputter(stdErrReader, "STDERR");
            nodeProcessOutputterStdErrToFile = new NodeProcessOutputterToFile(stdErrReader, "STDERR", createLogFile("node.stderr"));
            nodeProcessOutputterStdErrToFile.start();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private File createLogFile(String name) {
        File logFile = new File(targetDir, name + "." + currentNodeNumber);
        try {
            logFile.createNewFile();
        } catch (IOException ex) {
            log("Could not create {}", logFile.getAbsolutePath(), ex);
        }
        return logFile;
    }
    
    public void shutdown() {
        JPPFManagementByJobsClient client = JPPFConfiguratedComponentFactory.getInstance().createManagementByJobsClient("localhost", 11111);
        try {
            client.shutdownAllNodes();
            nodeProcessOutputterStdOutToFile.close();
            nodeProcessOutputterStdErrToFile.close();
            FileUtils.deleteDirectory(targetDir);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            client.close();
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
    
    private synchronized void log(String message, String... args) {
//        logger.info(message, args);
    	System.out.println(String.format(message.replaceAll("\\{\\}", "%s"), (Object[])args));
    }
    
    private synchronized void log(String message, String arg, Exception ex) {
//        logger.info(message, arg, ex);
        System.out.println(String.format(message.replaceAll("\\{\\}", "%s"), arg));
        System.out.println("Exception: "+ex.getMessage());
        ex.printStackTrace();
    }
    
    public class NodeProcessOutputter extends Thread {
        
        private BufferedReader reader;
        private final String logMarker;

        private NodeProcessOutputter(BufferedReader reader, String logMarker) {
            this.reader = reader;
            this.logMarker = logMarker;
        }

        @Override
        public void run() {
            log("{} started.", getClass().getName());
            try {
                String nextLine;
                while ((nextLine = reader.readLine()) != null) {
                    log("{}: {}", logMarker, nextLine);
                }
            } catch (IOException ex) {
                log("IOException while printing {}.", logMarker, ex);
            }
        }

        public void close() {
            try {
                this.reader.close();
            } catch (IOException ex) {
                // ignore
            }
        }
        
    }
    
    public class NodeProcessOutputterToFile extends Thread {
        
        private final BufferedReader reader;
        private final String logMarker;
        private final FileWriter fileWriter;

        private NodeProcessOutputterToFile(BufferedReader reader, String logMarker, File logToFile) throws IOException {
            this.reader = reader;
            this.logMarker = logMarker;
            fileWriter = new FileWriter(logToFile);
        }

        @Override
        public void run() {
            logToFile("{} started.", getClass().getName());
            try {
                String nextLine;
                while ((nextLine = reader.readLine()) != null) {
                    logToFile("{}: {}", logMarker, nextLine);
                }
            } catch (IOException ex) {
                log("IOException while printing {}.", logMarker, ex);
            }
        }

        public void close() {
            try {
                this.reader.close();
                this.fileWriter.close();
            } catch (IOException ex) {
                // ignore
            }
        }

        private void logToFile(String message, String... args) {
            try {
                fileWriter.append(String.format(message.replaceAll("\\{\\}", "%s"), (Object[]) args));
            } catch (IOException ex) {
                log("Could not write to log file", null, ex);
            }
        }

        private void logToFile(String message, String arg, Exception ex) {
            try {
                fileWriter.append(String.format(message.replaceAll("\\{\\}", "%s"), arg));
                fileWriter.append("Exception: " + ex.getMessage());
                ex.printStackTrace(new PrintWriter(fileWriter));
            } catch (IOException ex1) {
                log(message, arg, ex);
            }
        }
    }
    
}
