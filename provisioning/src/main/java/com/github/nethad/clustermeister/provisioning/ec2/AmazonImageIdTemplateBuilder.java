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

import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds a Template from an Image Id.
 * 
 * NOTE: When supplying an Image Id, the query can be sped up like described 
 * here (see "Lazy Image Fetching"): 
 * http://www.jclouds.org/documentation/userguide/using-ec2 
 *
 * @author daniel
 */
class AmazonImageIdTemplateBuilder extends AmazonTemplateBuilder {
	private final static Logger logger =
				LoggerFactory.getLogger(AmazonImageIdTemplateBuilder.class);
	private final String imageId;

	public AmazonImageIdTemplateBuilder(ComputeServiceContext context, 
			String imageId) {
		super(context);
		this.imageId = imageId;
	}
	
	@Override
	Template buildTemplate() {
		logger.info("Building Template from Image {}...", imageId);
		Template template = context.getComputeService().templateBuilder().
				imageId(imageId).build();
		logger.info("Template built.");

		return template;
	}
	
}
