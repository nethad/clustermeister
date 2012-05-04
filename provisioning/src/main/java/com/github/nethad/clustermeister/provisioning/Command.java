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

/**
 *
 * @author thomas
 */
public class Command {
    
    private String commandName;
    private String[] arguments;
    private String formattedArguments;
    private final String helpText;
    
    public Command(String commandName, String[] arguments, String helpText) {
        this.commandName = commandName;
        this.arguments = arguments;
        if (arguments == null) {
            formattedArguments = "";
        }
        this.helpText = helpText;
    }
    
    public String getCommandName() {
        return commandName;
    }
    
    public String getFormattedArguments() {
        if (formattedArguments == null) {
            buildFormattedArguments();
        }
        return formattedArguments;
    }
    
    public String[] getArguments() {
        return arguments;
    }
    
    public int getArgumentCount() {
        return arguments.length;
    }

    private void buildFormattedArguments() {
        StringBuilder sb = new StringBuilder();
        for (String arg : arguments) {
            sb.append("[").append(arg).append("] ");
        }
        formattedArguments = sb.toString();
    }
    
    public String getHelpText() {
        return helpText;
    }
    
}
