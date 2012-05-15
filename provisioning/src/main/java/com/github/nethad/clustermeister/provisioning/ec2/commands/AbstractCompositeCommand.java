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
package com.github.nethad.clustermeister.provisioning.ec2.commands;

import com.github.nethad.clustermeister.provisioning.AbstractExecutableCommand;
import com.github.nethad.clustermeister.provisioning.CommandLineArguments;
import com.github.nethad.clustermeister.provisioning.ec2.AmazonCommandLineEvaluation;
import com.google.common.collect.Maps;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.SortedMap;

/**
 * A Command that holds a set of subcommands and delegates execution to 
 * subcommands based on the first argument matching the subcommand name.
 * <p>
 * Useful to collect a number of commands with the same prefix.
 * </p>
 * <p>
 * Example: 
 * <table border="1">
 *   <tr><th>as top level commands</th><th>as subcommands</th></tr>
 *   <tr><td>get<i>instances</i></td><td>get <i>instances</i></td></tr>
 *   <tr><td>get<i>locations</i></td><td>get <i>locations</i></td></tr>
 *   <tr><td>get<i>profiles</i></td><td>get <i>profiles</i></td></tr>
 * </table>
 * </p>
 * @author daniel
 */
public abstract class AbstractCompositeCommand extends AbstractAmazonExecutableCommand {
    
    private SortedMap<String, AbstractExecutableCommand> subCommands = Maps.newTreeMap();
    
    /**
     * Creates a new command with a command line evaluation reference for access 
     * to the Clustermeister provisioning infrastructure.
     * 
     * @param commandName   the name of the command.
     * @param arguments the arguments of the command, may be null.
     * @param helpText the help text of the command.
     * @param commandLineEvaluation the command line evaluation instance reference.
     */
    public AbstractCompositeCommand(String commandName, String[] arguments, 
            String helpText, AmazonCommandLineEvaluation commandLineEvaluation) {
        super(commandName, arguments, helpText, commandLineEvaluation);
    }
    
    @Override
    public void execute(CommandLineArguments arguments) {
        if (arguments.argumentCount() < getArgumentCount()) {
            getCommandLineHandle().expectedArguments(getArguments());
            return;
        }
        
        Scanner scanner = arguments.asScanner();
        final String command = scanner.next();
        
        AbstractExecutableCommand subCommand = subCommands.get(command); 
        if(subCommand != null) {
            String remainingArguments = "";
            if(scanner.hasNext()) {
                remainingArguments = scanner.nextLine();
            }
            subCommand.execute(new CommandLineArguments(remainingArguments));
        } else {
            getCommandLineHandle().print("Unknown command '%s'", command);
        }
    }

    @Override
    public String getHelpText() {
        return String.format("%s\n\tsubcommands:%s", super.getHelpText(), getSubCommandsHelp());
    }
    
    /**
     * Register a sub command to this composite command.
     * <p>
     * When the first argument to this command matches the subcommand's name, 
     * the registered command is executed with the remaining arguments.
     * </p>
     * @param command   the command to register. 
     */
    protected void registerCommand(AbstractAmazonExecutableCommand command) {
        subCommands.put(command.getCommandName(), command);
    }
    
    private String getSubCommandsHelp() {
        StringBuilder sb = new StringBuilder();
        Iterator<Map.Entry<String, AbstractExecutableCommand>> iterator = 
                subCommands.entrySet().iterator();
        while(iterator.hasNext()) {
            Map.Entry<String, AbstractExecutableCommand> cEntry = iterator.next();
            sb.append("\n\t\t").
                    append(cEntry.getKey());
            String[] arguments = cEntry.getValue().getArguments();
            if(arguments != null) {
                for(String argument : arguments) {
                    sb.append(" [").append(argument).append("]");
                }
            }
            sb.append("\n\t\t\t").append(cEntry.getValue().getHelpText());
        }
        
        return sb.toString();
    }
}
