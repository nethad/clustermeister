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
package com.github.nethad.clustermeister.node.common;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import org.slf4j.Logger;

/**
 * MBean utilities.
 *
 * @author daniel
 */
public class MBeanUtils {

    /**
     * Utility method to invoke methods on MBeans.
     *
     * Exceptions are logged to the supplies logger.
     *
     * @param logger        the logger to use to log exceptions.
     * @param mbeanServer   the mbean server.
     * @param mbeanName     the name of the meban to invoke a method on.
     * @param methodName    the name of the method to invoke.
     * @param arguments     method arguments. Signature is generated based on runtime type of arguments.
     * 
     * @return the object returned by the method invocation or null.
     */
    public static Object invoke(Logger logger, MBeanServer mbeanServer, 
            ObjectName mbeanName, String methodName, Object... arguments) {
        try {
            String[] signature;
            if (arguments != null) {
                signature = new String[arguments.length];
                for (int i = 0; i < arguments.length; ++i) {
                    signature[i] = arguments[i].getClass().getCanonicalName();
                }
            } else {
                signature = null;
            }
            return mbeanServer.invoke(mbeanName, methodName, arguments, signature);
        } catch (InstanceNotFoundException ex) {
            logger.warn("No such MBean instance found.", ex);
        } catch (MBeanException ex) {
            logger.warn("MBean raised exception during method invokation.", ex);
        } catch (ReflectionException ex) {
            logger.warn("Method not found.", ex);
        }
        
        return null;
    }
    
    /**
     * Generates an ObjectName object for a mbean name supplied as String.
     * 
     * Exceptions are logged to the supplied logger.
     * 
     * @param logger the logger to use to log exceptions.
     * @param mbeanName the name of the mbean.
     * 
     * @return the corresponding ObjectName or null in case of an error.
     */
    public static ObjectName objectNameFor(Logger logger, String mbeanName) {
        try {
            return new ObjectName(mbeanName);
        } catch (Throwable ex) {
            logger.warn("Could not create object name.", ex);
        }
        
        return null;
    }
    
}
