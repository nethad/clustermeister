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

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import java.io.File;

/**
 * A class representing user name and key pair credentials. 
 * 
 * The key pair has to be configured with AWS already. That means the public 
 * key is already authorized on instances created with this key pair.
 *
 * @author daniel
 */
public class AmazonConfiguredKeyPairCredentials extends KeyPairCredentials {
    /**
     * Default AWS EC2 user name ("ec2-user").
     */
    public static final String DEFAULT_USER = "ec2-user";
    
    /**
     * AWS key pair name
     */
    protected final String amazonKeyPairName;

    /**
     * Creates Credentials with the default EC2 user name and a 
     * pre-configured AWS key pair.
     * 
     * @param privateKeySource 
     *      A source for the private key. This source is read when 
     *      {@link #getPrivateKey()} is called. The source is expected to be 
     *      encoded in UTF-8. This can be changed with 
     *      {@link #setKeySourceCharset(java.nio.charset.Charset)}.
     *      The private key must not have a pass phrase.
     * @param amazonKeyPairName 
     *      The key pair name in AWS.
     * 
     * @see #DEFAULT_USER
     */
    public AmazonConfiguredKeyPairCredentials(File privateKeySource, 
            String amazonKeyPairName) {
        this(DEFAULT_USER, privateKeySource, amazonKeyPairName);
    }
    
    /**
     * Creates Credentials with a user name and a pre-configured AWS key pair.
     * 
     * @param user  The user name.
     * @param privateKeySource 
     *      A source for the private key. This source is read when 
     *      {@link #getPrivateKey()} is called. The source is expected to be 
     *      encoded in UTF-8. This can be changed with 
     *      {@link #setKeySourceCharset(java.nio.charset.Charset)}.
     *      The private key must not have a pass phrase.
     * @param amazonKeyPairName 
     *      The key pair name in AWS.
     */
    public AmazonConfiguredKeyPairCredentials(String user, File privateKeySource, 
            String amazonKeyPairName) {
        super(user, privateKeySource, Optional.<File>absent());
        this.amazonKeyPairName = amazonKeyPairName;
    }

    /**
     * Get AWS key pair name.
     * 
     * @return the name of the key pair.
     */
    public String getAmazonKeyPairName() {
        return amazonKeyPairName;
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
        AmazonConfiguredKeyPairCredentials other = 
                (AmazonConfiguredKeyPairCredentials) obj;
        return amazonKeyPairName.equals(other.amazonKeyPairName) && 
                super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(user, privatekeySource, publickeySource, 
                amazonKeyPairName);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).
                addValue(user).
                add("privateKey", privatekeySource).
                add("Key Pair", amazonKeyPairName).toString();
    }
}
