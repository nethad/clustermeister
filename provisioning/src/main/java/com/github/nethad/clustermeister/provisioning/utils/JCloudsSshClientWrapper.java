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
 * Wraps a jClouds {@link SshClient} with a {@link SSHClient}.
 * 
 * Note: intended for internal use only.
 *
 * @author daniel
 */
public class JCloudsSshClientWrapper implements SSHClient {
    
    private final SshClient sshClient;
    private final int port;

    /**
     * A new JCloudsSshClientWrapper.
     * 
     * @param sshClient the wrapped jClouds SshClient instance.
     * @param port 
     *      The SSH port, this is not used to connect to the specified port 
     *      but only to support querying it. The correct port must be configured 
     *      on the wrapped SshClient instance.
     */
    public JCloudsSshClientWrapper(SshClient sshClient, int port) {
        this.sshClient = sshClient;
        this.port = port;
    }
    
    /**
     * A new JCloudsSshClientWrapper with SSH port 22.
     * 
     * @param sshClient the wrapped jClouds SshClient instance.
     */
    public JCloudsSshClientWrapper(SshClient sshClient) {
        this(sshClient, 22);
    }

    /**
     * Get the host name to connect to.
     * 
     * @return the host name.
     */
    @Override
    public String getHost() {
        return sshClient.getHostAddress();
    }

    /**
     * Get the user name to use.
     * 
     * @return the SSH user name.
     */
    @Override
    public String getUserName() {
        return sshClient.getUsername();
    }

    /**
     * Get the port to connect to.
     * 
     * This value may not be correct. It is always the value set when this class 
     * was instantiated, not the actual value used by the wrapped client.
     * 
     * @return the port.
     */
    @Override
    public int getPort() {
        return port;
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
        //TODO: does not recursively upload directories
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

    /**
     * This method does nothing.
     * 
     * @param privateKeyPath not used.
     * @throws SSHClientException never happens 
     */
    @Override
    public void setPrivateKey(String privateKeyPath) throws SSHClientException {
        //nop
    }

    /**
     * This method is not supported by the wrapper.
     * 
     * @return always throws an UnsupportedOperationException.
     */
    @Override
    public boolean isConnected() {
        throw new UnsupportedOperationException("This operation can not be supported.");
    }
}
