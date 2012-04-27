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
package com.github.nethad.clustermeister.provisioning.utils;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.Before;
import org.junit.BeforeClass;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

/**
 *
 * @author thomas
 */
public class UploadUtilTest {
    private SSHClient sshClient;
    private UploadUtil uploadUtil;
    
    @Before
    public void setup() {
        sshClient = mock(SSHClient.class);
        uploadUtil = new UploadUtil(sshClient);
    }
    
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
