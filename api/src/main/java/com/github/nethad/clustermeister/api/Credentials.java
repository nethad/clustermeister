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

import com.google.common.base.Preconditions;

/**
 * Credentials.
 *
 * @author daniel
 */
public abstract class Credentials {
    
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
    
    /**
     * Casts this instance to the type represented by {@code clazz}.
     * 
     * @param clazz     the class representing the type to cast to.
     * @return {@code this} as instance of clazz. 
     */
    public <T> T as(Class<T> clazz) {
        return clazz.cast(this);
    }
}
