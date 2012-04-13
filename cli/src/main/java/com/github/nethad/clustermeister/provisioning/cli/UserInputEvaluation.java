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

import com.google.common.annotations.VisibleForTesting;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 *
 * @author thomas
 */
public class UserInputEvaluation {
    private static final String COMMAND_HELP = "help";
    private static final String COMMAND_HELP_QUESTIONMARK = "?";
    private static final String COMMAND_STATE = "state";
    private static final String COMMAND_SHUTDOWN = "shutdown";
    private static final String COMMAND_ADDNODES = "addnodes";
    private static final String COMMAND_EXIT = "exit";
    private static final String COMMAND_QUIT = "quit";
    
    private Map<String, String> commandHelpMap = new HashMap<String, String>();
    private CommandLineTextBuilder commandLineHelpText;
    
    private final Provisioning provisioning;

    public UserInputEvaluation(Provisioning provisioning) {
        this.provisioning = provisioning;
        buildCommandHelpMap();
    }
    
    public String[] commands() {
        return commandHelpMap.keySet().toArray(new String[]{});
    }

    public void evaluate(String userInput) {
        final StringTokenizer tokenizer = new StringTokenizer(userInput);
        if (!tokenizer.hasMoreTokens()) {
            return;
        }
        try {
            handleCommand(tokenizer);
        } catch (Exception ex) {
            handleException(ex);
        }
    }
    
    @VisibleForTesting
    protected void handleCommand(StringTokenizer tokenizer) throws Exception {
        final String command = tokenizer.nextToken();
        if (commandHelpMap.containsKey(command)) {
            commandMarshalling(command, tokenizer);
        } else {
            provisioning.commandUnknownFallback(command, tokenizer);
        }
    }

    private void commandMarshalling(final String command, StringTokenizer tokenizer) {
        if (command.equals(COMMAND_HELP) || command.equals(COMMAND_HELP_QUESTIONMARK)) {
            command_help(tokenizer);
        } else if (command.equals(COMMAND_STATE)) {
            provisioning.commandState(tokenizer);
        } else if (command.equals(COMMAND_SHUTDOWN)) {
            provisioning.commandShutdown(tokenizer);
        } else if (command.equals(COMMAND_ADDNODES)) {
            provisioning.commandAddnodes(tokenizer);
        }
    }

    private void buildCommandHelpMap() {
        commandHelpMap.put(COMMAND_HELP, "Print out this help.");
        commandHelpMap.put(COMMAND_HELP_QUESTIONMARK, "Print out this help.");
        commandHelpMap.put(COMMAND_STATE, "Show the current state.");
        commandHelpMap.put(COMMAND_SHUTDOWN, "Shuts down all running drivers and nodes.");
        commandHelpMap.put(COMMAND_ADDNODES, provisioning.helpText(COMMAND_ADDNODES));
        commandHelpMap.put(COMMAND_EXIT, "Exits this command line.");
        commandHelpMap.put(COMMAND_QUIT, "Exits this command line.");
    }
    
    private void command_help(StringTokenizer tokenizer) {
        if (commandLineHelpText == null) {
            commandLineHelpText = new CommandLineTextBuilder("Clustermeister Command Line Help");
            for (Map.Entry<String, String> entry : commandHelpMap.entrySet()) {
                commandLineHelpText.addLine(entry.getKey(), entry.getValue());
            }
        }
        commandLineHelpText.print();
        provisioning.commandHelp(tokenizer);
    }

    private void unknownCommand() {
        System.out.println("Unknown command.");
    }

    private void handleException(Exception ex) {
        System.out.println("Exception: "+ex.getMessage());
        ex.printStackTrace();
    }

}
