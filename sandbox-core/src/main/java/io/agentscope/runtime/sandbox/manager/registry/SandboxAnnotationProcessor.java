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

import java.util.HashMap;
import java.util.Map;

public class SandboxAnnotationProcessor {
    private static final Logger logger = LoggerFactory.getLogger(SandboxAnnotationProcessor.class);

    /**
     * Process a single class with @RegisterSandbox annotation
     *
     * @param clazz Class to process
     */
    public static void processClass(Class<?> clazz) {
        if (clazz == null) {
            return;
        }

        RegisterSandbox annotation = clazz.getAnnotation(RegisterSandbox.class);
        if (annotation == null) {
            return;
        }

        try {
            String imageName = annotation.imageName();
            String sandboxType = annotation.sandboxType();
            String securityLevel = annotation.securityLevel();
            int timeout = annotation.timeout();
            String description = annotation.description();

            Map<String, String> environment = parseKeyValueArray(annotation.environment());
            Map<String, Object> resourceLimits = parseResourceLimits(annotation.resourceLimits());
            Map<String, Object> runtimeConfig = parseKeyValueArrayAsObject(annotation.runtimeConfig());

            SandboxConfig config = new SandboxConfig.Builder()
                    .imageName(imageName)
                    .sandboxType(sandboxType)
                    .securityLevel(securityLevel)
                    .timeout(timeout)
                    .description(description)
                    .environment(environment)
                    .resourceLimits(resourceLimits)
                    .runtimeConfig(runtimeConfig)
                    .build();

            SandboxRegistryService.register(clazz, config);
            logger.info("Registered sandbox via annotation: type={}, class={}, image={}",
                    sandboxType, clazz.getSimpleName(), imageName);

        } catch (Exception e) {
            logger.error("Failed to process @RegisterSandbox annotation on class {}: {}",
                    clazz.getName(), e.getMessage());
            throw new RuntimeException("Failed to register sandbox: " + clazz.getName(), e);
        }
    }

    /**
     * Parse key-value array to Map<String, String>
     * Format: "key1=value1", "key2=value2"
     * Supports environment variable placeholders: ${VAR_NAME} or ${VAR_NAME:default_value}
     */
    private static Map<String, String> parseKeyValueArray(String[] array) {
        Map<String, String> result = new HashMap<>();
        if (array == null) {
            return result;
        }

        for (String item : array) {
            if (item == null || item.trim().isEmpty()) {
                continue;
            }

            int equalIndex = item.indexOf('=');
            if (equalIndex > 0) {
                String key = item.substring(0, equalIndex).trim();
                String value = item.substring(equalIndex + 1).trim();
                value = resolveEnvironmentVariables(value);
                result.put(key, value);
            } else {
                logger.warn("Invalid key-value pair format: {}", item);
            }
        }

        return result;
    }

    /**
     * Resolve environment variable placeholders in a string
     * Supports syntax: ${VAR_NAME} or ${VAR_NAME:default_value}
     *
     * @param value String that may contain placeholders
     * @return String with placeholders replaced by actual environment variable values
     */
    private static String resolveEnvironmentVariables(String value) {
        if (value == null || !value.contains("${")) {
            return value;
        }

        StringBuilder result = new StringBuilder();
        int startIndex = 0;

        while (startIndex < value.length()) {
            int placeholderStart = value.indexOf("${", startIndex);
            if (placeholderStart == -1) {
                result.append(value.substring(startIndex));
                break;
            }

            int placeholderEnd = value.indexOf("}", placeholderStart);
            if (placeholderEnd == -1) {
                result.append(value.substring(startIndex));
                break;
            }

            result.append(value.substring(startIndex, placeholderStart));

            String placeholder = value.substring(placeholderStart + 2, placeholderEnd);
            String envVarName;
            String defaultValue = "";

            int colonIndex = placeholder.indexOf(':');
            if (colonIndex > 0) {
                envVarName = placeholder.substring(0, colonIndex).trim();
                defaultValue = placeholder.substring(colonIndex + 1).trim();
            } else {
                envVarName = placeholder.trim();
            }

            String envValue = System.getenv(envVarName);
            if (envValue != null) {
                result.append(envValue);
            } else {
                result.append(defaultValue);
                logger.info("Environment variable '{}' not found, using default: '{}'", envVarName, defaultValue);
            }

            startIndex = placeholderEnd + 1;
        }

        return result.toString();
    }

    /**
     * Parse key-value array to Map<String, Object>
     * Format: "key1=value1", "key2=value2"
     */
    private static Map<String, Object> parseKeyValueArrayAsObject(String[] array) {
        Map<String, Object> result = new HashMap<>();
        if (array == null) {
            return result;
        }

        for (String item : array) {
            if (item == null || item.trim().isEmpty()) {
                continue;
            }

            int equalIndex = item.indexOf('=');
            if (equalIndex > 0) {
                String key = item.substring(0, equalIndex).trim();
                String value = item.substring(equalIndex + 1).trim();
                Object parsedValue = parseValue(value);
                result.put(key, parsedValue);
            } else {
                logger.warn("Invalid key-value pair format: {}", item);
            }
        }

        return result;
    }

    /**
     * Parse resource limits configuration
     * Supported format: "memory=1g", "cpu=2.0"
     */
    private static Map<String, Object> parseResourceLimits(String[] array) {
        Map<String, Object> result = new HashMap<>();
        if (array == null) {
            return result;
        }

        for (String item : array) {
            if (item == null || item.trim().isEmpty()) {
                continue;
            }

            int equalIndex = item.indexOf('=');
            if (equalIndex > 0) {
                String key = item.substring(0, equalIndex).trim();
                String value = item.substring(equalIndex + 1).trim();

                if ("memory".equalsIgnoreCase(key)) {
                    result.put("memory", value);
                } else if ("cpu".equalsIgnoreCase(key)) {
                    try {
                        double cpuValue = Double.parseDouble(value);
                        result.put("cpu", cpuValue);
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid CPU value: {}", value);
                        result.put("cpu", value);
                    }
                } else {
                    result.put(key, value);
                }
            }
        }

        return result;
    }

    /**
     * Try to parse string value to appropriate type (String, Integer, Double, Boolean)
     */
    private static Object parseValue(String value) {
        if (value == null) {
            return null;
        }

        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            // Not an integer, continue trying
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            // Not a double, return string
        }

        return value;
    }
}
