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

import com.github.nethad.clustermeister.api.JPPFConstants;
import com.github.nethad.clustermeister.api.NodeType;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFNodeConfiguration;
import com.github.nethad.clustermeister.provisioning.utils.SSHClient;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.configuration.Configuration;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author thomas
 */
public class NodeDeployTaskTest {
    public static final int NUMBER_OF_CPUS = 42;

    private NodeDeployTask nodeDeployTask;
    private SSHClient sshClient;
    private TorqueNodeConfiguration torqueNodeConfiguration;
    private TorqueNodeDeployment torqueNodeDeployment;
    private TorqueConfiguration torqueConfiguration;

    @Before
    public void setup() throws ConfigurationValueMissingException {
        torqueNodeDeployment = mock(TorqueNodeDeployment.class);
        sshClient = mock(SSHClient.class);
        when(torqueNodeDeployment.sshClient()).thenReturn(sshClient);
        torqueNodeConfiguration = new TorqueNodeConfiguration("driverIp", NUMBER_OF_CPUS, null);
        torqueConfiguration = TorqueConfiguration.buildFromConfig(buildConfig());
        nodeDeployTask = new NodeDeployTask(torqueNodeDeployment, 10, torqueNodeConfiguration, torqueConfiguration);
    }
    
    private Configuration buildConfig(boolean isQueueSet) {
        Map<String, Object> configValues = new HashMap<String, Object>();
        configValues.put(TorqueConfiguration.TORQUE_SSH_HOST, "ssh.example.com");
        configValues.put(TorqueConfiguration.TORQUE_SSH_PORT, 22);
        configValues.put(TorqueConfiguration.TORQUE_SSH_PRIVATEKEY, "/path/to/privatekey");
        configValues.put(TorqueConfiguration.TORQUE_SSH_USER, "user");
        if (isQueueSet) {
            configValues.put(TorqueConfiguration.TORQUE_QUEUE_NAME, "myqueue");
        }
        configValues.put(TorqueConfiguration.TORQUE_EMAIL_NOTIFY, "test@example.com");
        return new ConfigurationForTesting(configValues);
    }
    
    private Configuration buildConfig() {
        return buildConfig(true);
    }

    @Test
    public void base64Encode() {
        String toEncode = "Hello World";
        String result = nodeDeployTask.base64Encode(toEncode);
        assertEquals("SGVsbG8gV29ybGQ=", result);
    }

    @Test
    public void isValidEmail() {
        assertThat(nodeDeployTask.isValidEmail(null), is(false));
        assertThat(nodeDeployTask.isValidEmail(""), is(false));
        assertThat(nodeDeployTask.isValidEmail("test"), is(false));
        assertThat(nodeDeployTask.isValidEmail("test.test.com"), is(false));
        assertThat(nodeDeployTask.isValidEmail("test@examplecom"), is(false));
        assertThat(nodeDeployTask.isValidEmail("test.test@examplecom"), is(false));
        assertThat(nodeDeployTask.isValidEmail("test@example.com"), is(true));
        assertThat(nodeDeployTask.isValidEmail("test.test@example.com"), is(true));
    }
    
    @Test
    public void createNodeConfiguration() {
        JPPFNodeConfiguration nodeConfiguration = nodeDeployTask.createNodeConfiguration("driverIp");
        assertThat(nodeConfiguration.getProperty(JPPFConstants.SERVER_HOST), is("driverIp"));
        assertThat(nodeConfiguration.getProperty(JPPFConstants.MANAGEMENT_PORT), is(String.valueOf(TorqueNodeDeployment.DEFAULT_MANAGEMENT_PORT+10)));
        assertThat(nodeConfiguration.getProperty(JPPFConstants.PROCESSING_THREADS), is(String.valueOf(NUMBER_OF_CPUS)));
    }

    @Test
    public void uploadNodeConfiguration() throws Exception {
        nodeDeployTask.uploadNodeConfiguration("config", "driverIp");
        verify(sshClient).sftpUpload(any(InputStream.class), eq("jppf-node/config/config"));
    }
    
    @Test
    public void execute() throws Exception {
        when(sshClient.executeWithResult(anyString())).thenReturn("42");
        nodeDeployTask.execute();
        verify(sshClient).executeWithResult(contains("qsub"));
//        assertThat(torqueNode.getTorqueJobId(), is("42"));
//        assertThat(torqueNode.getManagementPort(), is(TorqueNodeDeployment.DEFAULT_MANAGEMENT_PORT+10));
//        assertThat(torqueNode.getType(), is(NodeType.NODE));
    }
    
    @Test
    public void qsubScript() throws Exception {
        String qsubScript = nodeDeployTask.qsubScript("NodeName", "node-config.properties", NUMBER_OF_CPUS);
        assertThat(qsubScript, containsString("#PBS -l nodes=1:ppn=42"));
        assertThat(qsubScript, containsString("#PBS -q myqueue"));
        assertThat(qsubScript, containsString("#PBS -o out/NodeName.out"));
        assertThat(qsubScript, containsString("#PBS -M test@example.com"));
        assertThat(qsubScript, containsString("./startNode.sh node-config.properties"));
    }
    
    @Test
    public void qsubScript_defaultValues() throws Exception {
        torqueConfiguration = TorqueConfiguration.buildFromConfig(buildConfig(false));
        nodeDeployTask = new NodeDeployTask(torqueNodeDeployment, 10, torqueNodeConfiguration, torqueConfiguration);
        String qsubScript = nodeDeployTask.qsubScript("NodeName", "node-config.properties", NUMBER_OF_CPUS);
        assertThat(qsubScript, containsString("#PBS -l nodes=1:ppn=42"));
        assertThat(qsubScript, containsString("#PBS -q superfast"));
        assertThat(qsubScript, containsString("#PBS -o out/NodeName.out"));
        assertThat(qsubScript, containsString("#PBS -M test@example.com"));
        assertThat(qsubScript, containsString("./startNode.sh node-config.properties"));
    }
}
