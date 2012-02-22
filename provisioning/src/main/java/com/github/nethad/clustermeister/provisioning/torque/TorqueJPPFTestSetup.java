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
package com.github.nethad.clustermeister.provisioning.torque;

import com.github.nethad.clustermeister.provisioning.jppf.JPPFConfigurator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author thomas
 */
public class TorqueJPPFTestSetup {

    public static void main(String... args) {
        new TorqueJPPFTestSetup().execute();
    }

    private void execute() {
        System.setProperty("jppf.config.plugin", JPPFConfigurator.class.getCanonicalName());

        startDriver();

//        startNodes();
    }

    private void startDriver() {
//        JPPFDriver.main("noLauncher");

//        ProcessLauncher processLauncher = new ProcessLauncher("org.jppf.server.JPPFDriver");
//        processLauncher.run();

        TorqueLocalRunner runner = new TorqueLocalRunner();
        runner.start();

        try {
            Thread.sleep(5000);
            System.out.println("runner.stopDriver()");
            runner.stopDriver();
        } catch (InterruptedException ex) {
            Logger.getLogger(TorqueJPPFTestSetup.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void startNodes() {
        System.out.println("Start node");
        new TorqueJPPFNodeDeployer().execute(5);
    }

    private class TorqueLocalRunner extends Thread {

        private TorqueJPPFDriverDeployer deployer;

        @Override
        public void run() {
            System.out.println("Start driver");
            deployer = new TorqueJPPFDriverDeployer();
            deployer.runLocally().execute();
        }

        public void stopDriver() {
            deployer.stopLocalDriver();
        }
    }
}
