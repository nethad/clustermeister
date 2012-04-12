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

import com.github.nethad.clustermeister.provisioning.rmi.NodeConnectionListener;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.management.JPPFSystemInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas
 */
public abstract class AbstractScenario implements NodeConnectionListener {
    
    private JPPFTestDriver driver;
    private JPPFTestNode node;
    private StringBuilder report = new StringBuilder("EXECUTION REPORT:\n");
    private boolean scenarioStarted = false;
    private boolean shuttingDown = false;
    private boolean startNode = true;
    
    private int numberOfNodes = 1;
    
    public abstract void runScenario() throws Exception;
    
    protected void addToReport(String key, Object value) {
        report.append(key).append("\t").append(value.toString()).append("\n");
    }

    protected void execute() throws InterruptedException {
        startDriver();
        if (startNode) {
            startNode();
        }
    }
    
    protected void execute(int numberOfNodes) throws InterruptedException {
        this.numberOfNodes = numberOfNodes;
        execute();
    }
    
    protected void execute(boolean startNode) throws InterruptedException {
        this.startNode = startNode;
        execute();
    }

    private void startDriver() {
        driver = new JPPFTestDriver(this);
        driver.startDriver();
    }

    private void startNode() throws RuntimeException {
        node = new JPPFTestNode();
        node.prepare();
        for (int i=0; i<numberOfNodes; i++) {
            node.startNewNode();
        }
    }

    public void runScenario_wrapper() throws Exception {
        try {
            runScenario();
        } finally {
            shutdown();
        }
    }
    
    private void shutdown() {
        shuttingDown = true;
        if (node != null) {
            node.shutdown();
        }
        driver.shutdown();
        System.out.println(report.toString());
        System.exit(0);
    }

    @Override
    public void onNodeConnected(JPPFManagementInfo jppfmi, JPPFSystemInformation jppfsi) {
        if (!scenarioStarted) {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        runScenario_wrapper();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }).start();
            scenarioStarted = true;
        }
    }

    @Override
    public void onNodeDisconnected(JPPFManagementInfo jppfmi) {
        if (!shuttingDown) {
            shutdown();
        }
    }
    
}
