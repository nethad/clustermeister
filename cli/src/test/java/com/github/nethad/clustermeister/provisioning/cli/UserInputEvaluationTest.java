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

import com.github.nethad.clustermeister.provisioning.CommandLineEvaluation;
import java.util.StringTokenizer;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import org.junit.Before;

/**
 *
 * @author thomas
 */
public class UserInputEvaluationTest {
    private TestCommandLineEvaluation commandLineEvaluation;
    private Provisioning provisioning;
    private UserInputEvaluation userInputEvaluation;
    
//    private StringTokenizer setTokenizer;

    @Before
    public void setup() {
//        provisioning = mock(Provisioning.class);
//        setTokenizer = null;
        provisioning = new Provisioning(null, Provider.TEST);
        commandLineEvaluation = new TestCommandLineEvaluation();
        provisioning.setCommandLineEvaluation(commandLineEvaluation);
        userInputEvaluation = new UserInputEvaluation(provisioning);
    }
    
    @Test
    public void addnodesCommand() {
        userInputEvaluation.evaluate("addnodes 10 23");
        assertThat(commandLineEvaluation.getLastCommand(), is("addnodes"));
        assertThat(commandLineEvaluation.getLastTokenizer().countTokens(), is(2));
        assertThat(commandLineEvaluation.getLastTokenizer().nextToken(), is("10"));
        assertThat(commandLineEvaluation.getLastTokenizer().nextToken(), is("23"));
    }

    @Test
    public void stateCommand() {
        userInputEvaluation.evaluate("state");
        assertThat(commandLineEvaluation.getLastCommand(), is("state"));
        assertThat(commandLineEvaluation.getLastTokenizer().countTokens(), is(0));
    }
    
    @Test
    public void shutdownCommand() {
        userInputEvaluation.evaluate("shutdown");
        assertThat(commandLineEvaluation.getLastCommand(), is("shutdown"));
        assertThat(commandLineEvaluation.getLastTokenizer().countTokens(), is(0));
    }
    
    @Test
    public void helpCommand() {
        userInputEvaluation.evaluate("help");
        assertThat(commandLineEvaluation.getLastCommand(), is("help"));
        assertThat(commandLineEvaluation.getLastTokenizer().countTokens(), is(0));
    }
    
    @Test
    public void handleCommand() {
        userInputEvaluation.evaluate("newcommand arg1 arg2");
        assertThat(commandLineEvaluation.getLastCommand(), is("newcommand"));
        assertThat(commandLineEvaluation.getLastTokenizer().countTokens(), is(2));
        assertThat(commandLineEvaluation.getLastTokenizer().nextToken(), is("arg1"));
        assertThat(commandLineEvaluation.getLastTokenizer().nextToken(), is("arg2"));
    }
    
    class TestCommandLineEvaluation implements CommandLineEvaluation {
        
        private StringTokenizer lastTokenizer;
        private String lastCommand;
        
        public StringTokenizer getLastTokenizer() {
            return lastTokenizer;
        }
        
        public String getLastCommand() {
            return lastCommand;
        }

        @Override
        public void addNodes(StringTokenizer tokenizer, String driverHost) {
            lastCommand = "addnodes";
            lastTokenizer = tokenizer;
        }

        @Override
        public void state(StringTokenizer tokenizer) {
            lastCommand = "state";
            lastTokenizer = tokenizer;
        }

        @Override
        public void shutdown(StringTokenizer tokenizer) {
            lastCommand = "shutdown";
            lastTokenizer = tokenizer;
        }

        @Override
        public void help(StringTokenizer tokenizer) {
            lastCommand = "help";
            lastTokenizer = tokenizer;
        }

        @Override
        public void handleCommand(String command, StringTokenizer tokenizer) {
            lastTokenizer = tokenizer;
            this.lastCommand = command;
        }

        @Override
        public String helpText(String command) {
            this.lastCommand = command;
            return "";
        }
    }

}
