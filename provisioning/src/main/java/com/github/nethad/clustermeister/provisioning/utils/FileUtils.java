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

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.zip.CRC32;

/**
 *
 * @author thomas, daniel
 */
public class FileUtils {

    private static MessageDigest digest = null;
    private static CRC32 crc32 = new CRC32();
    private final static String HEXES = "0123456789ABCDEF";

    public static synchronized long getCRC32(final InputStream in) throws IOException {
        return ByteStreams.getChecksum(new InputSupplier<InputStream>() {
            @Override
            public InputStream getInput() throws IOException {
                return in;
            }
        }, crc32);
    }
    
    public static synchronized long getCRC32ForFile(File file) throws IOException {
        return Files.getChecksum(file, crc32);
    }

    public static synchronized String getMD5ForFile(File file) throws IOException {
        return getHexString(Files.getDigest(file, digest));
    }

    public static String getFileExistsShellCommand(String filePath) {
        StringBuilder sb = new StringBuilder("if  [ -f ");
        return appendIfElseBoolean(sb, filePath);
    }

    public static String getDirectoryExistsShellCommand(String dirPath) {
        StringBuilder sb = new StringBuilder("if  [ -d ");
        return appendIfElseBoolean(sb, dirPath);
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
