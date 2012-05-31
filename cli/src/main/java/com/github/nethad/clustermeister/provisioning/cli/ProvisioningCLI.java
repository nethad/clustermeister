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

import com.github.nethad.clustermeister.api.Loggers;
import com.github.nethad.clustermeister.api.impl.FileConfiguration;
import com.github.nethad.clustermeister.provisioning.Command;
import com.github.nethad.clustermeister.provisioning.CompositeCommand;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
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

    private static final Logger logger = LoggerFactory.getLogger(Loggers.CLI);
//    @Inject
//    @Named(Loggers.CLI)
//    private Logger logger;

    private static final String OPTION_HELP = "help";
    private static final String OPTION_CONFIG_FILE = "config";
//    private static final String CONFIG_FILE_NAME = "configuration.yml";
//    private static final String DEFAULT_CONFIG_FILE = System.getProperty("user.home") + "/.clustermeister/" + CONFIG_FILE_NAME;
    private static final String OPTION_PROVIDER = "provider";
    private static final String DEFAULT_PROVIDER = "local";

    public static void main(String... args) {
        loadJDKLoggingConfiguration();
//        Injector injector = Guice.createInjector(new LoggerModule());
//        ProvisioningCLI provisioningCLI = injector.getInstance(ProvisioningCLI.class);
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
    private boolean showHelp;
    
    protected void startCLI(String[] args) {
            try {
                parseArguments(args);
                if (showHelp) {
                    usage();
                    return;
                }
                logger.info("Using configuration in "+configFilePath +" or create file if it does not exist.");
                logger.info("Using provider "+provider);
                userInputEvaluation = new UserInputEvaluation();
                provisioning = new Provisioning(configFilePath, provider, userInputEvaluation);
                userInputEvaluation.setProvisioning(provisioning);
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

    private void usage() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("cli.jar", options);
    }
    
    private void startREPL() {
        try {
            ConsoleReader reader = new ConsoleReader();
            reader.setBellEnabled(false);
            registerCompletors(reader);

            PrintWriter out = new PrintWriter(System.out);
            String line;
            while ((line = reader.readLine("cm$ ")) != null) {
                userInputEvaluation.evaluate(line);
                if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) {
                    break;
                }
                out.flush();
            }
        } catch (Exception ex) {
            logger.warn("Exception", ex);
        }
    }
    
    private void registerCompletors(ConsoleReader reader) {
        List<String> completionTerms = Lists.newArrayList();
        Joiner joiner = Joiner.on(' ');
        for(String commandName : userInputEvaluation.commands()) {
            Command command = userInputEvaluation.getCommand(commandName);
            completionTerms.add(commandName);
            recursivelyExpandTerms(command, completionTerms, joiner);
        }
        reader.addCompletor(new SimpleCompletor(
                completionTerms.toArray(new String[0])));
        
    }
    
    private void recursivelyExpandTerms(Command command, List<String> completionTerms, Joiner joiner) {
        if(command != null && command instanceof CompositeCommand) {
            CompositeCommand composite = command.as(CompositeCommand.class);
            for(Command subCommand : composite.getSubCommands()) {
                String expandedCommand = joiner.join(command.getCommandName(), 
                        subCommand.getCommandName());
                completionTerms.add(expandedCommand);
                recursivelyExpandTerms(subCommand, completionTerms, joiner);
            }
        }
    }

//    private String nextUserInput() {
//        System.out.print("cm$ ");
//        BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
//        while (true) {
//            try {
//                return userInput.readLine();
//            } catch (IOException ex) {
//                logger.error("Could not read user input.", ex);
//            }
//            return nextUserInput();
//        }
//    }

    protected void parseArguments(String[] args) throws ParseException {
        CommandLineParser parser = new PosixParser();
        CommandLine cmd = parser.parse(getOptions(), args);
        showHelp = cmd.hasOption(OPTION_HELP);
        configFilePath = cmd.getOptionValue(OPTION_CONFIG_FILE, FileConfiguration.DEFAULT_CONFIG_FILE);
        provider = Provider.fromString(cmd.getOptionValue(OPTION_PROVIDER, DEFAULT_PROVIDER));
    }

    private Options getOptions() {
        if (options == null) {
            options = new Options();
            options.addOption("h", OPTION_HELP, false, "show this help text.");
            options.addOption("c", OPTION_CONFIG_FILE, true, "define the path to your "+FileConfiguration.CONFIG_FILE_NAME+", default: "+FileConfiguration.DEFAULT_CONFIG_FILE);
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
