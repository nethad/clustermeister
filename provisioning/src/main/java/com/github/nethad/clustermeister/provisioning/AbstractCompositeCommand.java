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
package com.github.nethad.clustermeister.provisioning;

import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.SortedMap;

/**
 * A CommandImpl that holds a set of subcommands and delegates execution to 
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
public abstract class AbstractCompositeCommand extends AbstractExecutableCommand 
    implements CompositeCommand {
    
    private final CommandLineEvaluation commandLineEvaluation;
    private SortedMap<String, ExecutableCommand> subCommands = Maps.newTreeMap();
    
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
            String helpText, CommandLineEvaluation commandLineEvaluation) {
        super(commandName, arguments, helpText);
        this.commandLineEvaluation = commandLineEvaluation;
    }
    
    @Override
    public void execute(CommandLineArguments arguments) {
        if (arguments.argumentCount() < getArgumentCount()) {
            getCommandLineHandle().expectedArguments(getArguments());
            return;
        }
        
        Scanner scanner = arguments.asScanner();
        final String command = scanner.next();
        
        ExecutableCommand subCommand = subCommands.get(command); 
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
    
    @Override
    public void registerSubCommand(ExecutableCommand command) {
        subCommands.put(command.getCommandName(), command);
    }
    
    @Override
    public void unregisterSubCommand(ExecutableCommand command) {
        subCommands.remove(command.getCommandName());
    }
    
    @Override
    public Collection<ExecutableCommand> getSubCommands() {
        return Collections.<ExecutableCommand>unmodifiableCollection(subCommands.values());
    }

    @Override
    public ExecutableCommand getSubCommand(String commandName) {
        return subCommands.get(commandName);
    }
    
    @Override
    protected CommandLineHandle getCommandLineHandle() {
        return commandLineEvaluation.getCommandLineHandle();
    }
    
    private String getSubCommandsHelp() {
        StringBuilder sb = new StringBuilder();
        Iterator<Map.Entry<String, ExecutableCommand>> iterator = 
                subCommands.entrySet().iterator();
        while(iterator.hasNext()) {
            Map.Entry<String, ExecutableCommand> cEntry = iterator.next();
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
