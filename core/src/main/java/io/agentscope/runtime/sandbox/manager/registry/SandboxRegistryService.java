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

import io.agentscope.runtime.sandbox.manager.model.container.SandboxConfig;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sandbox registry for managing sandbox configurations
 */
public class SandboxRegistryService {
    private static final Logger logger = LoggerFactory.getLogger(SandboxRegistryService.class);

    private static final Map<Class<?>, SandboxConfig> classRegistry = new ConcurrentHashMap<>();
    private static final Map<String, Class<?>> typeRegistry = new ConcurrentHashMap<>();
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

        classRegistry.put(targetClass, config);
        typeRegistry.put(sandboxType, targetClass);
        typeConfigRegistry.put(sandboxType, config);

        logger.info("Registered sandbox: type={}, class={}, image={}", sandboxType, targetClass.getSimpleName(), config.getImageName());
    }

    /**
     * Register a sandbox configuration by type
     * This is a simplified registration method that doesn't require a class
     *
     * @param sandboxType The sandbox type
     * @param imageName   The Docker image name
     */
    public static void register(String sandboxType, String imageName) {
        SandboxConfig config = new SandboxConfig.Builder()
                .sandboxType(sandboxType)
                .imageName(imageName)
                .build();
        typeConfigRegistry.put(sandboxType, config);

        logger.info("Registered sandbox: type={}, image={}", sandboxType, imageName);
    }

    /**
     * Register a sandbox with full configuration
     *
     * @param sandboxType    The sandbox type
     * @param imageName      The Docker image name
     * @param resourceLimits Resource limits
     * @param securityLevel  Security level
     * @param timeout        Timeout in seconds
     * @param description    Description
     * @param environment    Environment variables
     * @param runtimeConfig  Runtime configuration
     */
    public static void register(
            String sandboxType,
            String imageName,
            Map<String, Object> resourceLimits,
            String securityLevel,
            int timeout,
            String description,
            Map<String, String> environment,
            Map<String, Object> runtimeConfig) {

        SandboxConfig config = new SandboxConfig.Builder()
                .sandboxType(sandboxType)
                .imageName(imageName)
                .resourceLimits(resourceLimits)
                .securityLevel(securityLevel)
                .timeout(timeout)
                .description(description)
                .environment(environment)
                .runtimeConfig(runtimeConfig)
                .build();

        typeConfigRegistry.put(sandboxType, config);

        logger.info("Registered sandbox with full config: type={}, image={}, timeout={}s", sandboxType, imageName, timeout);
    }

    /**
     * Get sandbox configuration by class
     *
     * @param targetClass The target class
     * @return Optional containing the configuration if found
     */
    public static Optional<SandboxConfig> getConfig(Class<?> targetClass) {
        return Optional.ofNullable(classRegistry.get(targetClass));
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
     * Get Docker image name by class
     *
     * @param targetClass The target class
     * @return Optional containing the image name if found
     */
    public static Optional<String> getImage(Class<?> targetClass) {
        return getConfig(targetClass).map(SandboxConfig::getImageName);
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

    /**
     * Get class by sandbox type
     *
     * @param sandboxType The sandbox type
     * @return Optional containing the class if found
     */
    public static Optional<Class<?>> getClassesByType(String sandboxType) {
        return Optional.ofNullable(typeRegistry.get(sandboxType));
    }

    /**
     * List all registered sandboxes (class-based registrations)
     *
     * @return A copy of the registry
     */
    public static Map<Class<?>, SandboxConfig> listAllSandboxes() {
        return new HashMap<>(classRegistry);
    }

    /**
     * List all registered sandbox configurations by type
     *
     * @return A copy of the type-based registry
     */
    public static Map<String, SandboxConfig> listAllSandboxesByType() {
        return new HashMap<>(typeConfigRegistry);
    }

    /**
     * List all registered custom sandbox types
     *
     * @return A map of custom sandbox types to their configurations
     */
    public static Map<String, SandboxConfig> listAllCustomSandboxes() {
        if (typeConfigRegistry == null) {
            return new HashMap<>();
        }

        Map<String, SandboxConfig> customSandboxes = new HashMap<>(typeConfigRegistry);
        List<String> predefinedTypes = SandboxType.getAllPredefinedTypes();
        if (predefinedTypes != null) {
            customSandboxes.keySet().removeAll(predefinedTypes);
        }
        return customSandboxes;
    }


    /**
     * Check if a sandbox type is registered
     *
     * @param sandboxType The sandbox type to check
     * @return true if registered, false otherwise
     */
    public static boolean isRegistered(String sandboxType) {
        return typeConfigRegistry.containsKey(sandboxType);
    }

    /**
     * Unregister a sandbox type
     *
     * @param sandboxType The sandbox type to unregister
     * @return true if unregistered successfully, false if not found
     */
    public static boolean unregister(String sandboxType) {
        SandboxConfig config = typeConfigRegistry.remove(sandboxType);
        Class<?> clazz = typeRegistry.remove(sandboxType);
        if (clazz != null) {
            classRegistry.remove(clazz);
        }

        if (config != null) {
            logger.info("Unregistered sandbox: type={}", sandboxType);
            return true;
        }
        return false;
    }

    /**
     * Clear all registrations
     */
    public static void clear() {
        classRegistry.clear();
        typeRegistry.clear();
        typeConfigRegistry.clear();
        logger.info("Cleared all sandbox registrations");
    }

    /**
     * Get the count of registered sandboxes
     *
     * @return Number of registered sandbox types
     */
    public static int getRegisteredCount() {
        return typeConfigRegistry.size();
    }
}

