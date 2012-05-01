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
import java.util.Collection;
import java.util.LinkedList;
import static org.hamcrest.Matchers.*;
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
public class UploadUtilTest {
    private SSHClient sshClient;
    private UploadUtil uploadUtil;
    
    @Before
    public void setup() throws SSHClientException {
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
        uploadUtil = new UploadUtil(sshClient);
    }
    
    @Test
    public void delteConfigFiles() throws SSHClientException {
        uploadUtil.deleteConfigurationFiles();
        verify(sshClient).executeAndSysout(eq("rm -rf jppf-node/config/jppf-node-*.properties"));
    }
    
    @Test
    public void deployInfrastructure() throws Exception {
        final Collection<File> artifactsToPreload = new LinkedList<File>();
        uploadUtil.deployInfrastructure(artifactsToPreload);

        verify(sshClient).executeAndSysout(eq("rm -rf jppf-node*"));
        assertThat(uploadUtil.getArtifactsToPreload(), sameInstance(artifactsToPreload));
    }

}
