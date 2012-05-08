package com.github.nethad.clustermeister.provisioning;

import java.util.StringTokenizer;

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

/**
 * For each provisioning backend (provider), a CommandLineEvaluation must be provided, 
 * which reacts to built-in commands and may take additional arguments in a {@link StringTokenizer}.
 * 
 * Commands which are specific to the backend may be handled via {@link #handleCommand(java.lang.String, java.util.StringTokenizer) }.
 * @author thomas
 */
public interface CommandLineEvaluation {
    
    public static final String COMMAND_HELP = "help";
    public static final String COMMAND_HELP_QUESTIONMARK = "?";
    public static final String COMMAND_STATE = "state";
    public static final String COMMAND_SHUTDOWN = "shutdown";
    public static final String COMMAND_EXIT = "exit";
    public static final String COMMAND_QUIT = "quit";
    
//    /**
//     * Add nodes to the infrastructure.
//     * @param tokenizer Additional parameters
//     * @param driverHost the host or IP address for the driver
//     */
//    public void addNodes(StringTokenizer tokenizer, String driverHost);
    
    /**
     * Print the current state for this backend.
     * @param tokenizer Additional parameters
     */
    public void state(StringTokenizer tokenizer);
    
    /**
     * Shutdown this infrastructure.
     * @param tokenizer Additional parameters
     */
    public void shutdown(StringTokenizer tokenizer);
    
//    /**
//     * Print a help text.
//     * @param tokenizer Additional parameters
//     */
//    public void help(StringTokenizer tokenizer);
    
    /**
     * Handle commands which are not shared by other backends.
     * @param command the first argument (=command) provided by the user
     * @param tokenizer Additional parameters entered by the user
     */
    public void handleCommand(String command, StringTokenizer tokenizer);
    
//    /**
//     * Returns a help text for the given command. This is only needed for commands handled by {@link #handleCommand(java.lang.String, java.util.StringTokenizer) }
//     * or built-in commands that take arguments.
//     * @param command
//     * @return 
//     */
//    public String helpText(String command);
    
    public CommandLineHandle getCommandLineHandle();
    
}
