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

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SSH Client functionality for SSH and SFTP.
 *
 * @author daniel
 */
public class SSHClient {

	private final static Logger logger = 
			LoggerFactory.getLogger(SSHClient.class);
	
	/**
	 * JSch Instance.
	 */
	protected JSch jsch = null;
	
	/**
	 * JSch Session.
	 */
	protected Session session = null;
	
	/**
	 * TCP Port. 
	 * 
	 * (Default: 22)
	 */
	protected int port = 22;
	
	/**
	 * SSH Client. 
	 * 
	 * Private key provided as byte array, without passphrase.
	 * 
	 * @param keyName	
	 *		A human-readable name for the key-pair.
	 * @param privateKey	
	 *		The private key data.
	 * @throws SSHClientExcpetion	
	 *		If there is a problem processing the credentials.
	 */
	public SSHClient(String keyName, byte[] privateKey) 
			throws SSHClientExcpetion {
		this(keyName, privateKey, null, null);
	}
	
	/**
	 * SSH Client. 
	 * 
	 * Private key provided as byte array, with passphrase.
	 * 
	 * @param keyName	
	 *		A human-readable name for the key-pair.
	 * @param privateKey	
	 *		The private key data.
	 * @param passphrase
	 *		The passphrase data. Can be null.
	 * @throws SSHClientExcpetion	
	 *		If there is a problem processing the credentials.
	 */
	public SSHClient(String keyName, byte[] privateKey, byte[] passphrase) 
			throws SSHClientExcpetion {
		this(keyName, privateKey, null, passphrase);
	}
	
	/**
	 * SSH Client. 
	 * 
	 * Private and public keys provided as byte arrays, with passphrase.
	 * 
	 * @param keyName	
	 *		A human-readable name for the key-pair.
	 * @param privateKey	
	 *		The private key data.
	 * @param publicKey 
	 *		The public key data. Can be null.
	 * @param passphrase
	 *		The passphrase data. Can be null.
	 * @throws SSHClientExcpetion	
	 *		If there is a problem processing the credentials.
	 */
	public SSHClient(String keyName, byte[] privateKey, byte[] publicKey, 
			byte[] passphrase) throws SSHClientExcpetion {

		jsch = new JSch();
		try {
			jsch.addIdentity(keyName, privateKey, null, passphrase);
		} catch (JSchException ex) {
			throw new SSHClientExcpetion(ex);
		}
	}
	
	/**
	 * SSH Client. 
	 * 
	 * Private key provided as file path.
	 * 
	 * @param privateKeyFilePath
	 *		Path to the private key file.
	 * @throws SSHClientExcpetion	
	 *		If there is a problem processing the credentials.
	 */
	public SSHClient(String privateKeyFilePath) throws SSHClientExcpetion {
		this(privateKeyFilePath, (String) null);
	}
	
	/**
	 * SSH Client. 
	 * 
	 * Private key provided as file path, with passphrase.
	 * 
	 * @param privateKeyFilePath
	 *		Path to the private key file.
	 * @param passphrase
	 *		The passphrase.
	 * @throws SSHClientExcpetion
	 *		If there is a problem processing the credentials.
	 */
	public SSHClient(String privateKeyFilePath, String passphrase) 
			throws SSHClientExcpetion {
		
		jsch = new JSch();
		try {
			jsch.addIdentity(privateKeyFilePath, passphrase);
		} catch (JSchException ex) {
			throw new SSHClientExcpetion(ex);
		}
	}
	
	/**
	 * Open a connection to the SSH Daemon/Server on default port (TCP 22).
	 * 
	 * @param userName
	 *		The user name.
	 * @param host
	 *		The host or IP to connect to.
	 * @throws SSHClientExcpetion 
	 *		If a new connection can not be established.
	 */
	public void connect(String userName, String host) 
			throws SSHClientExcpetion {
		connect(userName, host, port);
	}
	
