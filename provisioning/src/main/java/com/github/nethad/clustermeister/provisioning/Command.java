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
 * Represtens a command line command with its arguments and help text.
 * @author thomas
 */
public interface Command {

    /**
     * Get the expected argument count.
     * 
     * @return the expected number of arguments of this command.
     */
    int getArgumentCount();

    /**
     * The argument names.
     * 
     * @return the names of the arguments to this command. May be null.
     */
    String[] getArguments();

    /**
     * The name of this command.
     * 
     * The name triggers this command when entered on the CLI.
     * 
     * @return the command's name.
     */
    String getCommandName();

    /**
     * The arguments of this command for printing.
     * 
     * This is intended to be presented to a human user.
     * 
     * @return the formatted arguments.
     */
    String getFormattedArguments();

    /**
     * A text describing this command.
     * 
     * Intended to be presented to a human user.
     * 
     * @return the help text for this command.
     */
    String getHelpText();
    
    /**
     * Casts this instance to {@code clazz}.
     */
    <T extends Command> T as(Class<T> clazz);
}
