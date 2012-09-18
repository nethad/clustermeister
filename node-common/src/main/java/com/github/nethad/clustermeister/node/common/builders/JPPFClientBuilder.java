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
package com.github.nethad.clustermeister.node.common.builders;

import org.jppf.client.JPPFClient;
import org.jppf.client.event.ClientListener;

/**
 * Builds a JPPF Client.
 *
 * @author daniel
 */
public class JPPFClientBuilder extends PropertyConfiguratedJPPFComponentBuilder<JPPFClient> {
    
    /**
     * The UUID to use for the client.
     */
    protected String uuid;
    
    /**
     * ClientListeners to register with the client.
     */
    protected ClientListener[] clientListeners;

    /**
     * Create a new JPPFClientBuilder with a new computed UUID.
     * 
     * @param clientListeners 
     */
    public JPPFClientBuilder(ClientListener... clientListeners) {
        this(null, clientListeners);
    }

    /**
     * Create a new JPPFClientBuilder with a specified UUID.
     * 
     * @param uuid A UUID to use for the built client or null to compute a new UUID. 
     * @param clientListeners {@link ClientListener}s to register with the JPPF Client.
     */
    public JPPFClientBuilder(String uuid, ClientListener... clientListeners) {
        this.uuid = uuid;
        this.clientListeners = clientListeners;
    }
    
    @Override
    protected JPPFClient doBuild() {
        JPPFClient client;
        if(uuid == null) {
            client = new JPPFClient(clientListeners);
        } else {
            client = new JPPFClient(null, clientListeners);
        }
        
        return client;
    }
}
