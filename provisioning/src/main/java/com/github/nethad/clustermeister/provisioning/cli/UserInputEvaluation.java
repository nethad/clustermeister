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
package com.github.nethad.clustermeister.provisioning.cli;

/**
 *
 * @author thomas
 */
public class UserInputEvaluation {
    private static final String COMMAND_HELP = "help";
    private static final String COMMAND_HELP_QUESTIONMARK = "?";
    private static final String COMMAND_STATE = "state";
    private static final String COMMAND_SHUTDOWN = "shutdown";
    private static final String COMMAND_ADDNODE = "addnode";
    
    private final Provisioning provisioning;

    public UserInputEvaluation(Provisioning provisioning) {
        this.provisioning = provisioning;
    }

    public void evaluate(String userInput) {
        if (userInput.equalsIgnoreCase(COMMAND_HELP) || userInput.equalsIgnoreCase(COMMAND_HELP_QUESTIONMARK)) {
            help();
        } else if (userInput.equalsIgnoreCase(COMMAND_STATE)) {
            state();
        } else if (userInput.equalsIgnoreCase(COMMAND_SHUTDOWN)) {
            shutdown();
        } else if (userInput.equalsIgnoreCase(COMMAND_ADDNODE)) {
           addNode();  
        } else {
            unknownCommand();
        }
    }

    private void help() {
        CommandLineTextBuilder cltb = new CommandLineTextBuilder("Clustermeister Command Line Help");
        cltb.addLine(COMMAND_HELP, "Print out this help.");
        cltb.addLine(COMMAND_HELP_QUESTIONMARK, "Print out this help.");
        cltb.addLine(COMMAND_STATE, "Show the current state.");
        cltb.addLine(COMMAND_SHUTDOWN, "Shuts down all running drivers and nodes.");
        cltb.addLine(COMMAND_ADDNODE, "Adds a node to the current setup.");
        cltb.print();
    }

    private void unknownCommand() {
        System.out.println("Unknown command.");
    }

    private void state() {
        CommandLineTextBuilder cltb = new CommandLineTextBuilder("state:");
        cltb.addLine("provider:", provisioning.getProvider());
        cltb.addLine("running nodes", provisioning.getNumberOfRunningNodes());
        cltb.print();
    }

    private void shutdown() {
        provisioning.shutdown();
    }

    private void addNode() {
        provisioning.addNode();
    }
}