	/**
	 * Open a connection to the SSH Daemon/Server.
	 * 
	 * @param userName
	 *		The user name.
	 * @param host
	 *		The host or IP to connect to.
	 * @param port 
	 *		The TCP port to use for the connection.
	 * @throws SSHClientExcpetion 
	 *		If a new connection can not be established.
	 */
	public void connect(String userName, String host, int port) 
			throws SSHClientExcpetion {
		
		if(isNull(userName) || userName.isEmpty()) {
			throw new SSHClientExcpetion("Invalid user name.");
		}
		if(isNull(host) || host.isEmpty()) {
			throw new SSHClientExcpetion("Invalid host.");
		}
		if(port < 0) {
			throw new SSHClientExcpetion("Invalid TCP port.");
		}
		
		try {
			session = jsch.getSession(userName, host, port);
			session.setConfig("StrictHostKeyChecking", "no");
			//TODO: deal with negotiation failure if server does not support compression
			session.setConfig("compression.c2s", "zlib@openssh.com");
			logger.info("Connecting to {}:{}.", host, port);
			session.connect();
		} catch (JSchException ex) {
			logger.error("Connection to {}:{} failed.", host, port);
			throw new SSHClientExcpetion(ex);
		}
	}
	
	/**
	 * Close the connection to the SSH Daemon/Server.
	 */
	public void disconnect() {
		if(notNull(session)) {
			session.disconnect();
			logger.info("Disconnected from {}:{}.", 
					session.getHost(), session.getPort());
		}
	}
	
	/**
	 * Upload a file or directory to the SSH server using SFTP.
	 * 
	 * Existing files or directories are overwritten.
	 * 
	 * @param srcPath	
	 *		The path to the file or directory to upload.
	 * @param destPath
	 *		The path to the file or directory on the server 
	 *		(relative to the user's home directory).
	 * @throws SSHClientExcpetion
	 *		When the file or directory can not be uploaded.
	 */
	public void sftpUpload(String srcPath, String destPath) throws SSHClientExcpetion {
		if(notNull(session) && session.isConnected()) {
			ChannelSftp channel = null;
			try {
				channel = (ChannelSftp) session.openChannel("sftp");
				channel.connect();
				logger.info("Opened SFTP channel.");
				File file = new File(srcPath);
				if(file.isDirectory()) {
					copyDirectory(file, destPath, channel);
				} else if(file.isFile()) {
					copyFile(file, destPath, channel);
				}
			} catch (SSHClientExcpetion ex) {
				throw ex;
			} catch (Exception ex) {
				throw new SSHClientExcpetion(ex);
			} finally {
				if(notNull(channel)) {
					channel.disconnect();
					logger.info("SFTP channel closed.");
				}
			}
		} else {
			throw new SSHClientExcpetion("No connection to server.");
		}
	}
	
	/**
	 * Execute a command.
	 * 
	 * @param command
	 * @throws SSHClientExcpetion 
	 */
	public String sshExec(String command, OutputStream errorOutputStream) throws SSHClientExcpetion {
		if(notNull(session) && session.isConnected()) {
			ChannelExec channel = null;
			try {
				channel = (ChannelExec) session.openChannel("exec");
				channel.setCommand(command.getBytes("UTF-8"));
				
				channel.setErrStream(errorOutputStream);
				
				BufferedReader in = new BufferedReader(
						new InputStreamReader(channel.getInputStream()));
				
				channel.connect();
				logger.info("Opened SSH Exec channel.");
				
				StringBuilder sb = new StringBuilder();
				String line;
				while((line = in.readLine()) != null) {
					sb.append(line);
					sb.append("\n");
				}
				
				return sb.toString().trim();
				
			} catch (Exception ex) {
				throw new SSHClientExcpetion(ex);
			} finally {
				if(notNull(channel)) {
					channel.disconnect();
					logger.info("SSH Exec channel closed.");
				}
			}
		} else {
			throw new SSHClientExcpetion("No connection to server.");
		}
	}
	
