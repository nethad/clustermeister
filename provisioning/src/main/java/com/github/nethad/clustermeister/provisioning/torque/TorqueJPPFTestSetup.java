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
import java.io.*;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import org.jppf.process.ProcessLauncher;
import org.jppf.server.JPPFDriver;
import org.jppf.utils.JPPFConfiguration;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas
 */
public class TorqueJPPFTestSetup {

    public static void main(String... args) {
        new TorqueJPPFTestSetup().execute();
    }

    private void execute() {
        String logFileConfig = "target/classes/config/logging-driver.properties";
        
        System.setProperty("jppf.config.plugin", JPPFConfigurator.class.getCanonicalName());
//        System.setProperty("log4j.rootLogger", "DEBUG, JPPF");
//        System.setProperty("log4j.configuration", "config/log4j-driver.properties");
//        System.setProperty("java.util.logging.config.file", logFileConfig);
        
//        System.out.println("logging-driver.properties = "+new File(logFileConfig).exists());
        try {
            LogManager.getLogManager().readConfiguration(getPropertiesInputStream());
        } catch (IOException ex) {
            Logger.getLogger(TorqueJPPFTestSetup.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(TorqueJPPFTestSetup.class.getName()).log(Level.SEVERE, null, ex);
        }
        
//        System.setProperty("jppf.plugin.config", JPPFConfigurator.class.getCanonicalName());
        
//        System.setProperty("jppf.config", "");
        
//        org.jppf.comm.socket.SocketWrapper
        
        startDriver();
        
//        startNodes();
    }
    
    private InputStream getPropertiesInputStream() {
        try {
            Properties properties = new Properties();
            String debugLevel = "FINEST";
            properties.setProperty("handlers", "java.util.logging.FileHandler");
            properties.setProperty(".level", debugLevel);
            
            properties.setProperty("java.util.logging.FileHandler.pattern", "logging-driver.log");
            properties.setProperty("java.util.logging.FileHandler.level", debugLevel);
            properties.setProperty("java.util.logging.FileHandler.count", "1");
            properties.setProperty("java.util.logging.FileHandler.formatter", "org.jppf.logging.jdk.JPPFLogFormatter");
            properties.setProperty("java.util.logging.FileHandler.append", "false");
            properties.setProperty("java.util.logging.ConsoleHandler.level", debugLevel);
            properties.setProperty("java.util.logging.ConsoleHandler.formatter", "java.util.logging.SimpleFormatter");
            properties.setProperty("org.jppf.logging.jdk.JmxHandler.level", debugLevel);
            properties.setProperty("org.jppf.logging.jdk.JmxHandler.formatter", "org.jppf.logging.jdk.JPPFLogFormatter");
            properties.setProperty("org.jppf.level", debugLevel);
            properties.setProperty("org.jppf.utils.level", debugLevel);
            properties.setProperty("org.jppf.comm.discovery.level", debugLevel);
            properties.setProperty("org.jppf.server.JPPFDriver", debugLevel);
        
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            properties.store(baos, "");
            return new ByteArrayInputStream(baos.toByteArray());
        } catch (IOException ex) {
            Logger.getLogger(TorqueJPPFTestSetup.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private void startDriver() {
        
        
//        new Thread(new Runnable() {
//
//            @Override
//            public void run() {
                JPPFDriver.main("noLauncher");
//            }
//        }).start();
        
//        ProcessLauncher processLauncher = new ProcessLauncher("org.jppf.server.JPPFDriver");
//        processLauncher.run();
//        System.exit(0);
        
//        for (int i=0; i<60; i++) {
//            try {
//                Thread.sleep(1000);
//                System.out.println("Still alive.");
//            } catch (InterruptedException ex) {
//                Logger.getLogger(TorqueJPPFTestSetup.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
        
//        try {
//            Thread.sleep(3000);
//            runner.stopDriver();
//            Thread.sleep(1000);
//            runner.printStatus();
//        } catch (InterruptedException ex) {
//            Logger.getLogger(TorqueJPPFTestSetup.class.getName()).log(Level.SEVERE, null, ex);
//        }
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

        private void printStatus() {
            deployer.printProcessStatus();
        }
        
    }
}
