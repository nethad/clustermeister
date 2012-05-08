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
package com.github.nethad.clustermeister.provisioning.cli;

import com.github.nethad.clustermeister.provisioning.Command;
import com.github.nethad.clustermeister.provisioning.CommandLineArguments;
import com.github.nethad.clustermeister.provisioning.CommandLineEvaluation;
import com.github.nethad.clustermeister.provisioning.CommandLineHandle;
import java.util.Scanner;
import java.util.StringTokenizer;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import org.junit.Before;

/**
 *
 * @author thomas
 */
public class UserInputEvaluationTest {
    private TestCommandLineEvaluation commandLineEvaluation;
    private Provisioning provisioning;
    private UserInputEvaluation userInputEvaluation;
    
    @Before
    public void setup() {
        userInputEvaluation = new UserInputEvaluation();
        provisioning = new Provisioning(null, Provider.TEST, userInputEvaluation);
        userInputEvaluation.setProvisioning(provisioning);
        commandLineEvaluation = new TestCommandLineEvaluation();
        provisioning.setCommandLineEvaluation(commandLineEvaluation);
    }
    
    @Test
    public void registerCommand() {
        assertThat(userInputEvaluation.commands(), not(hasItemInArray("mycommand")));
        userInputEvaluation.registerCommand(new Command("mycommand", null, ""));
        assertThat(userInputEvaluation.commands(), hasItemInArray("mycommand"));
    }
    
    @Test
    public void unregisterCommand() {
        userInputEvaluation.registerCommand(new Command("mycommand", null, ""));
        int numberOfCommands = userInputEvaluation.commands().length;
        userInputEvaluation.unregisterCommand(new Command("mycommand", null, ""));
        assertThat(userInputEvaluation.commands(), not(hasItemInArray("mycommand")));
        assertThat(userInputEvaluation.commands().length, is(numberOfCommands-1));
    }

    @Test
    public void stateCommand() {
        userInputEvaluation.evaluate("state");
        assertThat(commandLineEvaluation.getLastCommand(), is("state"));
        assertThat(commandLineEvaluation.getLastArguments().asScanner().hasNext(), is(false));
    }
    
    @Test
    public void shutdownCommand() {
        userInputEvaluation.evaluate("shutdown");
        assertThat(commandLineEvaluation.getLastCommand(), is("shutdown"));
        assertThat(commandLineEvaluation.getLastArguments().asScanner().hasNext(), is(false));
    }
    
    @Test
    public void handleCommand() {
        userInputEvaluation.registerCommand(new Command("newcommand", new String[]{"arg1", "arg2"}, null));
        userInputEvaluation.evaluate("newcommand arg1 arg2");
        assertThat(commandLineEvaluation.getLastCommand(), is("newcommand"));
        Scanner scanner = commandLineEvaluation.getLastArguments().asScanner();
        assertThat(scanner.next(), is("arg1"));
        assertThat(scanner.next(), is("arg2"));
    }
    
    class TestCommandLineEvaluation implements CommandLineEvaluation {
        
        private CommandLineArguments lastArguments;
        private String lastCommand;
        
        public CommandLineArguments getLastArguments() {
            return lastArguments;
        }
        
        public String getLastCommand() {
            return lastCommand;
        }

        @Override
        public void state(CommandLineArguments arguments) {
            lastCommand = "state";
            lastArguments = arguments;
        }

        @Override
        public void shutdown(CommandLineArguments arguments) {
            lastCommand = "shutdown";
            lastArguments = arguments;
        }

        @Override
        public void handleCommand(String command, CommandLineArguments arguments) {
            this.lastCommand = command;
            lastArguments = arguments;
        }

        @Override
        public CommandLineHandle getCommandLineHandle() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        
    }

}
