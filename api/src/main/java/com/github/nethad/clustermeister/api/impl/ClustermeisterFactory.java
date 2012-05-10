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

import com.github.nethad.clustermeister.api.Clustermeister;
import com.github.nethad.clustermeister.api.JPPFConstants;

/**
 * A factory to instantiate the {@link Clustermeister} object. This is the entry point to the Clustermeister API.
 * @author thomas
 */
public class ClustermeisterFactory {
    
    /**
     * Instantiates a {@link Clustermeister} object.
     * @return a configured Clustermeister object.
     */
    public static Clustermeister create() {
        
        System.setProperty(JPPFConstants.CONFIG_PLUGIN, JPPFClientConfiguration.class.getCanonicalName());
        ClustermeisterImpl clustermeister = new ClustermeisterImpl();
        clustermeister.gatherNodeInformation();
        return clustermeister;
    }
    
}