	/**
	 * Upload a directory.
	 * 
	 * If the directory is a file, it will call uploadFile.
	 * 
	 * @param dir
	 *		The directory to upload.
	 * @param destDir
	 *		The destination directory name. 
	 *		Relative to the current remote directory.
	 * 
	 * @throws SSHClientExcpetion
	 *		When it is not possible to read from the 
	 *		provided file.
	 * @throws SftpException 
	 *		Possible reasons: Can not read current working directory, 
	 *		can not create destination directory, 
	 *		can not change to destination directory.
	 */
	private void copyDirectory(File dir, String destDir, ChannelSftp channel) 
			throws SSHClientExcpetion, SftpException {
		
		if(isNull(dir) || !dir.canRead()) {
			throw new SSHClientExcpetion("Can not read from " + dir);
		}
		
		String currentDir = channel.pwd();
		createDirectoryIfNotExists(channel, currentDir, destDir);
		channel.cd(destDir);
		currentDir += "/" + destDir;
		
		for(File file : dir.listFiles()) {
			if(file.isDirectory()) {
				try {
					copyDirectory(file, file.getName(), channel);
				} catch(SftpException ex) {
					logger.warn("Can not copy directory {}/{}.", 
							currentDir, file.getName());
					logger.warn(ex.getMessage(), ex);
				} catch(SSHClientExcpetion ex) {
					logger.warn("Can not copy directory {}/{}.", 
							currentDir, file.getName());
					logger.warn(ex.getMessage(), ex);
				}
			} else if(file.isFile()) {
				try {
					copyFile(file, file.getName(), channel);
				} catch(SftpException ex) {
					logger.warn(ex.getMessage(), ex);
				} catch(SSHClientExcpetion ex) {
					logger.warn(ex.getMessage(), ex);
				}
			}
		}
		channel.cd("..");
	}

	private void createDirectoryIfNotExists(ChannelSftp channel, 
			String currentDir, String destDir) throws SftpException {
		
		for(Object o : channel.ls(currentDir)) {
			//TODO: what to do on windows OS where dirs are case insensitive?
			if(((ChannelSftp.LsEntry) o).getFilename().equals(destDir)) {
				return;
			}
		}
		logger.info("Creating remote directory {}/{}.", currentDir, destDir);
		channel.mkdir(destDir);
	}
	
	/**
	 * Upload a file.
	 * 
	 * @param src 
	 *		The file to upload.
	 * @param dest	
	 *		The destination file name. Relative to the current remote directory.
	 * 
	 * @throws SSHClientExcpetion	
	 *		When it is not possible to read from the 
	 *		provided file or the file is not found.
	 * @throws SftpException	
	 *		When the provided file could not be uploaded for some reason.
	 */
	private void copyFile(File src, String dest, ChannelSftp channel) 
			throws SSHClientExcpetion, SftpException {
		
		if(isNull(src) || !src.canRead()) {
			throw new SSHClientExcpetion("Can not read from " + src);
		}
		InputStream fin = null;
		try {
			fin = new FileInputStream(src);
			logger.info("Uploading {} to {}/{}.", 
					new String[]{src.getAbsolutePath(), channel.pwd(), dest});
			channel.put(fin, dest, ChannelSftp.OVERWRITE);
		} catch(SftpException ex) {
			logger.warn("{} could not be uploaded to {}/{}.", 
					new String[]{src.getAbsolutePath(), channel.pwd(), dest});
			throw ex;
		} catch (FileNotFoundException ex) {
			throw new SSHClientExcpetion(ex);
		} finally {
			if(notNull(fin)) {
				try {
					fin.close();
				} catch (IOException ex) {
					//ignore
				}
			}
		}
	}
	
	private boolean notNull(Object o) {
		return o != null;
	}
	
	private boolean isNull(Object o) {
		return o == null;
	}
}
