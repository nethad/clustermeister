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
import com.github.nethad.clustermeister.api.impl.FileConfiguration;
import com.github.nethad.clustermeister.provisioning.ec2.AmazonAPIManageNodes;
import com.github.nethad.clustermeister.provisioning.utils.SSHClient;
import com.github.nethad.clustermeister.provisioning.utils.SSHClientExcpetion;

/**
 *
 * @author thomas
 */
public class TorqueJPPFDriverDeployer {
    
    private static final String DEPLOY_BASE_NAME = "jppf-driver";
    private static final String DEPLOY_ZIP = DEPLOY_BASE_NAME+".zip";
    private static final String DEPLOY_PROPERTIES = DEPLOY_BASE_NAME+".properties";

    private String host;
    private int port;
    private SSHClient sshClient;
    private String user;
    private String privateKeyFilePath;
//    private String passphrase;

    public static void main(String... args) {
        new TorqueJPPFDriverDeployer().execute();
    }

    public void execute() {
        sshClient = null;
        try {
            loadConfiguration();

            sshClient = new SSHClient(privateKeyFilePath);
            sshClient.connect(user, host, port);
            executeAndSysout("rm -rf "+DEPLOY_BASE_NAME+"*");

            sshClient.sftpUpload(getResourcePath(DEPLOY_ZIP), DEPLOY_ZIP);
            executeAndSysout("unzip "+DEPLOY_ZIP);

            sshClient.sftpUpload(getResourcePath(DEPLOY_PROPERTIES), DEPLOY_BASE_NAME+"/config/"+DEPLOY_PROPERTIES);
            executeAndSysout("chmod +x "+DEPLOY_BASE_NAME+"/startDriver.sh");
            
            // assume java is installed (installed in ~/jdk-1.7)
//            executeAndSysout("cp -R /home/user/dspicar/jdk-1.7 ~/jdk-1.7");

            executeAndSysout("cd "+DEPLOY_BASE_NAME+";nohup ./startDriver.sh ~/jdk-1.7/bin/java > nohup.out 2>&1");
        } catch (SSHClientExcpetion ex) {
            ex.printStackTrace();
        } finally {
            if (sshClient != null) {
                sshClient.disconnect();
            }
        }
    }

    private String getResourcePath(String resource) {
        return TorqueJPPFDriverDeployer.class.getResource(resource).getPath();
    }

    private void executeAndSysout(String command) throws SSHClientExcpetion {
        String result = sshClient.sshExec(command, System.err);
        System.out.println("Result: " + result);
    }

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
}
