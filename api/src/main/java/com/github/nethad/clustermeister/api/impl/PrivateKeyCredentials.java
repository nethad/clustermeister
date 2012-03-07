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
package com.github.nethad.clustermeister.api.impl;

import com.github.nethad.clustermeister.api.Credentials;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * A class representing user name and private key credentials.
 *
 * @author daniel
 */
public class PrivateKeyCredentials extends Credentials {
    
    /**
     * A source to read the private key from.
     */
    protected final InputStream keySource;
    
    private Charset charset;
    
    /**
     * Creates Credentials with private key and user name.
     * 
     * @param user  The user name.
     * @param keySource 
     *      A source for the private key. This source is read when 
     *      {@link #getPrivateKey()} is called. The source is expected to be 
     *      encoded in UTF-8. This can be changed with 
     *      {@link #setKeySourceCharset(java.nio.charset.Charset)}.
     *      The private key must not have a pass phrase.
     */
    public PrivateKeyCredentials(String user, InputStream keySource) {
        super(user);
        this.keySource = keySource;
        this.charset = Charsets.UTF_8;
    }
    
    /**
     * Set the {@link Charset} the private key is encoded in.
     * 
     * @param charset 
     *      the {@link Charset} the private key is encoded in. Default is UTF-8.
     */
    public void setKeySourceCharset(Charset charset) {
        this.charset = charset;
    }
    
    /**
     * Get the private key.
     * 
     * @return  the private key as a String.
     * @throws IOException  
     *      when an error occurs while reading from {@link #keySource}. 
     */
    public String getPrivateKey() throws IOException {
        return CharStreams.toString(new InputStreamReader(keySource, charset));
    }
}
