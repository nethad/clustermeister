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

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ComparisonChain;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import java.nio.charset.Charset;

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
     * A name for these credentials.
     */
    protected final String name;
    
    /**
     * Creates a Credential with a user name.
     * 
     * @param user the user name.
     */
    public Credentials(String user) {
        this(Credentials.class.getSimpleName(), user);
    }
    
    /**
     * Creates a Credential with a user defined name and a user name.
     * 
     * @param name a name for these credentials.
     * @param user the user name.
     */
    public Credentials(String name, String user) {
        Preconditions.checkArgument(name != null && !name.isEmpty(), "Invalid name.");
        Preconditions.checkArgument(user != null && !user.isEmpty(), "Invalid user.");
        this.name = name;
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

    /**
     * Returns the name for these Credentials.
     * @return 
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(name).
                add("user", user).
                toString();
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
        Credentials otherCreds = (Credentials) obj;
        
        return Objects.equal(name, otherCreds.name) && Objects.equal(user, otherCreds.user);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, user);
    }

    @Override
    public int compareTo(Credentials o) {
        return ComparisonChain.start().
                compare(name, o.name).
                compare(user, o.user).
                result();
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

    /**
     * Get a {@link Supplier} that returns the hex-string representation of the 
     * input's SHA-256 hash.
     * 
     * The hash is only computed once and cached for further requests.
     * 
     * @param string the input.
     * @param charset   the charset of the input.
     * @return the hexadecimal string  representation of the {@code input}s hash code.
     */
    protected Supplier<String> getSha256DigestSupplier(final String string, 
            final Charset charset) {
        
        return Suppliers.memoize(new Supplier<String>() {

            @Override
            public String get() {
                if(string == null || string.isEmpty()) {
                    return string;
                }
                HashFunction hf = Hashing.sha256();
                HashCode hc = hf.newHasher(string.length()).
                        putString(string, charset).
                        hash();
                return hc.toString();
            }
        });
    }
}
