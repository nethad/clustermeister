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
package com.github.nethad.clustermeister.provisioning.ec2;

import java.util.concurrent.Callable;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.Template;

/**
 * Builds Templates.
 *
 * @author daniel
 */
abstract class AmazonTemplateBuilder implements Callable<Template> {
	
	final ComputeServiceContext context;

	public AmazonTemplateBuilder(ComputeServiceContext context) {
		this.context = context;
	}
	
	abstract Template buildTemplate();
	
	@Override
	public Template call() throws Exception {
		return buildTemplate();
	}
	
}
