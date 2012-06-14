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

import com.github.nethad.clustermeister.node.common.Constants;

/**
 * 
 * Creates JPPF component instances that read the JPPF configuration from a class.
 *
 * This class coordinates the System property settings between concurrent calls.
 *
 * @author daniel
 */
public abstract class PluginConfiguratedJPPFComponentBuilder <T> 
        implements ComponentBuilder<T> {
    
    @Override
    public T build() {
        //JVM wide synchronization
        synchronized(PluginConfiguratedJPPFComponentBuilder.class) {
            setConfigProperty();
            try {
                return doBuild();
            } finally {
                deleteConfigProperty();
            }
        }
    }
    
    /**
     * Sub-classes need to implement the component creation here.
     * 
     * @return a new instance of this component.
     */
    protected abstract T doBuild();
    
    /**
     * Returns the fully qualified class name of the class to read the configuration from.
     * 
     * The class needs to implement the {@link JPPFConfiguration.ConfigurationSource} interface.
     * 
     * @return the class name of the configuration source.
     */
    protected abstract String getConfigurationClassName();

    private void setConfigProperty() {
        System.setProperty(Constants.JPPF_CONFIG_PLUGIN, getConfigurationClassName());
    }
    
    private void deleteConfigProperty() {
        System.getProperties().remove(Constants.JPPF_CONFIG_PLUGIN);
    }
}
