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

package io.agentscope.runtime.autoconfigure.controller;

import io.agentscope.runtime.engine.Runner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

    @Autowired
    private ApplicationContext applicationContext;

    @GetMapping("/health")
    public String healthCheck() {
        return "AgentScope service is running ✓";
    }

    @GetMapping("readiness")
    public String readinessCheck() {
        String[] beanNames = applicationContext.getBeanNamesForType(Runner.class);
        return beanNames.length > 0 ? "Ready" : "Not ready";
    }

    @GetMapping("/")
    public String root(){
        return "AgentScope Runtime";
    }

    @GetMapping("/liveness")
    public String livenessCheck() {
        return "Alive";
    }
}
