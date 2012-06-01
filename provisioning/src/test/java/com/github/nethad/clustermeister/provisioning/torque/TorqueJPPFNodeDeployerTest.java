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

import com.github.nethad.clustermeister.provisioning.utils.SSHClient;
import com.github.nethad.clustermeister.provisioning.utils.SSHClientException;
import java.io.File;
import java.io.InputStream;
import java.util.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 *
 * @author thomas
 */
public class TorqueJPPFNodeDeployerTest {
    private SSHClient sshClient;
    private TorqueJPPFNodeDeployer torqueJPPFNodeDeployer;
    
    static final String PACKAGE_PREFIX = "/com/github/nethad/clustermeister/provisioning/torque";
    private String notifiedIp;

    @Before
    public void setup() throws ConfigurationValueMissingException, SSHClientException {
        notifiedIp = null;
        Map<String, Object> configValues = new HashMap<String, Object>();
        configValues.put(TorqueConfiguration.TORQUE_EMAIL_NOTIFY, "test@example.com");
        configValues.put(TorqueConfiguration.TORQUE_SSH_HOST, "ssh.example.com");
        configValues.put(TorqueConfiguration.TORQUE_SSH_PORT, 30);
        configValues.put(TorqueConfiguration.TORQUE_SSH_PRIVATEKEY, "/path/to/the/private/key");
        configValues.put(TorqueConfiguration.TORQUE_SSH_USER, "user");
        TorqueConfiguration configuration = TorqueConfiguration.buildFromConfig(new ConfigurationForTesting(configValues));
        sshClient = mock(SSHClient.class);
        when(sshClient.executeWithResultSilent(anyString())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                String arg = (String) invocation.getArguments()[0];
                if(arg.contains("then echo true; else echo false")) {
                    return "true";
                } else {
                    return "12345678";
                }
            }
        });
        torqueJPPFNodeDeployer = new TorqueJPPFNodeDeployer(configuration, sshClient) {

            @Override
            void deployInfrastructure(Collection<File> artifactsToPreload) {
                // do nothing
            }
            
//            @Override
//            InputStream getResourceStream(String resource) {
//                if(resource.contains("node")) {
//                    return super.getResourceStream(PACKAGE_PREFIX + resource);
//                } else {
//                    return super.getResourceStream(resource);
//                }
//            }
        };
    }
    
    @Test
    public void deployInfrastructure() throws Exception {
        assertThat(torqueJPPFNodeDeployer.isInfrastructureDeployed(), is(false));
        
        torqueJPPFNodeDeployer.prepareAndDeployInfrastructure(new LinkedList<File>());
        
        verify(sshClient).connect(eq("ssh.example.com"), eq(30));
        assertThat(torqueJPPFNodeDeployer.isInfrastructureDeployed(), is(true));
    }
    
    @Test
    public void doPublicIpRequest() throws Exception {
        when(sshClient.executeWithResult(eq("echo $SSH_CLIENT"))).thenReturn("1.2.3.5 123 456");
        torqueJPPFNodeDeployer.addListener(new Observer() {

            @Override
            public void update(Observable o, Object arg) {
                notifiedIp = (String)arg;
            }
        });
        verify(sshClient).executeWithResult(eq("echo $SSH_CLIENT"));
        assertThat(notifiedIp, is("1.2.3.5"));
    }
    
    private void setNotifiedIp(String ip) {
        this.notifiedIp = ip;
    }
}
