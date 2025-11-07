/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.agentscope.runtime.engine.schemas.agent;

/**
 * Run status enumeration
 * Corresponds to the RunStatus class in agent_schemas.py of the Python version
 */
public class RunStatus {
    
    public static final String CREATED = "created";
    public static final String IN_PROGRESS = "in_progress";
    public static final String COMPLETED = "completed";
    public static final String CANCELED = "canceled";
    public static final String FAILED = "failed";
    public static final String REJECTED = "rejected";
    public static final String UNKNOWN = "unknown";
    
    private RunStatus() {
        // Utility class, instantiation not allowed
    }
}
