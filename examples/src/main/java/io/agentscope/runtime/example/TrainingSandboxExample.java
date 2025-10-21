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
package io.agentscope.runtime.example;

import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;
import io.agentscope.runtime.sandbox.tools.TrainingSandboxTools;

/**
 * Example usage of Training Sandbox functionality
 */
public class TrainingSandboxExample {

    public static void main(String[] args) {
        TrainingSandboxTools trainingSandboxTools = new TrainingSandboxTools();
        String result = trainingSandboxTools.getEnvProfiles(SandboxType.TRAINING, "appworld", null, null, "", "");
        System.out.println(result);
        result = trainingSandboxTools.createInstance("appworld", "82e2fac_1", null, null, "", "");
        System.out.println(result);
    }
}
