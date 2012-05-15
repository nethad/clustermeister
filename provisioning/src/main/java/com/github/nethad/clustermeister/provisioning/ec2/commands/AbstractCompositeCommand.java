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
 *
 * @author daniel
 */
public abstract class AbstractCompositeCommand extends AbstractAmazonExecutableCommand {
    
    private SortedMap<String, AbstractExecutableCommand> subCommands = Maps.newTreeMap();
    
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
            String remainingArguments = scanner.nextLine();
            subCommand.execute(new CommandLineArguments(remainingArguments));
        } else {
            getCommandLineHandle().print("Unknown command '%s'", command);
        }
    }

    @Override
    public String getHelpText() {
        return String.format("%s\n\tsubcommands:%s", super.getHelpText(), getSubCommandsHelp());
    }
    
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
                    append(cEntry.getKey()).
                    append(" - ").
                    append(cEntry.getValue().getHelpText());
        }
        
        return sb.toString();
    }
}
