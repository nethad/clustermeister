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
package com.github.nethad.clustermeister.provisioning.torque;

import com.github.nethad.clustermeister.api.Configuration;
import com.github.nethad.clustermeister.api.NodeType;
import com.github.nethad.clustermeister.api.impl.FileConfiguration;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFConfiguratedComponentFactory;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFDriverConfigurationSource;
import com.github.nethad.clustermeister.provisioning.utils.PublicIp;
import com.github.nethad.clustermeister.provisioning.utils.SSHClient;
import com.github.nethad.clustermeister.provisioning.utils.SSHClientException;
import com.github.nethad.clustermeister.provisioning.utils.SSHClientImpl;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.process.ProcessLauncher;

/**
 *
 * @author thomas
 */
public class TorqueJPPFDriverDeployer {
	
	public static final int SERVER_PORT = 11111;
	private static final int MANAGEMENT_PORT = 11198;

    private static final String DEPLOY_BASE_NAME = "jppf-driver";
    private static final String DEPLOY_ZIP = DEPLOY_BASE_NAME + ".zip";
    private static final String DEPLOY_PROPERTIES = DEPLOY_BASE_NAME + ".properties";
    private String host;
    private int port;
    private ProcessLauncher processLauncher;
    private SSHClient sshClient;
    private String user;
    private String privateKeyFilePath;
//    private String passphrase;
    private boolean runExternally = false;
	private TorqueNodeManagement torqueNodeManagement;

    public TorqueNode execute(TorqueNodeManagement torqueNodeManagement) {
		this.torqueNodeManagement = torqueNodeManagement;
        if (runExternally) {
            return remoteSetupAndRun();
        } else {
            return localSetupAndRun();
        }
    }

    private TorqueNode remoteSetupAndRun() {
	loadConfiguration();
        sshClient = null;
        try {

            sshClient = new SSHClientImpl(privateKeyFilePath);
            sshClient.connect(user, host, port);
            sshClient.executeAndSysout("rm -rf " + DEPLOY_BASE_NAME + "*");

            sshClient.sftpUpload(getResourcePath(DEPLOY_ZIP), DEPLOY_ZIP);
            sshClient.executeAndSysout("unzip " + DEPLOY_ZIP);

            sshClient.sftpUpload(getResourcePath(DEPLOY_PROPERTIES), DEPLOY_BASE_NAME + "/config/" + DEPLOY_PROPERTIES);
            sshClient.executeAndSysout("chmod +x " + DEPLOY_BASE_NAME + "/startDriver.sh");

            // assume java is installed (installed in ~/jdk-1.7)
//            executeAndSysout("cp -R /home/user/dspicar/jdk-1.7 ~/jdk-1.7");

            sshClient.executeAndSysout("cd " + DEPLOY_BASE_NAME + ";nohup ./startDriver.sh ~/jdk-1.7/bin/java > nohup.out 2>&1");
			// TODO this is a dummy initialization
			final TorqueNode torqueNode = new TorqueNode(NodeType.DRIVER, null, null, null, SERVER_PORT, MANAGEMENT_PORT);
			torqueNodeManagement.addManagedNode(torqueNode);
			return torqueNode;
        } catch (SSHClientException ex) {
            ex.printStackTrace();
        } finally {
            if (sshClient != null) {
                sshClient.disconnect();
            }
        }
		return null;
    }

    private String getResourcePath(String resource) {
        return TorqueJPPFDriverDeployer.class.getResource(resource).getPath();
    }

//    private void executeAndSysout(String command) throws SSHClientException {
//        String result = sshClient.sshExec(command, System.err);
//        System.out.println("Result: " + result);
//    }

    private void loadConfiguration() {
        String home = System.getProperty("user.home");
        String separator = System.getProperty("file.separator");
        Configuration config = new FileConfiguration(home + separator + ".clustermeister" + separator + "torque.properties");

        host = getStringDefaultempty(config, "host");
        port = config.getInt("port", 22);
        user = getStringDefaultempty(config, "user");
        privateKeyFilePath = getStringDefaultempty(config, "privateKey");
//        passphrase = getStringDefaultempty(config, "passphrase");

    }

    private String getStringDefaultempty(Configuration config, String key) {
        return config.getString(key, "");
    }

    public TorqueJPPFDriverDeployer runExternally() {
        runExternally = true;
        return this;
    }

    private TorqueNode localSetupAndRun() {
//		System.setProperty("jppf.config.plugin", JPPFDriverConfigurationSource.class.getCanonicalName());
//        processLauncher = new ProcessLauncher("org.jppf.server.JPPFDriver");
//        processLauncher.run();
		JPPFConfiguratedComponentFactory.getInstance().createLocalDriver(SERVER_PORT, MANAGEMENT_PORT);
		String publicIp = PublicIp.getPublicIp();
		TorqueNode torqueNode = new TorqueNode(NodeType.DRIVER, null, publicIp, "localhost", SERVER_PORT, MANAGEMENT_PORT);
		torqueNodeManagement.addManagedNode(torqueNode);
		return torqueNode;
		//        try {
		//            Process process = processLauncher.buildProcess();
		//            process.
		//        } catch (Exception ex) {
		//            Logger.getLogger(TorqueJPPFDriverDeployer.class.getName()).log(Level.SEVERE, null, ex);
		//        }
    }

//    private void stopLocalDriver() {
//        JMXDriverConnectionWrapper wrapper = new JMXDriverConnectionWrapper("localhost", 11198);
//        wrapper.connect();
//        try {
//            wrapper.restartShutdown(1L, -1L);
//        } catch (Exception ex) {
//            Logger.getLogger(TorqueJPPFTestSetup.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
    
}
