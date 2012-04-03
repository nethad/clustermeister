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

import com.github.nethad.clustermeister.api.Clustermeister;
import com.github.nethad.clustermeister.api.ExecutorNode;
import com.github.nethad.clustermeister.api.impl.ClustermeisterFactory;
import com.github.nethad.clustermeister.integration.Assertions;
import com.github.nethad.clustermeister.node.common.ClustermeisterLauncher;
import com.github.nethad.clustermeister.provisioning.cli.Provider;
import com.github.nethad.clustermeister.provisioning.cli.Provisioning;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFNodeConfiguration;
import com.github.nethad.clustermeister.provisioning.rmi.NodeConnectionListener;
import com.github.nethad.clustermeister.provisioning.rmi.RmiInfrastructure;
import com.github.nethad.clustermeister.provisioning.rmi.RmiServerForDriver;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.google.common.util.concurrent.ListenableFuture;
import com.sun.imageio.spi.OutputStreamImageOutputStreamSpi;
import java.io.*;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.management.JPPFSystemInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas
 */
public class Scenario02 implements NodeConnectionListener {
    private final Logger logger = LoggerFactory.getLogger(Scenario02.class);
    
    public static void main(String... args) throws InterruptedException {
        new Scenario02().execute();
    }
    private Provisioning provisioning;

    private void execute() throws InterruptedException {
        
        String configFilePath = System.getProperty("user.home") + "/.clustermeister/configuration.properties";
        provisioning = new Provisioning(configFilePath, Provider.TORQUE);
        RmiInfrastructure rmiInfrastructure = provisioning.getRmiInfrastructure();
        RmiServerForDriver rmiServerForDriver = rmiInfrastructure.getRmiServerForDriverObject();
        rmiServerForDriver.addNodeConnectionListener(this);
        provisioning.execute();
        startNode();
    }

    private void runScenario() throws InterruptedException, RuntimeException {
        try {
            logger.info("Start Clustermeister.");
            Clustermeister clustermeister = ClustermeisterFactory.create();
            Collection<ExecutorNode> allNodes = clustermeister.getAllNodes();
            logger.info("nodes size = {}", allNodes.size());
            Assertions.assertEquals(1, allNodes.size(), "Number of nodes not as expected");
            if (allNodes.size() > 0) {
                ListenableFuture<String> result = allNodes.iterator().next().execute(new ReturnStringCallable("it works!"));
                try {
                    String resultString = result.get();
                    Assertions.assertEquals("it works!", resultString, "Result string is not as expected.");
                } catch (ExecutionException ex) {
                    throw new RuntimeException(ex);
                }
            }
        } finally {
            provisioning.shutdown();
        }
    }

    private void startNode() throws RuntimeException {
        String currentDirPath = System.getProperty("user.dir");
        File nodeDir = getNodeDir(new File(currentDirPath));
        File jppfNodeZip = new File(nodeDir, "target/jppf-node.zip");
        if (!jppfNodeZip.exists()) {
            throw new RuntimeException("Could not find jppf-node.zip.");
        }
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        if (!tempDir.exists()) {
            throw new RuntimeException("Temp directory does not exist");
        }
        unzipNode(jppfNodeZip, tempDir);
        JPPFNodeConfiguration nodeConfiguration = new JPPFNodeConfiguration();
        nodeConfiguration.setProperty("jppf.server.host", "localhost")
                .setProperty("jppf.management.port", String.valueOf(12001))
                .setProperty("jppf.resource.cache.dir", "/tmp/.jppf/node-" + System.currentTimeMillis())
                .setProperty("processing.threads", String.valueOf(1));
        
            final File propertiesFile = new File(tempDir, "/jppf-node/config/jppf-node.properties");
        try {
            InputStream propertyStream = nodeConfiguration.getPropertyStream();
            copyInputStream(propertyStream, new FileOutputStream(propertiesFile));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        
        File startNodeScript = new File(tempDir, "jppf-node/startNode.sh");
        startNodeScript.setExecutable(true);
        try {
//            String jppfNodePath = startNodeScript.getParentFile().getAbsolutePath();
            final String command = String.format("%s %s %s",
                    "./" + startNodeScript.getName(), 
                    "config/jppf-node.properties",
                    "true");
            logger.info("Start node with {}", command);
            Process exec = Runtime.getRuntime().exec(command, new String[]{}, startNodeScript.getParentFile());
//            Process exec = Runtime.getRuntime().exec(command);
            InputStream inputStream = exec.getInputStream();
            //            CharStreams.class;
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String nextLine;
//            
            while ((nextLine = reader.readLine()) != null) {
                System.out.println("process line ==> "+nextLine);
                if (nextLine.contains("Got UUID.")) {
                    break;
                }
            }
//            int c;
//            while ((c = inputStream.read()) != -1) {
//                System.out.print((char) c);
//            }
            // Got UUID.
            inputStream.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
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

    @Override
    public void onNodeConnected(JPPFManagementInfo jppfmi, JPPFSystemInformation jppfsi) {
        try {
            runScenario();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
        System.exit(0);
    }

    @Override
    public void onNodeDisconnected(JPPFManagementInfo jppfmi) {
        // do nothing
    }

}
