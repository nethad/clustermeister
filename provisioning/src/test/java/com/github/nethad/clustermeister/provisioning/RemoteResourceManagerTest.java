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
package com.github.nethad.clustermeister.provisioning;

import com.github.nethad.clustermeister.provisioning.utils.SSHClient;
import com.github.nethad.clustermeister.provisioning.utils.SSHClientException;
import com.google.common.base.Charsets;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import static org.hamcrest.Matchers.*;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import static org.mockito.Matchers.*;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

/**
 * Tests for RemoteResourceManager.
 *
 * @author daniel
 */
@RunWith(MockitoJUnitRunner.class)
public class RemoteResourceManagerTest {
    private static final String DUMMY_RESOURCE_CONTENT = "Hello World";
    private static final ByteArrayInputStream DUMMY_RESOURCE_DATA = 
            new ByteArrayInputStream(DUMMY_RESOURCE_CONTENT.getBytes(Charsets.UTF_8));
    private static final String UPLOADED_UNDEPLOYED_RESOURCE_NAME = "uploadedUndeployed.txt";
    private static final long DUMMY_RESOURCE_CHECKSUM = 123456789l;
    private static final String DUMMY_RESOURCE_DEPLOYMENT_DIR = "remote~directory";
    private static final String DUMMY_RESOURCE_NAME = "dummyResource.txt";
    private static final String RESOURCE_DIR_NAME = "resources";
    private static final String RESOURCE_DIR_PATH = "path~to~resources";
    private static final String SEPARATOR = "~";
    
    private RemoteResourceManager resourceManager;
    
    @Mock
    private SSHClient sshClient;
    
    @Mock
    private Resource dummyResource;
    
    @Mock
    private Resource dummyResource2;
    
    @Mock
    private Resource dummyResource3;
    
    @Mock
    private Resource uploadedUndeployedResource;
    
