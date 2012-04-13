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

import com.github.nethad.clustermeister.api.JPPFConstants;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFConfiguratedComponentFactory;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFManagementByJobsClient;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFNodeConfiguration;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas
 */
public class JPPFTestNode {
    private final Logger logger = LoggerFactory.getLogger(JPPFTestNode.class);
    
    private static final String AKKA_ZIP = "akka-libs.tar.bz2";
    
    private Process nodeProcess;
    private NodeProcessOutputter nodeProcessOutputterStdOut;
    private NodeProcessOutputter nodeProcessOutputterStdErr;
//    private File tempDir;
    private File targetDir;
    
    private int managementPort = 12001;
    private int currentNodeNumber = 0;
    private String currentNodeConfig;
    private File libDir;
    
    public void prepare() {
        unpackNodeZip();
        libDir = new File(targetDir, "jppf-node/lib/");
        preloadLibraries();
    }
    
    public void startNewNode() {
        prepareNodeConfiguration();
        startNode();
    }

    public void prepareNodeConfiguration() throws RuntimeException {
        JPPFNodeConfiguration nodeConfiguration = new JPPFNodeConfiguration();
        nodeConfiguration.setProperty(JPPFConstants.SERVER_HOST, "localhost")
                .setProperty(JPPFConstants.MANAGEMENT_PORT, String.valueOf(managementPort++))
                .setProperty(JPPFConstants.RESOURCE_CACHE_DIR, "/tmp/.jppf/node-" + System.currentTimeMillis())
                .setProperty(JPPFConstants.PROCESSING_THREADS, String.valueOf(4));
        
        currentNodeConfig = "config/jppf-node-"+(currentNodeNumber++)+".properties";
        final File propertiesFile = new File(targetDir, "/jppf-node/"+currentNodeConfig);
        try {
            InputStream propertyStream = nodeConfiguration.getPropertyStream();
            copyInputStream(propertyStream, new FileOutputStream(propertiesFile));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void unpackNodeZip() throws RuntimeException {
        String currentDirPath = System.getProperty("user.dir");
        targetDir = getTargetDir(currentDirPath);
        InputStream jppfNodeZipStream = JPPFTestNode.class.getResourceAsStream("/jppf-node.zip");
        if (jppfNodeZipStream == null) {
            throw new RuntimeException("Could not find jppf-node.zip.");
        }
        unzipNode(jppfNodeZipStream, targetDir);
    }

    private void startNode() throws RuntimeException {
        File startNodeScript = new File(targetDir, "jppf-node/startNode.sh");
        startNodeScript.setExecutable(true);
        try {
//            String jppfNodePath = startNodeScript.getParentFile().getAbsolutePath();
            final String command = String.format("%s %s %s",
                    "./" + startNodeScript.getName(), 
                    currentNodeConfig,
                    "true");
            logger.info("Start node with {}", command);
            nodeProcess = Runtime.getRuntime().exec(command, new String[]{}, startNodeScript.getParentFile());
//            Process exec = Runtime.getRuntime().exec(command);
            InputStream stdOutInputStream = nodeProcess.getInputStream();
            InputStream stdErrInputStream = nodeProcess.getErrorStream();
            //            CharStreams.class;
            BufferedReader stdOutReader = new BufferedReader(new InputStreamReader(stdOutInputStream));
            BufferedReader stdErrReader = new BufferedReader(new InputStreamReader(stdErrInputStream));
            nodeProcessOutputterStdOut = new NodeProcessOutputter(stdOutReader, "STDOUT");
            nodeProcessOutputterStdOut.start();
            nodeProcessOutputterStdErr = new NodeProcessOutputter(stdErrReader, "STDERR");
            nodeProcessOutputterStdErr.start();
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
    
    private void preloadLibraries() {
        String akkaZip = System.getProperty("user.home")+"/.clustermeister/"+AKKA_ZIP;
        File akkaZipFile = new File(akkaZip);
        File tarFile = new File(libDir, "archive.tar");
        boolean akkaZipFileExists = akkaZipFile.exists();
        boolean tarFileNotAlreadyExists = !tarFile.exists();
        if (akkaZipFileExists && tarFileNotAlreadyExists) {
            try {
                extractBzip2(akkaZipFile, tarFile);
                untar(tarFile);
            } catch (IOException ex) {
                logger.warn("Could not untar libraries (preloading)", ex);
            }
        }
    }
    
    private void untar(File tarFile) throws IOException {
        logger.info("Untar {}.", tarFile.getAbsolutePath());
        TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(new FileInputStream(tarFile));
        try {
            ArchiveEntry tarEntry = tarArchiveInputStream.getNextEntry();
            while (tarEntry != null) {
                File destPath = new File(libDir, tarEntry.getName());
                logger.info("Unpacking {}.", destPath.getAbsoluteFile());
                if (!tarEntry.isDirectory()) {
                    FileOutputStream fout = new FileOutputStream(destPath);
                    final byte[] buffer = new byte[8192];
                    int n = 0;
                    while (-1 != (n = tarArchiveInputStream.read(buffer))) {
                        fout.write(buffer, 0, n);
                    }
                    fout.close();

                } else {
                    destPath.mkdir();
                }
                tarEntry = tarArchiveInputStream.getNextEntry();
            }
        } finally {
            tarArchiveInputStream.close();
        }        
    }
    
    private File extractBzip2(File bzip2File, File tarFile) throws FileNotFoundException, IOException {
        FileInputStream fin = null;
        BZip2CompressorInputStream bzIn = null;
        try {
            fin = new FileInputStream(bzip2File);
            BufferedInputStream in = new BufferedInputStream(fin);
            FileOutputStream out = new FileOutputStream(tarFile);
            bzIn = new BZip2CompressorInputStream(in);
            final byte[] buffer = new byte[8192];
            int n = 0;
            while (-1 != (n = bzIn.read(buffer))) {
                out.write(buffer, 0, n);
            }
            out.close();
            bzIn.close();
        }  finally {
            try {
                fin.close();
            } catch (IOException ex) {
//                throw new RuntimeException(ex);
            }
            try {
                bzIn.close();
            } catch (IOException ex) {
//                throw new RuntimeException(ex);
            }
        }
        return tarFile;
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
    
    private synchronized void log(String message, String... args) {
        logger.info(message, args);
    }
    
    private synchronized void log(String message, String arg, Exception ex) {
        logger.info(message, arg, ex);
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
    
}
