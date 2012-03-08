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
import com.google.common.base.Optional;
import static com.google.common.base.Preconditions.*;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * A class representing user name and key pair credentials.
 *
 * @author daniel
 */
public class KeyPairCredentials extends Credentials {
    
    /**
     * A source to read the private key from.
     */
    protected final File privatekeySource;
    
    /**
     * A source to read the public key from.
     */
    protected final Optional<File> publickeySource;
    
    private Charset charset;
    
    /**
     * Creates Credentials with private key and user name.
     * 
     * @param user  The user name.
     * @param privateKeySource 
     *      A source for the private key. This source is read when 
     *      {@link #getPrivateKey()} is called. The source is expected to be 
     *      encoded in UTF-8. This can be changed with 
     *      {@link #setKeySourceCharset(java.nio.charset.Charset)}.
     *      The private key must not have a pass phrase.
     */
    public KeyPairCredentials(String user, File privateKeySource) {
        this(user, privateKeySource, Optional.<File>absent());
    }
    
    /**
     * Creates Credentials with private key, public key and user name.
     * 
     * @param user  The user name.
     * @param privateKeySource 
     *      A source for the private key. This source is read when 
     *      {@link #getPrivateKey()} is called. The source is expected to be 
     *      encoded in UTF-8. This can be changed with 
     *      {@link #setKeySourceCharset(java.nio.charset.Charset)}.
     *      The private key must not have a pass phrase.
     * @param publicKeySource (optional) 
     *      A source for the public key. This source is read when 
     *      {@link #getPublicKey()} is called. The source is expected to be 
     *      encoded in UTF-8. This can be changed with 
     *      {@link #setKeySourceCharset(java.nio.charset.Charset)}.
     */
    public KeyPairCredentials(String user, File privateKeySource, File publicKeySource) {
        this(user, privateKeySource, Optional.fromNullable(publicKeySource));
    }
    
    /**
     * Creates Credentials with private key, public key and user name.
     * 
     * @param user  The user name.
     * @param privateKeySource 
     *      A source for the private key. This source is read when 
     *      {@link #getPrivateKey()} is called. The source is expected to be 
     *      encoded in UTF-8. This can be changed with 
     *      {@link #setKeySourceCharset(java.nio.charset.Charset)}.
     *      The private key must not have a pass phrase.
     * @param publicKeySource (optional) 
     *      A source for the public key. This source is read when 
     *      {@link #getPublicKey()} is called. The source is expected to be 
     *      encoded in UTF-8. This can be changed with 
     *      {@link #setKeySourceCharset(java.nio.charset.Charset)}.
     */
    protected KeyPairCredentials(String user, File privateKeySource, 
            Optional<File> publicKeySource) {
        super(user);
        checkArgument(privateKeySource != null && 
                privateKeySource.canRead(), "Can not read from private key source.");
        this.privatekeySource = privateKeySource;
        this.publickeySource = publicKeySource;
        this.charset = Charsets.UTF_8;
    }
    
    /**
     * Set the {@link Charset} the private key is encoded in.
     * 
     * @param charset 
     *      the {@link Charset} the private key is encoded in. Default is UTF-8.
     */
    public void setKeySourceCharset(Charset charset) {
        checkArgument(charset != null, "Invalid charset.");
        this.charset = charset;
    }
    
    /**
     * Get the private key.
     * 
     * @return  the private key as a String.
     * @throws IOException  
     *      when an error occurs while reading from {@link #privateKeySource}. 
     */
    public String getPrivateKey() throws IOException {
        return Files.toString(privatekeySource, charset);
    }
}
