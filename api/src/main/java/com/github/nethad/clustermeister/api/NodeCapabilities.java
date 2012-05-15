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

import org.jppf.management.JPPFSystemInformation;

/**
 *
 * These node capabilities are taken from the {@link JPPFSystemInformation} that is retrieved when a node connects
 * to a driver.
 * 
 * @author thomas
 */
public interface NodeCapabilities {
    
    /**
     * The number of processors on the machine the node is running. Depending on the provider, not all of these
     * processors are available for computation. A better figure is {@link #getNumberOfProcessingThreads()}.
     * @return 
     */
    public int getNumberOfProcessors();
    
    /**
     * The number of processing threads for this node.
     * @return 
     */
    public int getNumberOfProcessingThreads();
    
    
    public String getJppfConfig();
    
}
