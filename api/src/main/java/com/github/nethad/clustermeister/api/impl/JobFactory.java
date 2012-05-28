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
package com.github.nethad.clustermeister.api.impl;

import com.github.nethad.clustermeister.api.Job;
import com.google.common.base.Optional;
import java.util.Map;

/**
 *
 * @author thomas
 */
public class JobFactory {
    public static final String DEFAULT_JOB_NAME = "Clustermeister Job";
    
    public static <T> Job<T> create(Map<String, Object> jobData) {
        return new JobImpl(DEFAULT_JOB_NAME, Optional.fromNullable(jobData));
    }
    
    public static <T> Job<T> create(String name, Map<String, Object> jobData) {
        return new JobImpl(name, Optional.fromNullable(jobData));
    }
    
}
