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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A thread running some JPPF component using reflection.
 * 
 * <p>
 * This thread allows other threads to wait for the component to be 
 * initialized via the {@link #awaitInitialization()} method.
 * </p>
 * 
 * <p>
 * Signaling this thread of initialization via the 
 * {@link #signalInitialization()} method is the responsibility of the launched 
 * component.
 * </p>
 *
 * @author daniel
 */
public abstract class ClustermeisterJPPFThread extends Thread {
    
    private static final Lock initializationMutex = new ReentrantLock();
    private static final Condition initialization = initializationMutex.newCondition();
    private static ClustermeisterJPPFThread currentWaitingInstance = null;
    
    /**
     * Argument to launch JPPF components without launcher.
     */
    protected static final String NO_LAUNCHER_ARG = "noLauncher";
    
    private final AtomicBoolean initializationMonitor = new AtomicBoolean(false);
    
    private boolean initialized = false;
    
    /**
     * Creates a new ClustermeisterJPPFThread.
     * 
     * @param name the thread name.
     */
    public ClustermeisterJPPFThread(String name) {
        super(name);
    }
    
    /**
     * Get the monitor that allows waiting for initialization. 
     * 
     * @return 
     *      when the component signaled initialization, the threads waiting on 
     *      this monitor are notified.
     */
    public AtomicBoolean getMonitor() {
        return initializationMonitor;
    }
    
    /**
     * Signal the currently waiting thread of initialization.
     */
    public static void signalInitialization() {
        initializationMutex.lock();
        try {
            if(currentWaitingInstance != null) {
                currentWaitingInstance.initialized = true;
            }
            initialization.signalAll();
        } finally {
            initializationMutex.unlock();
        }
    }
    
    /**
     * Execute the main method of {@code clazz} with {@code args}.
     * 
     * @param clazz FQCN of class with a static main(String... args) method.
     * @param args The arguments to the main method.
     */
    protected void executeMain(String clazz, String... args) {
        new WaitForInitialization(this).start();
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
    
    private class WaitForInitialization extends Thread {
        
        private final ClustermeisterJPPFThread instance;

        private WaitForInitialization(ClustermeisterJPPFThread instance) {
            this.instance = instance;
        }
        
        @Override
        public void run() {
            //make sure only one thread registers as waiting instance at a time 
            //waits at a time
            synchronized(ClustermeisterJPPFThread.class) {
                initializationMutex.lock();
                try {
                    currentWaitingInstance = instance;
                    waitForSignal();
                } finally {
                    initializationMutex.unlock();
                }
            }
        }

        private void waitForSignal() throws RuntimeException {
            while (!initialized) {
                try {
                    initialization.await();
                    notifyWaitingThreads();
                } catch (InterruptedException ex) {
                    String message = String.format(
                            "Interrupted while waiting for %s to initialize.",
                            getName());
                    throw new RuntimeException(message, ex);
                }
            }
        }

        private void notifyWaitingThreads() {
            synchronized(initializationMonitor) {
                initializationMonitor.set(true);
                initializationMonitor.notifyAll();
            }
        }
    }
}
