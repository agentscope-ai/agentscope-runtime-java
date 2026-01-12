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
package io.agentscope.runtime.sandbox.manager.registry;

import io.agentscope.runtime.sandbox.manager.model.sandbox.SandboxConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sandbox registry for managing sandbox configurations
 */
public class SandboxRegistryService {
    private static final Logger logger = LoggerFactory.getLogger(SandboxRegistryService.class);
    private static final Map<String, SandboxConfig> typeConfigRegistry = new ConcurrentHashMap<>();

    static {
        try {
            Class.forName(SandboxRegistryInitializer.class.getName());
            logger.info("SandboxRegistryInitializer loaded and executed");
        } catch (ClassNotFoundException e) {
            logger.warn("SandboxRegistryInitializer not found, annotation-based registration disabled");
        }
    }

    /**
     * Register a sandbox configuration for a specific class and type
     *
     * @param targetClass The class to register
     * @param config      The sandbox configuration
     */
    public static void register(Class<?> targetClass, SandboxConfig config) {
        if (targetClass == null) {
            throw new IllegalArgumentException("Target class cannot be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("Sandbox configuration cannot be null");
        }

        String sandboxType = config.getSandboxType();

        typeConfigRegistry.put(sandboxType, config);

        logger.info("Registered sandbox: type={}, class={}, image={}", sandboxType, targetClass.getSimpleName(), config.getImageName());
    }


    /**
     * Get sandbox configuration by type
     *
     * @param sandboxType The sandbox type
     * @return Optional containing the configuration if found
     */
    public static Optional<SandboxConfig> getConfigByType(String sandboxType) {
        return Optional.ofNullable(typeConfigRegistry.get(sandboxType));
    }

    /**
     * Get Docker image name by sandbox type
     *
     * @param sandboxType The sandbox type
     * @return Optional containing the image name if found
     */
    public static Optional<String> getImageByType(String sandboxType) {
        return getConfigByType(sandboxType).map(SandboxConfig::getImageName);
    }
}

