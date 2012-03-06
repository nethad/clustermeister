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

import com.google.common.base.Objects;
import org.apache.commons.lang3.builder.EqualsBuilder;

/**
 * Lightweight IP Socket description (IP/Host and port).
 * 
 * Carries no socket functionality, only data for host and port. 
 *
 * @author daniel
 */
public class IPSocket {
    private final String host;
    private final int port;
    private final String str;

    public IPSocket(String host, int port) {
        this.host = host;
        this.port = port;
        this.str = host + ":" + port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
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
        IPSocket other = (IPSocket) obj;
        return new EqualsBuilder().
                append(host, other.host).
                append(port, other.port).
                isEquals();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(host, port);
    }
    
    @Override
    public String toString() {
        return str;
    }
}
