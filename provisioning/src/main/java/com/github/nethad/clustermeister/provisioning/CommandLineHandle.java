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
 * An object to encapsulate command line output.
 * 
 * @author thomas
 */
public class CommandLineHandle {
    private final CommandRegistry commandRegistry;

    public CommandLineHandle(CommandRegistry commandRegistry) {
        this.commandRegistry = commandRegistry;
    }
    
    public CommandRegistry getCommandRegistry() {
        return commandRegistry;
    }
    
    public void expectedArguments(String[] argumentNames) {
        StringBuilder arguments = new StringBuilder();
        for (String argument : argumentNames) {
            arguments.append("[").append(argument).append("] ");
        }
        print(String.format("Expected %d arguments: %s", argumentNames.length, arguments.toString()));
    }
    
    public void print(String line) {
        System.out.println(line);
    }
    
    public void print(String formatString, Object... args) {
        print(String.format(formatString, args));
    }
    
}
