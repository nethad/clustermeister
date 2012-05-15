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
package com.github.nethad.clustermeister.provisioning.ec2.commands;

import com.github.nethad.clustermeister.provisioning.AbstractExecutableCommand;
import com.github.nethad.clustermeister.provisioning.CommandLineHandle;
import com.github.nethad.clustermeister.provisioning.ec2.AmazonCommandLineEvaluation;
import com.github.nethad.clustermeister.provisioning.ec2.AmazonNodeManager;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFManagementByJobsClient;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Super-class of all Amazon provisioning provider CLI commands.
 * 
 * <p>
 * Offers access to the Amazon provisioning infrastructure and some commonly 
 * used utilities.
 * </p>
 *
 * @author daniel, thomas
 */
public abstract class AbstractAmazonExecutableCommand extends AbstractExecutableCommand {
    
    /**
     * Shared logger for subclasses.
     */
    protected final static Logger logger = 
            LoggerFactory.getLogger(AbstractAmazonExecutableCommand.class);
 
    /**
     * Visual line separator for command line output layouting.
     */
    protected static final String SEPARATOR_LINE = "-------------------------------------------------";
    
    private AmazonCommandLineEvaluation commandLineEvaluation;
    
    /**
     * Creates a new command with a command line evaluation reference for access 
     * to the Clustermeister provisioning infrastructure.
     * 
     * @param commandName   the name of the command.
     * @param arguments the arguments of the command, may be null.
     * @param helpText the help text of the command.
     * @param commandLineEvaluation the command line evaluation instance reference.
     */
    public AbstractAmazonExecutableCommand(String commandName, String[] arguments, 
            String helpText, AmazonCommandLineEvaluation commandLineEvaluation) {
        
        super(commandName, arguments, helpText);
        this.commandLineEvaluation = commandLineEvaluation;
    }
    
    @Override
    protected CommandLineHandle getCommandLineHandle() {
        return commandLineEvaluation.getCommandLineHandle();
    }
    
    /**
     * The node manager allows to interact with the provisioning infrastructure.
     * 
     * @return the Amazon node manager.
     */
    protected AmazonNodeManager getNodeManager() {
        return commandLineEvaluation.getNodeManager();
    }

    /**
     * The management client allows performing management tasks 
     * (such as shutdown or restart) on running JPPF nodes.
     * 
     * @return the Amazon management client.
     */
    protected JPPFManagementByJobsClient getManagementClient() {
        return commandLineEvaluation.getManagementClient();
    }
    
    /**
     * Wait for a list of futures to complete.
     * 
     * <p>
     * The futures are considered as failed when they return null or fail to return.
     * </p>
     * @param futures the futures to wait for.
     * @param interruptedMessage 
     *      Log this message when the thread waiting for the futures to return 
     *      is interrupted. The exception's stack trace is appended to this message.
     * @param executionExceptionMessage 
     *      Log this message when the thread waiting for the futures throws an 
     *      exception while waiting. The exception's stack trace is appended to 
     *      this message.
     * @param unsuccessfulFuturesMessage 
     *      Log this message when at least one future failed (or returned null).
     *      Can be a formatted string where '{}' is replaced with the number of 
     *      failed futures.
     * 
     */
    protected void waitForFuturesToComplete(List<ListenableFuture<? extends Object>> futures, 
            String interruptedMessage, String executionExceptionMessage, 
            String unsuccessfulFuturesMessage) {
        try {
            List<Object> startedNodes = Futures.successfulAsList(futures).get();
            int failedNodes = Iterables.frequency(startedNodes, null);
            if(failedNodes > 0) {
                logger.warn(unsuccessfulFuturesMessage, failedNodes);
            }
        } catch (InterruptedException ex) {
            logger.warn(interruptedMessage, ex);
        } catch (ExecutionException ex) {
            logger.warn(executionExceptionMessage, ex);
        }
    }
}
