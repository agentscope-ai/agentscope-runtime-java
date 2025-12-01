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

package io.agentscope.maven.plugin.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Top-level environment variables configuration shared by multiple deployment types.
 */
public class EnvironmentConfig {

    private Map<String, String> variables = new HashMap<>();

    public Map<String, String> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, String> variables) {
        this.variables = variables;
    }

    /**
     * Merge with another set of environment variables.
     * The other variables take precedence over this config's variables.
     */
    public Map<String, String> mergeWith(Map<String, String> other) {
        Map<String, String> merged = new HashMap<>(variables);
        if (other != null) {
            merged.putAll(other);
        }
        return merged;
    }
}

