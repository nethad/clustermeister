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

import com.github.nethad.clustermeister.api.Configuration;
import com.github.nethad.clustermeister.api.impl.FileConfiguration;
import com.github.nethad.clustermeister.provisioning.torque.TorqueJPPFDriverDeployer;
import com.google.common.annotations.VisibleForTesting;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.*;

/**
 *
 * @author thomas
 */
public class ProvisioningCLI {

    private static final String OPTION_CONFIG_FILE = "config";
    private static final String DEFAULT_CONFIG_FILE = System.getProperty("user.home") + "/.clustermeister/configuration.properties";
    private static final String OPTION_NUMBER_OF_NODES = "nodes";
    private static final String DEFAULT_NUMBER_OF_NODES = "3";
    private static final String OPTION_PROVIDER = "provider";
    private static final String DEFAULT_PROVIDER = "torque";

    public static void main(String... args) {
        new ProvisioningCLI().startCLI(args);
    }
    private String configFilePath;
    private int numberOfNodes;
    private Provider provider;

    protected void startCLI(String[] args) {
        if (args.length == 0) {
            startREPL();
        } else {
            try {
                parseArguments(args);
            } catch (ParseException ex) {
                System.err.println("Terminated: " + ex.getMessage());
            }
        }
    }

    private void startREPL() {
        throw new UnsupportedOperationException("REPL is not yet implemented");
    }

    protected void parseArguments(String[] args) throws ParseException {
        CommandLineParser parser = new PosixParser();
        CommandLine cmd = parser.parse(getOptions(), args);
        configFilePath = cmd.getOptionValue(OPTION_CONFIG_FILE, DEFAULT_CONFIG_FILE);
        numberOfNodes = Integer.parseInt(cmd.getOptionValue(OPTION_NUMBER_OF_NODES, DEFAULT_NUMBER_OF_NODES));
        provider = Provider.fromString(cmd.getOptionValue(OPTION_PROVIDER, DEFAULT_PROVIDER));
    }

    private Options getOptions() {
        Options options = new Options();
        options.addOption("c", OPTION_CONFIG_FILE, true, "define the path to your configuration.properties");
        options.addOption("n", OPTION_NUMBER_OF_NODES, true, "specify the number of nodes to be started");
        options.addOption("p", OPTION_PROVIDER, true, "specify the provider to use, either 'amazon' or 'torque'");
        return options;
    }
    
    @VisibleForTesting
    protected String getConfigFilePath() {
        return configFilePath;
    }
    
    @VisibleForTesting
    protected int getNumberOfNodes() {
        return numberOfNodes;
    }
    
    @VisibleForTesting
    protected Provider getProvider() {
        return provider;
    }
}
