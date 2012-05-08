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

import java.util.Scanner;
import org.junit.AfterClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import static org.hamcrest.Matchers.is;


/**
 *
 * @author thomas
 */
public class AbstractExecutableCommandTest {
    private AbstractExecutableCommand executableCommand;
    
    @Before
    public void setup() {
        executableCommand = new AbstractExecutableCommand("command", new String[2], "") {
            @Override
            protected CommandLineHandle getCommandLineHandle() { return new CommandLineHandle(null); }
            @Override
            public void execute(CommandLineArguments arguments) {}
        };
    }

    @Test
    public void isArgumentsCountFalseScanner() {
        assertThat(executableCommand.isArgumentsCountFalse(new CommandLineArguments("foo")), is(true));
        assertThat(executableCommand.isArgumentsCountFalse(new CommandLineArguments("foo bar")), is(false));
        assertThat(executableCommand.isArgumentsCountFalse(new CommandLineArguments("foo bar baz")), is(true));
    }

    @Test
    public void isArgumentsCountFalseScanner_unmodifiedScanner() {
        CommandLineArguments commandLineArguments = new CommandLineArguments("foo bar");
        assertThat(executableCommand.isArgumentsCountFalse(commandLineArguments), is(false));
        Scanner scanner = commandLineArguments.asScanner();
        assertThat(scanner.next(), is("foo"));
        assertThat(scanner.next(), is("bar"));
        assertThat(scanner.hasNext(), is(false));
    }
    
}
