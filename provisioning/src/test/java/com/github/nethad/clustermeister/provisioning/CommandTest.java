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

import org.junit.AfterClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import static org.hamcrest.Matchers.is;

/**
 *
 * @author thomas
 */
public class CommandTest {
    private Command command;
    
    @Before
    public void setup() {
        command = new Command("command", new String[]{"arg1", "arg2"}, "help text");
    }

    @Test
    public void initialization() {
        assertThat(command.getCommandName(), is("command"));
        assertThat(command.getArgumentCount(), is(2));
        assertThat(command.getFormattedArguments().trim(), is("[arg1] [arg2]"));
        assertThat(command.getHelpText(), is("help text"));
    }
}
