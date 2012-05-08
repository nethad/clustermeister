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
package com.github.nethad.clustermeister.integration;

import com.github.nethad.clustermeister.provisioning.cli.Provider;
import com.github.nethad.clustermeister.provisioning.cli.Provisioning;
import com.github.nethad.clustermeister.provisioning.rmi.NodeConnectionListener;
import com.github.nethad.clustermeister.provisioning.rmi.RmiInfrastructure;
import com.github.nethad.clustermeister.provisioning.rmi.RmiServerForDriver;

/**
 *
 * @author thomas
 */
public class JPPFTestDriver {
    private final NodeConnectionListener nodeConnectionListener;
    private DriverRunnable driverRunnable;

    public JPPFTestDriver(NodeConnectionListener nodeConnectionListener) {
        this.nodeConnectionListener = nodeConnectionListener;
    }
    
    public void startDriver() {
        driverRunnable = new DriverRunnable();
        new Thread(driverRunnable).start();
    }
    
    public void shutdown() {
        driverRunnable.shutdown();
    }
    
    public class DriverRunnable implements Runnable {
        private Provisioning provisioning;
        
        @Override
        public void run() {
            provisioning = new Provisioning(null, Provider.TEST, null);
            RmiInfrastructure rmiInfrastructure = provisioning.getRmiInfrastructure();
            RmiServerForDriver rmiServerForDriver = rmiInfrastructure.getRmiServerForDriverObject();
            rmiServerForDriver.addNodeConnectionListener(nodeConnectionListener);
            provisioning.execute();
        }
        
        private void shutdown() {
            provisioning.commandShutdown(null);
        }
        
    }
    
}
