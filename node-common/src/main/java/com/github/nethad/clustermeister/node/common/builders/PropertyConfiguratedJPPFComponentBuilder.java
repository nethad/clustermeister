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

import java.util.Map;
import java.util.Properties;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates JPPF component instances that can be configured with {@link Properties}.
 *
 * @author daniel
 */
public abstract class PropertyConfiguratedJPPFComponentBuilder <T>
        extends PluginConfiguratedJPPFComponentBuilder<T> {
    
    private static final Logger logger = 
            LoggerFactory.getLogger(PropertyConfiguratedJPPFComponentBuilder.class);
    
    /**
     * The properties that will be used as JPPFConfiguration.
     */
    protected Properties properties = new Properties();

    private static int configClassId = 1;

    /**
     * Set properties.
     * 
     * @param properties the properties to use as JPPFConfiguration.
     */
    public void setProperties(Properties properties) {
        this.properties = properties;
    }
    
    /**
     * Set a property to be used as JPPFConfiguration.
     * 
     * @param key the property key
     * @param value the property value
     */
    public void setProperty(String key, String value) {
        this.properties.setProperty(key, value);
    }
    
    @Override
    protected String getConfigurationClassName() {
        return createConfigurationSource(properties);
    }
    
    /**
     * Dynamically creates a new class that implements 
     * {@link JPPFConfiguration.ConfigurationSource} and returns the provided properties.
     */
    private static String createConfigurationSource(Properties properties) {
        ClassPool classPool = ClassPool.getDefault();
        String name = String.format("GenericClustermeisterConfigurationSource#%d", configClassId++);
        CtClass classPrototype = classPool.makeClass(name);
        try {
            classPrototype.setInterfaces(
                    new CtClass[]{classPool.get("org.jppf.utils.JPPFConfiguration$ConfigurationSource")});
            StringBuilder method = 
                    new StringBuilder("public java.io.InputStream getPropertyStream() throws java.io.IOException { ");
            method.append("java.util.Properties properties = new java.util.Properties(); ");
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                method.append("properties.setProperty(\"");
                method.append(entry.getKey());
                method.append("\", \"");
                method.append(entry.getValue());
                method.append("\"); ");
            }
            method.append("java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream(); ");
            method.append("properties.store(baos, \"Generic configuration\"); ");
            method.append("return new java.io.ByteArrayInputStream(baos.toByteArray());");
            method.append(" }");
            classPrototype.addMethod(CtMethod.make(method.toString(), classPrototype));
            Class clazz = classPrototype.toClass();
            return clazz.getCanonicalName();
        } catch (Exception ex) {
            logger.error("Can not create generic configuration source class.");
            throw new IllegalStateException(ex);
        }
    }

}
