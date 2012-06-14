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
package com.github.nethad.clustermeister.node.common.launchers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A thread running some JPPF component using reflection.
 *
 * @author daniel
 */
public abstract class ClustermeisterJPPFThread extends Thread {
    protected static final String NO_LAUNCHER_ARG = "noLauncher";

    protected final AtomicBoolean monitorObject = new AtomicBoolean(false);
    
    public ClustermeisterJPPFThread(String name) {
        super(name);
    }
    
    public AtomicBoolean getMonitor() {
        return monitorObject;
    }
    
    protected void executeMain(String clazz, String... args) {
        try {
            Class<?> driver = Class.forName(clazz);
            Method method = driver.getMethod("main", String[].class);
            method.invoke(null, (Object) args);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        } catch (SecurityException ex) {
            throw new RuntimeException(ex);
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }
}
