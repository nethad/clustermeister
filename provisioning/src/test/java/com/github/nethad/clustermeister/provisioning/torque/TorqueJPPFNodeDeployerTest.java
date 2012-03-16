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
import com.github.nethad.clustermeister.provisioning.utils.SSHClient;
import java.util.HashMap;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import static org.mockito.Mockito.*;
import static org.hamcrest.CoreMatchers.*;

/**
 *
 * @author thomas
 */
public class TorqueJPPFNodeDeployerTest {
    private SSHClient sshClient;
    private TorqueJPPFNodeDeployer torqueJPPFNodeDeployer;

    @Before
    public void setup() {
        Map<String, Object> configValues = new HashMap<String, Object>();
        configValues.put(Configuration.TORQUE_EMAIL_NOTIFY, "test@example.com");
        configValues.put(Configuration.TORQUE_SSH_HOST, "ssh.example.com");
        configValues.put(Configuration.TORQUE_SSH_PORT, 30);
        configValues.put(Configuration.TORQUE_SSH_PRIVATEKEY, "/path/to/the/private/key");
        configValues.put(Configuration.TORQUE_SSH_USER, "user");
        ConfigurationForTesting configuration = new ConfigurationForTesting(configValues);
        sshClient = mock(SSHClient.class);
        torqueJPPFNodeDeployer = new TorqueJPPFNodeDeployer(configuration, sshClient) {
            @Override
            boolean isResourceAlreadyDeployedAndUpToDate() {
                return true;
            }
        };
    }
    
    @Test
    public void deployInfrastructure() throws Exception {
        assertThat(torqueJPPFNodeDeployer.isInfrastructureDeployed(), is(false));
        
        torqueJPPFNodeDeployer.deployInfrastructure();
        
        verify(sshClient).connect(eq("user"), eq("ssh.example.com"), eq(30));
        verify(sshClient).executeAndSysout(eq("rm -rf jppf-node/config/*.properties"));
        assertThat(torqueJPPFNodeDeployer.isInfrastructureDeployed(), is(true));
    }
}
