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

import ch.ethz.ssh2.*;
import com.google.common.io.CharStreams;
import com.jcraft.jsch.SftpException;
import java.io.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas
 * @deprecated You should use {@link SSHClientImpl}
 */
@Deprecated
public class GanymedSSHClient implements SSHClient {

    private Logger logger = LoggerFactory.getLogger(GanymedSSHClient.class);
    private Connection connection;
    private final String privateKeyFilePath;

    public GanymedSSHClient(String privateKeyFilePath) {
        this.privateKeyFilePath = privateKeyFilePath;
    }

    @Override
    public void connect(String userName, String host) throws SSHClientException {
        connect(userName, host, 22);
    }

    @Override
    public void connect(String userName, String host, int port) throws SSHClientException {
        connection = new Connection(host, port);
        try {
            connection.connect();
            connection.authenticateWithPublicKey(userName, new File(privateKeyFilePath), "");
            if (!connection.isAuthenticationComplete()) {
                throw new SSHClientException("Could not complete authentication");
            }
            executeAndSysout("uname -r");
//            session = connection.openSession();
//            StreamGobbler streamGobbler = new StreamGobbler(session.getStdout());
//            outputStream = new BufferedReader(new InputStreamReader(streamGobbler));
        } catch (IOException ex) {
            throw new SSHClientException("Could not read private key", ex);
        }
        logger.info("Connected to " + host + ":" + port + " as " + userName);
    }

    @Override
    public String executeWithResult(String command) throws SSHClientException {
        logger.info("$ " + command);
        Session session = null;
        try {
            session = checkIfConnectionIsOpenAndOpenSession();
            session.execCommand(command);
            return readOutputStream(session);
        } catch (IOException ex) {
            throw new SSHClientException("Could not execute command.", ex);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    private String readOutputStream(Session session) throws IOException {
        StreamGobbler streamGobbler = new StreamGobbler(session.getStdout());
        BufferedReader outputStream = new BufferedReader(new InputStreamReader(streamGobbler));
        return CharStreams.toString(outputStream);
    }

    @Override
    public void executeAndSysout(String command) throws SSHClientException {
        System.out.println("=> " + executeWithResult(command));
    }

    @Override
    public void sftpUpload(String srcPath, String destPath) throws SSHClientException {
        throw new UnsupportedOperationException("Not yet implemented");
//        try {
//            SFTPv3Client client = new SFTPv3Client(connection);
//            copyFileOrDirectory(srcPath, destPath, null);
//        } catch (SftpException ex) {
//            throw new RuntimeException(ex);
//        } catch (IOException ex) {
//            throw new SSHClientException("Could not upload file.", ex);
//        }
    }

    private void copyFileOrDirectory(String srcPath, String destPath, SFTPv3Client client) throws SSHClientException, SftpException {
        File file = new File(srcPath);
        if (file.isDirectory()) {
            copyDirectory(file, destPath, client);
        } else if (file.isFile()) {
            copyFile(file, destPath, client);
        }
    }

    private void copyFile(File file, String destPath, SFTPv3Client client) {
    }

    private void copyFile(InputStream inputStream, String destPath, SFTPv3Client client) throws IOException {
//        byte[] toByteArray = ByteStreams.toByteArray(inputStream);
//        SFTPv3FileHandle file = client.createFileTruncate(destPath);
//        client.write(file, 0, toByteArray, 0, toByteArray.length);
//        client.closeFile(file);

        SFTPv3FileHandle h = client.createFileTruncate(destPath);
        byte[] buff = new byte[32768];
        long off = 0;
        while (true) {
            int res = inputStream.read(buff);
            if (res == -1) {
                break;
            }
            if (res > 0) {
                client.write(h, off, buff, 0, res);
                off += res;
            }
        }
        client.closeFile(h);
    }

    private void copyDirectory(File file, String destPath, Object channel) {
    }

    @Override
    public void sftpUpload(InputStream stream, String dest) throws SSHClientException {
        checkIfConnectionIsOpen();
        try {
            SFTPv3Client client = new SFTPv3Client(connection);
            copyFile(stream, dest, client);
        } catch (IOException ex) {
            throw new SSHClientException("Could not upload file.", ex);
        }

    }

    @Override
    public void disconnect() {
        if (connection != null) {
            connection.close();
        }
    }

    private void checkIfConnectionIsOpen() throws SSHClientException {
        if (connection == null) {
            throw new SSHClientException("Connection is not open");
        }
    }

    private Session checkIfConnectionIsOpenAndOpenSession() throws SSHClientException {
        checkIfConnectionIsOpen();
        try {
            return connection.openSession();
        } catch (IOException ex) {
            throw new SSHClientException("Could not open session", ex);
        }
    }
}
