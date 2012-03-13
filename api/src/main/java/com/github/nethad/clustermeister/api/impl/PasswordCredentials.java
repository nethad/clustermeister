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
import com.google.common.base.Objects;

/**
 * A class representing user name and password credentials.
 *
 * @author daniel
 */
public class PasswordCredentials extends Credentials {
    /**
     * The login password.
     */
    protected final String password;
    
    /**
     * Creates Credentials with a user name and a password.
     * 
     * @param user  The user name.
     * @param password  The password.
     */
    public PasswordCredentials(String user, String password) {
        super(user);
        this.password = password;
    }

    /**
     * Get the password.
     * 
     * @return  the password.
     */
    public String getPassword() {
        return password;
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
        PasswordCredentials other = (PasswordCredentials) obj;
        return Objects.equal(user, other.user) && 
                Objects.equal(password, other.password);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(user, password);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).
                addValue(user).
                add("password", password == null ? "null" : "<secret>").
                toString();
    }
}