    @Before
    public void setUp() throws IOException, SSHClientException {
        
        resourceManager = new RemoteResourceManager(sshClient, RESOURCE_DIR_PATH, 
                RESOURCE_DIR_NAME, SEPARATOR);
        when(dummyResource.getName()).thenReturn(DUMMY_RESOURCE_NAME);
        when(dummyResource.getRemoteDeploymentDirectory()).thenReturn(DUMMY_RESOURCE_DEPLOYMENT_DIR);
        when(dummyResource.getResourceChecksum()).thenReturn(DUMMY_RESOURCE_CHECKSUM);
        when(dummyResource.getResourceData()).thenReturn(DUMMY_RESOURCE_DATA);
        
        when(dummyResource2.getName()).thenReturn(DUMMY_RESOURCE_NAME + 2);
        when(dummyResource2.getRemoteDeploymentDirectory()).thenReturn(DUMMY_RESOURCE_DEPLOYMENT_DIR);
        when(dummyResource2.getResourceChecksum()).thenReturn(DUMMY_RESOURCE_CHECKSUM);
        when(dummyResource2.getResourceData()).thenReturn(DUMMY_RESOURCE_DATA);
        
        when(dummyResource3.getName()).thenReturn(DUMMY_RESOURCE_NAME + 3);
        when(dummyResource3.getRemoteDeploymentDirectory()).thenReturn(DUMMY_RESOURCE_DEPLOYMENT_DIR);
        when(dummyResource3.getResourceChecksum()).thenReturn(DUMMY_RESOURCE_CHECKSUM);
        when(dummyResource3.getResourceData()).thenReturn(DUMMY_RESOURCE_DATA);
        
        when(uploadedUndeployedResource.getName()).thenReturn(UPLOADED_UNDEPLOYED_RESOURCE_NAME);
        when(uploadedUndeployedResource.getRemoteDeploymentDirectory()).thenReturn(DUMMY_RESOURCE_DEPLOYMENT_DIR);
        when(uploadedUndeployedResource.getResourceChecksum()).thenReturn(DUMMY_RESOURCE_CHECKSUM);
        
        when(sshClient.executeWithResultSilent(anyString())).then(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                String arg = (String) invocation.getArguments()[0];
                if(arg.contains("then echo true; else echo false")) {
                    if(arg.contains(DUMMY_RESOURCE_NAME)) {
                        return "false";
                    } else {
                        return "true";
                    }
                } else {
                    return String.valueOf(DUMMY_RESOURCE_CHECKSUM);
                }
            }
        });
    }
    
    @After
    public void tearDown() {
        resourceManager = null;
        sshClient = null;
    }

    /**
     * Test of addResource method, of class RemoteResourceManager.
     */
    @Test
    public void testAddResource() {
        resourceManager.addResource(dummyResource);
        assertThat(resourceManager.managedResources, hasItem(dummyResource));
    }

    /**
     * Test of removeResource method, of class RemoteResourceManager.
     */
    @Test
    public void testRemoveResource() {
        resourceManager.addResource(dummyResource);
        resourceManager.removeResource(dummyResource);
        assertThat(resourceManager.managedResources, not(hasItem(dummyResource)));
    }

    /**
     * Test of uploadResources method, of class RemoteResourceManager.
     */
    @Test
    public void testUploadResources() throws SSHClientException {
        resourceManager.addResource(dummyResource);
        resourceManager.addResource(dummyResource2);
        resourceManager.addResource(uploadedUndeployedResource);
        resourceManager.uploadResources();
        
        //2 file uploads, 2 crc file uploads
        verify(sshClient, times(4)).sftpUpload(
                Matchers.any(InputStream.class), anyString());
        
        when(dummyResource.isUploaded()).thenReturn(Boolean.TRUE);
        when(dummyResource2.isUploaded()).thenReturn(Boolean.TRUE);
        when(uploadedUndeployedResource.isUploaded()).thenReturn(Boolean.TRUE);
        
        resourceManager.addResource(dummyResource3);
        resourceManager.uploadResources();
        
        //additional 2 calls, 1 file, 1 crc file
        verify(sshClient, times(6)).sftpUpload(
                Matchers.any(InputStream.class), anyString());
    }

    /**
     * Test of deployResources method, of class RemoteResourceManager.
     */
    @Test
    public void testDeployResources() throws SSHClientException {
        resourceManager.addResource(dummyResource);
        resourceManager.addResource(uploadedUndeployedResource);
        
        when(dummyResource.isUploaded()).thenReturn(Boolean.TRUE);
        when(uploadedUndeployedResource.isUploaded()).thenReturn(Boolean.TRUE);
        
        resourceManager.deployResources();
        
        String pattern = String.format(".*%s.*%s.*%s.*%s.*", 
                DUMMY_RESOURCE_NAME, DUMMY_RESOURCE_DEPLOYMENT_DIR, 
                UPLOADED_UNDEPLOYED_RESOURCE_NAME, DUMMY_RESOURCE_DEPLOYMENT_DIR);
        verify(sshClient).executeWithResultSilent(matches(pattern));
        
        when(dummyResource.isDeployed()).thenReturn(Boolean.TRUE);
        when(uploadedUndeployedResource.isDeployed()).thenReturn(Boolean.TRUE);
        
        resourceManager.addResource(dummyResource2);
        when(dummyResource2.isUploaded()).thenReturn(Boolean.TRUE);
        
        resourceManager.deployResources();
        String pattern2 = String.format(".*%s.*%s.*", DUMMY_RESOURCE_NAME + 2, 
                DUMMY_RESOURCE_DEPLOYMENT_DIR);
        verify(sshClient).executeWithResultSilent(matches(pattern2));
        verify(sshClient, times(1)).executeWithResultSilent(matches(pattern));
    }

    /**
     * Test of prepareResourceDirectory method, of class RemoteResourceManager.
     */
    @Test
    public void testPrepareResourceDirectory() throws Exception {
        resourceManager.prepareResourceDirectory();
        verify(sshClient).executeWithResultSilent(matches(
                String.format(".*mkdir.*%s%s%s.*", 
                RESOURCE_DIR_PATH, SEPARATOR, RESOURCE_DIR_NAME)));
    }

    /**
     * Test of uploadResource method, of class RemoteResourceManager.
     */
    @Test
    public void testUploadResource() throws Exception {
        resourceManager.uploadResource(dummyResource);
        resourceManager.uploadResource(uploadedUndeployedResource);
        String dest = String.format("%s%s%s%s%s", RESOURCE_DIR_PATH, SEPARATOR, 
                RESOURCE_DIR_NAME, SEPARATOR, DUMMY_RESOURCE_NAME);
        verify(sshClient, times(1)).sftpUpload(DUMMY_RESOURCE_DATA, dest);
        //verify crc file upload
        dest = String.format("%s%s%s%s", resourceManager.remoteCrcDir, 
                SEPARATOR, DUMMY_RESOURCE_NAME, RemoteResourceManager.CRC_FILE_EXTENSION);
        verify(sshClient, times(1)).sftpUpload(
                Matchers.any(ByteArrayInputStream.class), eq(dest));
    }

    /**
     * Test of isResourceUploadedAndUpToDate method, of class RemoteResourceManager.
     */
    @Test
    public void testIsResourceUploadedAndUpToDate() throws Exception {
        boolean upToDate = resourceManager.isResourceUploadedAndUpToDate(
                dummyResource, DUMMY_RESOURCE_CHECKSUM);
        assertThat(upToDate, is(equalTo(false)));
        upToDate = resourceManager.isResourceUploadedAndUpToDate(
                uploadedUndeployedResource, DUMMY_RESOURCE_CHECKSUM);
        assertThat(upToDate, is(equalTo(true)));
    }

    /**
     * Test of fileExistOnRemote method, of class RemoteResourceManager.
     */
    @Test
    public void testFileExistOnRemote() throws Exception {
        boolean exists = resourceManager.fileExistOnRemote(
                String.format("%s%s%s", resourceManager.remoteResourcesDir, 
                resourceManager.remoteSeparator, DUMMY_RESOURCE_NAME));
        assertThat(exists, is(equalTo(false)));
        exists = resourceManager.fileExistOnRemote(
                String.format("%s%s%s", resourceManager.remoteResourcesDir, 
                resourceManager.remoteSeparator, UPLOADED_UNDEPLOYED_RESOURCE_NAME));
        assertThat(exists, is(equalTo(true)));
    }
}
