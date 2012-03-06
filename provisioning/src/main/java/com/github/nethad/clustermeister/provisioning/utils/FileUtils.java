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

import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
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

    public static synchronized long getCRC32ForFile(File file) throws IOException {
        return Files.getChecksum(file, crc32);
    }

    public static synchronized String getMD5ForFile(File file) throws IOException {
        return getHexString(Files.getDigest(file, digest));
    }

    public static String getFileExistsShellCommand(String filePath) {
        StringBuilder sb = new StringBuilder("if  [ -f ");
        sb.append(filePath);
        sb.append(" ]; then echo ");
        sb.append(Boolean.TRUE);
        sb.append("; else; echo ");
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
