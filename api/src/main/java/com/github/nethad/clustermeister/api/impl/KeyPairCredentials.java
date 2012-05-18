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
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import static com.google.common.base.Preconditions.*;
import com.google.common.collect.ComparisonChain;
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
    protected final Optional<File> publicKeySource;
    
    /**
     * Charset used to interpret private and public key bytes.
     */
    protected Charset charset;
    
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
        this(null, user, privateKeySource);
    }
    
    /**
     * Creates Credentials with a name, a private key and a user name.
     * 
     * @param name A name for these credentials.
     * @param user  The user name.
     * @param privateKeySource 
     *      A source for the private key. This source is read when 
     *      {@link #getPrivateKey()} is called. The source is expected to be 
     *      encoded in UTF-8. This can be changed with 
     *      {@link #setKeySourceCharset(java.nio.charset.Charset)}.
     *      The private key must not have a pass phrase.
     */
    public KeyPairCredentials(String name, String user, File privateKeySource) {
        this((name == null || name.isEmpty()) ? 
                KeyPairCredentials.class.getSimpleName() : name, 
                user, privateKeySource, Optional.<File>absent());
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
        this(null, user, privateKeySource, publicKeySource);
    }
    
    /**
     * Creates Credentials with a name, a private key, a public key and a user name.
     * 
     * @param name A name for these credentials.
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
    public KeyPairCredentials(String name, String user, File privateKeySource, File publicKeySource) {
        this((name == null || name.isEmpty()) ? 
                KeyPairCredentials.class.getSimpleName() : name, user, 
                privateKeySource, Optional.fromNullable(publicKeySource));
    }
    
    /**
     * Creates Credentials with private key, public key and user name.
     * 
     * @param name A name for these credentials.
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
    protected KeyPairCredentials(String name, String user, File privateKeySource, 
            Optional<File> publicKeySource) {
        super(name, user);
        this.privatekeySource = privateKeySource;
        this.publicKeySource = publicKeySource;
        this.charset = Charsets.UTF_8;
    }
    
    /**
     * Set the {@link Charset} the key sources are encoded in.
     * 
     * @param charset 
     *      the {@link Charset} the key sources are encoded in. Default is UTF-8.
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

    /**
     * Get the public key.
     * 
     * @return  the public key as a String.
     * @throws IOException 
     *      when an error occurs while reading from {@link #publicKeySource}. 
     */
    public Optional<String> getPublicKey() throws IOException {
        Optional<String> publicKey;
        if(publicKeySource.isPresent()) {
            publicKey = Optional.of(Files.toString(publicKeySource.get(), charset));
        } else {
            publicKey = Optional.absent();
        }
        
        return publicKey;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != (getClass())) {
            return false;
        }
        KeyPairCredentials other = (KeyPairCredentials) obj;
        return super.equals(obj) && 
                Objects.equal(privatekeySource, other.privatekeySource) && 
                Objects.equal(publicKeySource, other.publicKeySource);
        
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, user, privatekeySource, publicKeySource.orNull());
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(name).
                add("user", user).
                add("privateKey", privatekeySource.getPath()).
                add("publicKey", publicKeySource.orNull()).toString();
    }

    @Override
    public int compareTo(Credentials o) {
        int result = super.compareTo(o);
        if(result == 0 && o instanceof KeyPairCredentials) {
            KeyPairCredentials other = o.as(KeyPairCredentials.class);
            result = ComparisonChain.start().
                    compare(privatekeySource, other.privatekeySource).
                    compare(publicKeySource.orNull(), other.publicKeySource.orNull()).
                    result();
        }
        
        return result;
    }
}
