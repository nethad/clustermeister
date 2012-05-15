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

import java.io.InputStream;

/**
 * 
 * @author thomas
 */
public interface SSHClient {
    
    /**
     * Connect to the SSH server.
     * 
     * @param userName login user name
     * @param host host address for the SSH server
     * @throws SSHClientException 
     */
    public void connect(String userName, String host) throws SSHClientException;
    
    /**
     * Connect to the SSH server.
     * 
     * @param userName login user name
     * @param host host address for the SSH server
     * @param port port number for the SSH server
     * @throws SSHClientException 
     */
    public void connect(String userName, String host, int port) throws SSHClientException;
    
    /**
     * Execute the given command on the ssh shell.
     * 
     * @param command the shell command
     * @return The command's result as a string
     * @throws SSHClientException 
     */
    public String executeWithResult(String command) throws SSHClientException;
    
    /**
     * Execute the given command on the ssh shell without logging output.
     * 
     * @param command the shell command
     * @return The command's result as a string
     * @throws SSHClientException 
     */
    public String executeWithResultSilent(String command) throws SSHClientException;
    
    /**
     * Execute the given command on the ssh shell and log it.
     * 
     * @param command
     * @throws SSHClientException 
     */
    public void executeAndSysout(String command) throws SSHClientException;
    
    /**
     * Use SFTP to upload a file to the SSH server
     * 
     * @param srcPath the local path for the file to upload
     * @param destPath the remote path to upload
     * @throws SSHClientException 
     */
    public void sftpUpload(String srcPath, String destPath) throws SSHClientException;
    
    /**
     * Use SFTP to upload an InputStream to the SSH server as a file.
     * @param stream the stream to upload
     * @param dest the remote path to upload it to
     * @throws SSHClientException 
     */
    public void sftpUpload(InputStream stream, String dest) throws SSHClientException;
    
    /**
     * Close the SSH connection.
     */
    public void disconnect();

    /**
     * Set the path to the private key used for the SSH connection.
     * @param privateKeyPath path to the private key file
     * @throws SSHClientException 
     */
    public void setPrivateKey(String privateKeyPath) throws SSHClientException;
    
    /**
     * Check SSH connection
     * @return returns <code>true</code> if connected, <code>false</code> otherwise.
     */
    public boolean isConnected();
    
    /**
     * Returns the SSH user name.
     * @return 
     */
    public String getUserName();
    
    /**
     * Returns the SSH server host address.
     * @return 
     */
    public String getHost();
    
    /**
     * Returns the SSH server port number.
     * @return 
     */
    public int getPort();
    
}
