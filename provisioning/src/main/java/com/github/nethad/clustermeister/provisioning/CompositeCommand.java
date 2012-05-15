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

import java.util.Collection;

/**
 * A command that contains several subcommands.
 *
 * @author daniel
 */
public interface CompositeCommand extends Command {
    
    /**
     * Register a sub command to this composite command.
     * 
     * <p>
     * When the first argument to this command matches the subcommand's name, 
     * the registered command is executed with the remaining arguments.
     * </p>
     * 
     * @param command   the command to register. 
     */
    public void registerSubCommand(ExecutableCommand command);
    
    /**
     * Unregister a sub command from this composite command.
     * 
     * </p>
     * 
     * @param command   the command to unregister. 
     */
    public void unregisterSubCommand(ExecutableCommand command);

    /**
     * Return registered subcommands.
     * 
     * @return the registered subcommands.
     */
    public Collection<ExecutableCommand> getSubCommands();
    
    /**
     * Return the command by name.
     * 
     * @param commandName the command name.
     * @return the command or {@code null} if none if found. 
     */
    public ExecutableCommand getSubCommand(String commandName);
    
}
