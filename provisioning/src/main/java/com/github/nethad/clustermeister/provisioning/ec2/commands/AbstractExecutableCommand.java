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

import com.github.nethad.clustermeister.provisioning.Command;
import com.github.nethad.clustermeister.provisioning.ec2.AmazonCommandLineEvaluation;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author daniel
 */
public abstract class AbstractExecutableCommand extends Command {
    
    final static Logger logger = 
            LoggerFactory.getLogger(AbstractExecutableCommand.class);

    protected AmazonCommandLineEvaluation commandLineEvaluation;
    
    public AbstractExecutableCommand(String commandName, String[] arguments, 
            String helpText, AmazonCommandLineEvaluation commandLineEvaluation) {
        super(commandName, arguments, helpText);
        
        this.commandLineEvaluation = commandLineEvaluation;
    }
    
    /**
     * Execute the command.
     * 
     * @param tokenizer The command line arguments.
     */
    public abstract void execute(StringTokenizer tokenizer);
    
    /**
     * Wait for a list of futures to complete.
     * 
     * NOTE: for internal use only.
     * 
     */
    void waitForFuturesToComplete(List<ListenableFuture<? extends Object>> futures, 
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
