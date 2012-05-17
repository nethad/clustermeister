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
 * A base class for executable commands.
 * Classes that implement {@link ExecutableCommand} should extend this class.
 * @author daniel
 */
public abstract class AbstractExecutableCommand extends CommandImpl implements ExecutableCommand {
    
    /**
     * Creates a new executable command.
     * 
     * @param commandName   
     *      The name of this command (how to trigger this command on the CLI).
     * @param arguments 
     *      The specification of the expected arguments. {@code null} means none.
     * @param helpText 
     *      The help text that describes this command for a user.
     */
    public AbstractExecutableCommand(String commandName, String[] arguments, String helpText) {
        super(commandName, arguments, helpText);
    }
    
    /**
     * The command line handle can be used to interact with the command line client.
     * 
     * @return the command line context.
     */
    protected abstract CommandLineHandle getCommandLineHandle();
    
    /**
     * Check if the provided arguments match exactly the number of expected 
     * arguments as configured in the constructor.
     * 
     * <p>
     * Prints the expected argument specification to the command line if this 
     * test fails.
     * </p>
     * 
     * @param arguments The arguments from the command line.
     * @return 
     *      {@code true} if the provided arguments match exactly the number of 
     *      expected arguments, {@code false} otherwise.
     */
    protected boolean isArgumentsCountFalse(CommandLineArguments arguments) {
        if (arguments.argumentCount() != getArgumentCount()) {
            getCommandLineHandle().expectedArguments(getArguments());
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Execute the command.
     * 
     * @param tokenizer The command line arguments.
     */
    @Override
    public abstract void execute(CommandLineArguments arguments);
    
}
