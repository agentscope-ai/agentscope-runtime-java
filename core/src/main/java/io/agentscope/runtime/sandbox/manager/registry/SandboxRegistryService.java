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

import io.agentscope.runtime.sandbox.manager.model.container.DynamicSandboxType;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxConfig;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Sandbox registry for managing sandbox configurations
 */
public class SandboxRegistryService {
    private static final Logger logger = Logger.getLogger(SandboxRegistryService.class.getName());

    private static final Map<Class<?>, SandboxConfig> classRegistry = new ConcurrentHashMap<>();
    private static final Map<SandboxType, Class<?>> typeRegistry = new ConcurrentHashMap<>();
    private static final Map<SandboxType, SandboxConfig> typeConfigRegistry = new ConcurrentHashMap<>();

    private static final Map<String, SandboxConfig> customTypeRegistry = new ConcurrentHashMap<>();

    static {
        try {
            Class.forName(SandboxRegistryInitializer.class.getName());
            logger.info("SandboxRegistryInitializer loaded and executed");
        } catch (ClassNotFoundException e) {
            logger.warning("SandboxRegistryInitializer not found, annotation-based registration disabled");
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

        SandboxType sandboxType = config.getSandboxType();

        classRegistry.put(targetClass, config);
        typeRegistry.put(sandboxType, targetClass);
        typeConfigRegistry.put(sandboxType, config);

        logger.info("Registered sandbox: type=" + sandboxType +
                ", class=" + targetClass.getSimpleName() +
                ", image=" + config.getImageName());
    }

    /**
     * Register a sandbox configuration by type
     * This is a simplified registration method that doesn't require a class
     *
     * @param sandboxType The sandbox type
     * @param imageName   The Docker image name
     */
    public static void register(SandboxType sandboxType, String imageName) {
        SandboxConfig config = new SandboxConfig.Builder()
                .sandboxType(sandboxType)
                .imageName(imageName)
                .build();
        typeConfigRegistry.put(sandboxType, config);

        logger.info("Registered sandbox: type=" + sandboxType + ", image=" + imageName);
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
            SandboxType sandboxType,
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

        logger.info("Registered sandbox with full config: type=" + sandboxType +
                ", image=" + imageName +
                ", timeout=" + timeout + "s");
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
    public static Optional<SandboxConfig> getConfigByType(SandboxType sandboxType) {
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
    public static Optional<String> getImageByType(SandboxType sandboxType) {
        return getConfigByType(sandboxType).map(SandboxConfig::getImageName);
    }

    /**
     * Get class by sandbox type
     *
     * @param sandboxType The sandbox type
     * @return Optional containing the class if found
     */
    public static Optional<Class<?>> getClassesByType(SandboxType sandboxType) {
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
    public static Map<SandboxType, SandboxConfig> listAllSandboxesByType() {
        return new HashMap<>(typeConfigRegistry);
    }

    /**
     * Check if a sandbox type is registered
     *
     * @param sandboxType The sandbox type to check
     * @return true if registered, false otherwise
     */
    public static boolean isRegistered(SandboxType sandboxType) {
        return typeConfigRegistry.containsKey(sandboxType);
    }

    /**
     * Unregister a sandbox type
     *
     * @param sandboxType The sandbox type to unregister
     * @return true if unregistered successfully, false if not found
     */
    public static boolean unregister(SandboxType sandboxType) {
        SandboxConfig config = typeConfigRegistry.remove(sandboxType);
        Class<?> clazz = typeRegistry.remove(sandboxType);
        if (clazz != null) {
            classRegistry.remove(clazz);
        }

        if (config != null) {
            logger.info("Unregistered sandbox: type=" + sandboxType);
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
        customTypeRegistry.clear();
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

    /**
     * Register a custom sandbox type by string name
     * This supports dynamic type registration.
     *
     * @param typeName  The custom type name
     * @param imageName The Docker image name
     */
    public static void registerCustomType(String typeName, String imageName) {
        DynamicSandboxType.custom(typeName);

        SandboxConfig config = new SandboxConfig.Builder()
                .sandboxType(SandboxType.BASE) // Placeholder, actual type is identified by string name
                .imageName(imageName)
                .build();

        customTypeRegistry.put(typeName.toLowerCase(), config);
        logger.info("Registered custom sandbox type: name=" + typeName + ", image=" + imageName);
    }

    /**
     * Register a custom sandbox type with full configuration
     *
     * @param typeName       The custom type name
     * @param imageName      The Docker image name
     * @param resourceLimits Resource limits
     * @param securityLevel  Security level
     * @param timeout        Timeout in seconds
     * @param description    Description
     * @param environment    Environment variables
     * @param runtimeConfig  Runtime configuration
     */
    public static void registerCustomType(
            String typeName,
            String imageName,
            Map<String, Object> resourceLimits,
            String securityLevel,
            int timeout,
            String description,
            Map<String, String> environment,
            Map<String, Object> runtimeConfig) {

        // Create dynamic type to ensure it's registered in the type system
        DynamicSandboxType.custom(typeName);

        // Create full config for the custom type
        SandboxConfig config = new SandboxConfig.Builder()
                .sandboxType(SandboxType.BASE) // Placeholder
                .imageName(imageName)
                .resourceLimits(resourceLimits)
                .securityLevel(securityLevel)
                .timeout(timeout)
                .description(description)
                .environment(environment)
                .runtimeConfig(runtimeConfig)
                .build();

        customTypeRegistry.put(typeName.toLowerCase(), config);

        logger.info("Registered custom sandbox type with full config: name=" + typeName +
                ", image=" + imageName +
                ", timeout=" + timeout + "s");
    }

    /**
     * Get configuration for a custom sandbox type by name
     *
     * @param typeName The custom type name
     * @return Optional containing the configuration if found
     */
    public static Optional<SandboxConfig> getCustomTypeConfig(String typeName) {
        if (typeName == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(customTypeRegistry.get(typeName.toLowerCase()));
    }

    /**
     * Get image name for a custom sandbox type
     *
     * @param typeName The custom type name
     * @return Optional containing the image name if found
     */
    public static Optional<String> getCustomTypeImage(String typeName) {
        return getCustomTypeConfig(typeName).map(SandboxConfig::getImageName);
    }

    /**
     * Check if a custom type is registered
     *
     * @param typeName The custom type name
     * @return true if registered, false otherwise
     */
    public static boolean isCustomTypeRegistered(String typeName) {
        if (typeName == null) {
            return false;
        }
        return customTypeRegistry.containsKey(typeName.toLowerCase());
    }

    /**
     * List all registered custom sandbox types
     *
     * @return A copy of the custom type registry
     */
    public static Map<String, SandboxConfig> listAllCustomTypes() {
        return new HashMap<>(customTypeRegistry);
    }
}

