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

import com.google.common.annotations.VisibleForTesting;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.logging.LogManager;
import jline.ConsoleReader;
import jline.SimpleCompletor;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas
 */
public class ProvisioningCLI {

    private final Logger logger = LoggerFactory.getLogger(ProvisioningCLI.class);

    private static final String OPTION_CONFIG_FILE = "config";
    private static final String DEFAULT_CONFIG_FILE = System.getProperty("user.home") + "/.clustermeister/configuration.properties";
    private static final String OPTION_PROVIDER = "provider";
    private static final String DEFAULT_PROVIDER = "torque";

    public static void main(String... args) {
        loadJDKLoggingConfiguration();
        new ProvisioningCLI().startCLI(args);
    }
    private String configFilePath;
    private Options options;
    private Provider provider;
    private Provisioning provisioning;
    private UserInputEvaluation userInputEvaluation;

    private static void loadJDKLoggingConfiguration() {
        try {
            LogManager.getLogManager().readConfiguration(
                    ProvisioningCLI.class.getResourceAsStream("/jdk-logging.properties"));
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (SecurityException ex) {
            ex.printStackTrace();
        }
    }
    
    protected void startCLI(String[] args) {
//        if (args.length == 0) {
//            HelpFormatter formatter = new HelpFormatter();
//            formatter.printHelp( "clustermeister", getOptions());
//        } else {
            try {
                parseArguments(args);
                logger.info("Using configuration in "+configFilePath);
                logger.info("Using provider "+provider);
                provisioning = new Provisioning(configFilePath, provider);
                provisioning.execute();
                startREPL();
            } catch (ParseException ex) {
                logger.error("Terminated.", ex);
            }
//        }
        // TODO this is necessary because JPPFClient still has running threads which 
        // won't shut down with close().
        System.exit(0);
    }
    
    private void startREPL() {
        try {
            ConsoleReader reader = new ConsoleReader();
            reader.setBellEnabled(false);
            reader.addCompletor(new SimpleCompletor(UserInputEvaluation.commands()));

            PrintWriter out = new PrintWriter(System.out);
            String line;
            userInputEvaluation = new UserInputEvaluation(provisioning);
            while ((line = reader.readLine("cm$ ")) != null) {
                userInputEvaluation.evaluate(line);
                if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) {
                    break;
                }
                out.flush();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String nextUserInput() {
        System.out.print("cm$ ");
        BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            try {
                return userInput.readLine();
            } catch (IOException ex) {
                logger.error("Could not read user input.", ex);
            }
            return nextUserInput();
        }
    }

    protected void parseArguments(String[] args) throws ParseException {
        CommandLineParser parser = new PosixParser();
        CommandLine cmd = parser.parse(getOptions(), args);
        configFilePath = cmd.getOptionValue(OPTION_CONFIG_FILE, DEFAULT_CONFIG_FILE);
        provider = Provider.fromString(cmd.getOptionValue(OPTION_PROVIDER, DEFAULT_PROVIDER));
    }

    private Options getOptions() {
        if (options == null) {
            options = new Options();
            options.addOption("c", OPTION_CONFIG_FILE, true, "define the path to your configuration.properties, default: "+DEFAULT_CONFIG_FILE);
            options.addOption("p", OPTION_PROVIDER, true, "specify the provider to use, either 'amazon' or 'torque', default: "+DEFAULT_PROVIDER);
        }
        return options;
    }
    
    @VisibleForTesting
    protected String getConfigFilePath() {
        return configFilePath;
    }
    
    @VisibleForTesting
    protected Provider getProvider() {
        return provider;
    }


}
