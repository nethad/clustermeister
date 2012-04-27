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
import java.io.InputStream;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.io.Payloads;
import org.jclouds.ssh.SshClient;

/**
 * only for internal use.
 *
 * @author daniel
 */
public class JCloudsSshClientWrapper implements SSHClient {
    
    private final SshClient sshClient;

    public JCloudsSshClientWrapper(SshClient sshClient) {
        this.sshClient = sshClient;
    }

    @Override
    public void connect(String userName, String host) throws SSHClientException {
        sshClient.connect();
    }

    @Override
    public void connect(String userName, String host, int port) throws SSHClientException {
        sshClient.connect();
    }

    @Override
    public String executeWithResult(String command) throws SSHClientException {
        return executeWithResultSilent(command);
    }
    
    @Override
    public String executeWithResultSilent(String command) throws SSHClientException {
        ExecResponse response = sshClient.exec(command);
        return response.getOutput();
    }

    @Override
    public void executeAndSysout(String command) throws SSHClientException {
        ExecResponse response = sshClient.exec(command);
        System.out.println(response.getOutput());
    }

    @Override
    public void sftpUpload(String srcPath, String destPath) throws SSHClientException {
        sshClient.put(destPath, Payloads.newFilePayload(new File(srcPath)));
    }

    @Override
    public void sftpUpload(InputStream stream, String dest) throws SSHClientException {
        sshClient.put(dest, Payloads.newInputStreamPayload(stream));
    }

    @Override
    public void disconnect() {
        sshClient.disconnect();
    }

    @Override
    public void setPrivateKey(String privateKeyPath) throws SSHClientException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isConnected() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
