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

import com.github.nethad.clustermeister.provisioning.Command;
import com.github.nethad.clustermeister.provisioning.CommandLineArguments;
import com.github.nethad.clustermeister.provisioning.CommandRegistry;
import com.google.common.annotations.VisibleForTesting;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 *
 * @author thomas
 */
public class UserInputEvaluation implements CommandRegistry {
    private static final String COMMAND_HELP = "help";
    private static final String COMMAND_HELP_QUESTIONMARK = "?";
    private static final String COMMAND_STATE = "state";
    private static final String COMMAND_SHUTDOWN = "shutdown";
    private static final String COMMAND_EXIT = "exit";
    private static final String COMMAND_QUIT = "quit";
    
//    private Map<String, String> commandHelpMap = new HashMap<String, String>();
    private CommandLineTextBuilder commandLineHelpText;
    
    private Provisioning provisioning;
    
    private Map<String, Command> commands = new HashMap<String, Command>();

    public UserInputEvaluation() {
        buildCommandHelpMap();
    }
    
    public void setProvisioning(Provisioning provisioning) {
        this.provisioning = provisioning;
    }
    
    public String[] commands() {
        return commands.keySet().toArray(new String[]{});
    }
    
    @Override
    public void registerCommand(Command command) {
        commands.put(command.getCommandName(), command);
    }
    
    @Override
    public void unregisterCommand(Command command) {
        commands.remove(command.getCommandName());
    }

    @Override
    public Command getCommand(String commandName) {
        return commands.get(commandName);
    }
    
    public void evaluate(String userInput) {
        CommandLineArguments arguments = new CommandLineArguments(userInput);
        if (!arguments.asScanner().hasNext()) {
            return;
        }
        try {
            handleCommand(arguments);
        } catch (Exception ex) {
            handleException(ex);
        }
    }
    
    @VisibleForTesting
    protected void handleCommand(CommandLineArguments arguments) throws Exception {
        Scanner scanner = arguments.asScanner();
        final String command = scanner.next();
        
        String fullLine = "";
        if (scanner.hasNext()) {
            fullLine = scanner.nextLine();
        }
        if (commands.containsKey(command)) {
            commandMarshalling(command, fullLine);
        } else {
//            provisioning.commandUnknownFallback(command, tokenizer);
            unknownCommand();
        }
    }

    private void commandMarshalling(final String command, String arguments) {
        if (command.equals(COMMAND_HELP) || command.equals(COMMAND_HELP_QUESTIONMARK)) {
            help();
        } else if (command.equals(COMMAND_STATE)) {
            provisioning.commandState(new CommandLineArguments(arguments));
        } else if (command.equals(COMMAND_SHUTDOWN)) {
            provisioning.commandShutdown(new CommandLineArguments(arguments));
        } else {
            provisioning.commandUnknownFallback(command, new CommandLineArguments(arguments));
        }
    }

    private void buildCommandHelpMap() {
//        commandHelpMap.put(COMMAND_HELP, "Print out this help.");
//        commandHelpMap.put(COMMAND_HELP_QUESTIONMARK, "Print out this help.");
//        commandHelpMap.put(COMMAND_STATE, "Show the current state.");
//        commandHelpMap.put(COMMAND_SHUTDOWN, "Shuts down all running drivers and nodes.");
//        commandHelpMap.put(COMMAND_ADDNODES, provisioning.helpText(COMMAND_ADDNODES));
//        commandHelpMap.put(COMMAND_EXIT, "Exits this command line.");
//        commandHelpMap.put(COMMAND_QUIT, "Exits this command line.");
        
        registerCommand(new Command(COMMAND_HELP, null, "Print out this help."));
        registerCommand(new Command(COMMAND_HELP_QUESTIONMARK, null, "Print out this help."));
        registerCommand(new Command(COMMAND_STATE, null, "Show the current state."));
        registerCommand(new Command(COMMAND_SHUTDOWN, null, "Shuts down all running drivers and nodes."));
        
        registerCommand(new Command(COMMAND_EXIT, null, "Exits this command line."));
        registerCommand(new Command(COMMAND_QUIT, null, "Exits this command line."));
    }
    
    private void help() {
        if (commandLineHelpText == null) {
            commandLineHelpText = new CommandLineTextBuilder("Clustermeister Command Line Help");
            for (Command command : commands.values()) {
                commandLineHelpText.addLine(command.getCommandName(), command.getFormattedArguments(), command.getHelpText());
            }
        }
        commandLineHelpText.print();
    }

    private void unknownCommand() {
        System.out.println("Unknown command.");
    }

    private void handleException(Exception ex) {
        System.out.println("Exception: "+ex.getMessage());
        ex.printStackTrace();
    }

}
