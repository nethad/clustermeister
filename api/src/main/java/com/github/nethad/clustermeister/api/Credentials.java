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
package com.github.nethad.clustermeister.api;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ComparisonChain;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Credentials.
 *
 * @author daniel
 */
public abstract class Credentials implements Comparable<Credentials> {
    
    /**
     * A user name.
     */
    protected final String user;
    
    /**
     * Creates a Credential with a user name.
     * 
     * @param user the user name.
     */
    public Credentials(String user) {
        Preconditions.checkArgument(user != null && !user.isEmpty(), "Invalid user.");
        this.user = user;
    }

    /**
     * Returns the user name.
     * 
     * @return  the user name. 
     */
    public String getUser() {
        return user;
    }

    @Override
    public int compareTo(Credentials o) {
        return ComparisonChain.start().compare(user, o.user).result();
    }
    
    /**
     * Casts this instance to the type represented by {@code clazz}.
     * 
     * @param clazz     the class representing the type to cast to.
     * @return {@code this} as instance of clazz. 
     */
    public <T> T as(Class<T> clazz) {
        return clazz.cast(this);
    }

    protected Supplier<String> getSha1DigestSupplier(final String string) {
        return Suppliers.memoize(new Supplier<String>() {

            @Override
            public String get() {
                MessageDigest md;
                try {
                    md = MessageDigest.getInstance("SHA-1");
                } catch (NoSuchAlgorithmException ex) {
                    return "";
                }
                byte[] passwordBytes = string.getBytes(Charsets.UTF_8);
                byte[] digest = md.digest(passwordBytes);
                return new String(digest, Charsets.UTF_8);
            }
        });
    }
}
