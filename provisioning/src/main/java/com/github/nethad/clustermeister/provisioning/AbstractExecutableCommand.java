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

import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 *
 * @author daniel
 */
public abstract class AbstractExecutableCommand extends Command {
    protected static final String SEPARATOR_LINE = "-------------------------------------------------";
    private Scanner oldScanner;

//    protected CommandLineEvaluation commandLineEvaluation;
    
    public AbstractExecutableCommand(String commandName, String[] arguments, 
            String helpText) {
        super(commandName, arguments, helpText);
    }
    
    protected abstract CommandLineHandle getCommandLineHandle();
    
    /**
     * 
     * @param tokenizer
     * @return
     * @deprecated Use {@link Scanner#nextInt()} instead.
     */
    @Deprecated()
    protected int nextTokenAsInteger(StringTokenizer tokenizer) {
        return Integer.valueOf(tokenizer.nextToken());
    }
    
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
    public abstract void execute(CommandLineArguments arguments);
    
}
