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
import org.jclouds.compute.domain.OsFamily;
import org.jclouds.compute.domain.Template;
import org.jclouds.ec2.domain.InstanceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds a Template for an Amazon Linux T1 Micro Instance.
 *
 * @author daniel
 */
class AmazonT1MicroTemplateBuilder extends AmazonTemplateBuilder {
	private final static Logger logger =
			LoggerFactory.getLogger(AmazonT1MicroTemplateBuilder.class);

	private final String locationId;
	
	
	public AmazonT1MicroTemplateBuilder(ComputeServiceContext context, 
			String locationId) {
		super(context);
		this.locationId = locationId;
	}

	@Override
	Template buildTemplate() {
		logger.info("Building Template[{}, {}, {}]...", 
                        new Object[]{locationId, InstanceType.T1_MICRO, OsFamily.AMZN_LINUX});
		Template template = context.getComputeService().templateBuilder().
                        locationId(locationId).hardwareId(InstanceType.T1_MICRO).
                        osFamily(OsFamily.AMZN_LINUX).build();
		logger.info("Template built.");

		return template;
	}
}
