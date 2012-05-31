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

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Properties;
import java.util.zip.CRC32;

/**
 *
 * @author thomas, daniel
 */
public class FileUtils {

    private static MessageDigest digest = null;
    private final static String HEXES = "0123456789ABCDEF";

    /**
     * Compute Cyclic Redundancy Check (CRC32).
     * @param in the InputStream to compute
     * @return CRC32 for the given InputStream
     * @throws IOException 
     */
    public static synchronized long getCRC32(final InputStream in) throws IOException {
        return ByteStreams.getChecksum(new InputSupplier<InputStream>() {
            @Override
            public InputStream getInput() throws IOException {
                return in;
            }
        }, new CRC32());
    }
   
    /**
     * Compute Cyclic Redunancy Check (CRC32).
     * @param file the File to compute
     * @return CRC32 for the given File.
     * @throws IOException 
     */
    public static synchronized long getCRC32ForFile(File file) throws IOException {
        return Files.getChecksum(file, new CRC32());
    }

    /**
     * Compute the MD5 sum.
     * @param file the File to compute
     * @return the MD5 sum for the given file
     * @throws IOException 
     */
    public static synchronized String getMD5ForFile(File file) throws IOException {
        return getHexString(Files.getDigest(file, digest));
    }

    /**
     * Builds a shell (bash) command which checks whether a file path exists.
     * The shell command returns <code>true</code> if the path exists, <code>false</code> otherwise.
     * @param filePath the file path as a string to build the shell command for
     * @return the shell command to check the file path
     */
    public static String getFileExistsShellCommand(String filePath) {
        StringBuilder sb = new StringBuilder("if  [ -f ");
        return appendIfElseBoolean(sb, filePath);
    }

    /**
     * Builds a shell (bash) command which checks whether a directory path exists.
     * The shell command returns <code>true</code> if the path exists, <code>false</code> otherwise.
     * @param dirPath the directory path as a string to build the shell command for
     * @return the shell command to check the directory path.
     */
    public static String getDirectoryExistsShellCommand(String dirPath) {
        StringBuilder sb = new StringBuilder("if  [ -d ");
        return appendIfElseBoolean(sb, dirPath);
    }
    
    /**
     * Write a {@link Properties} to a file.
     * 
     * @param comment   A comment to add at the top of the file.
     * @param propertiesFile    the properties file to write to.
     * @param properties    the properties to write.
     * @throws FileNotFoundException when propertiesFile is not found or can not be created.
     * @throws IOException when propertiesFile can not be written to.
     */
    public static void writePropertiesToFile(String comment, final File propertiesFile, 
            Properties properties) throws FileNotFoundException, IOException {
        BufferedWriter fileWriter = Files.newWriter(propertiesFile, Charsets.UTF_8);
        try {
            properties.store(fileWriter, comment);
        } finally {
            if(fileWriter != null) {
                fileWriter.close();
            }
        }
    }
    
    private static String appendIfElseBoolean(StringBuilder sb, String path) {
        sb.append(path);
        sb.append(" ]; then echo ");
        sb.append(Boolean.TRUE);
        sb.append("; else echo ");
        sb.append(Boolean.FALSE);
        sb.append("; fi");
        return sb.toString();
    }
    
    private static String getHexString(byte[] raw) {
        if (raw == null) {
            return null;
        }
        final StringBuilder hex = new StringBuilder(2 * raw.length);
        for (final byte b : raw) {
            hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
        }
        return hex.toString();
    }
}
