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

import static com.google.common.base.Preconditions.*;
import com.google.common.util.concurrent.Monitor;
import com.jcraft.jsch.JSchException;

/**
 * Creates a reverse SOCKS tunnel on the host the associated SSH Client is connected to.
 *
 * @author daniel
 */
public class SocksTunnel extends Thread {
    
    /**
     * Associated SSH client.
     */
    protected final SSHClientImpl sshClient;
    
    /**
     * Indicated whether a stop has been requested.
     */
    protected Boolean stopRequested = Boolean.FALSE;
    
    /**
     * Monitor to synchronize access and await a condition.
     */
    protected final Monitor monitor = new Monitor();
    
    /**
     * Guard to check whether a stop has been requested.
     */
    protected final Monitor.Guard isStop = new Monitor.Guard(monitor) {
        @Override
        public boolean isSatisfied() {
            return stopRequested;
        }
    };

    /**
     * Creates a new SocksTunnel.
     * 
     * Only intended to be used by SSHClientImpl.
     * 
     * @param sshClient the associated sshClient. It is expected to be connected already.
     */
    SocksTunnel(SSHClientImpl sshClient) {
        this.sshClient = sshClient;
        this.setName(String.format("Reverse Tunnel Thread [%d]", this.getId()));
    }

    /**
     * Open a SOCKS tunnel.
     * 
     * This is equivalent to the command ssh -R remotePort:host:localPort in OpenSSH.
     * 
     * @param remotePort    the port on the remote host to bind to.
     * @param host  the hostname to bind to (on the remote host).
     * @param localPort the local port to bind to.
     * @throws SSHClientException 
     *      when the associated SSH client's session is 
     *      not connected or the tunnel can not be opened.
     */
    public void openTunnel(int remotePort, String host, int localPort) throws SSHClientException {
        if(!isSessionsConnected()) {
            throw new SSHClientException("Session not connected.");
        }
        try {
            sshClient.session.setPortForwardingR(remotePort, host, localPort);
            this.start();
        } catch (JSchException ex) {
            throw new SSHClientException(ex);
        }
    }

    /**
     * Requests the tunnel to be closed.
     * 
     * Does not block until the tunnel is closed.
     */
    public void closeTunnel() {
        monitor.enter();
        try {
            this.stopRequested = Boolean.TRUE;
        } finally {
            monitor.leave();
        }
    }
    
    /**
     * Checks whether the socks tunnel is open or closed.
     * 
     * @return true when the tunnel is open, false if it is closed or closing.
     */
    public boolean isTunnelOpen() {
        if(!isSessionsConnected()) {
            return false;
        } else if(!monitor.enterIf(isStop)) {
            try {
                return false;
            } finally {
                monitor.leave();
            }
        } else {
            return true;
        }
    }

    @Override
    public void run() {
        //simply wait for stop request.
        monitor.enter();
        try {
            monitor.waitForUninterruptibly(isStop);
        } finally {
            monitor.leave();
        }
    }

    /**
     * Returns the associated SSHClient.
     * 
     * @return the ssh client.
     */
    public SSHClient getSshClient() {
        return sshClient;
    }
    
    /**
     * Checks if the sessions is not null and connected.
     * 
     * @return  true if the session is connected, false otherwise.
     * 
     */
    protected boolean isSessionsConnected() {
        checkNotNull(sshClient.session);
        return !sshClient.session.isConnected();
    }
}
