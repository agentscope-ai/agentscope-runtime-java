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

import io.agentscope.runtime.sandbox.box.*;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxConfig;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Annotation processor for scanning and registering Sandbox classes with @RegisterSandbox annotation
 * Corresponds to Python's decorator registration mechanism
 */
public class SandboxAnnotationProcessor {
    private static final Logger logger = Logger.getLogger(SandboxAnnotationProcessor.class.getName());
    
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
            String customType = annotation.customType();
            SandboxType sandboxType = annotation.sandboxType();
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
            
            if (customType != null && !customType.isEmpty()) {
                SandboxRegistryService.registerCustomType(
                        customType,
                        imageName,
                        resourceLimits,
                        securityLevel,
                        timeout,
                        description,
                        environment,
                        runtimeConfig
                );
                logger.info("Registered custom sandbox via annotation: " + 
                           "type=" + customType + 
                           ", class=" + clazz.getSimpleName() + 
                           ", image=" + imageName);
            } else {
                SandboxRegistryService.register(clazz, config);
                logger.info("Registered sandbox via annotation: " + 
                           "type=" + sandboxType + 
                           ", class=" + clazz.getSimpleName() + 
                           ", image=" + imageName);
            }
            
        } catch (Exception e) {
            logger.severe("Failed to process @RegisterSandbox annotation on class " + 
                         clazz.getName() + ": " + e.getMessage());
            throw new RuntimeException("Failed to register sandbox: " + clazz.getName(), e);
        }
    }
    
    /**
     * Scan and process all classes in the specified package
     * 
     * @param packageName Package name
     */
    public static void scanAndRegisterPackage(String packageName) {
        logger.info("Scanning package for @RegisterSandbox annotations: " + packageName);
        logger.warning("Package scanning not fully implemented. " +
                      "Please call processClass() manually for each sandbox class " +
                      "or use a classpath scanner like Reflections library.");
    }
    
    /**
     * Manually register all known Sandbox classes
     * This method should be called at application startup
     */
    public static void registerAllKnownSandboxes() {
        logger.info("Registering all known sandbox classes with @RegisterSandbox annotation");
        
        try {
            Class<?> baseSandboxClass = Class.forName(BaseSandbox.class.getName());
            processClass(baseSandboxClass);
        } catch (ClassNotFoundException e) {
            logger.fine("BaseSandbox class not found, skipping: " + e.getMessage());
        }
        
        try {
            Class<?> filesystemSandboxClass = Class.forName(FilesystemSandbox.class.getName());
            processClass(filesystemSandboxClass);
        } catch (ClassNotFoundException e) {
            logger.fine("FilesystemSandbox class not found, skipping: " + e.getMessage());
        }
        
        try {
            Class<?> browserSandboxClass = Class.forName(BrowserSandbox.class.getName());
            processClass(browserSandboxClass);
        } catch (ClassNotFoundException e) {
            logger.fine("BrowserSandbox class not found, skipping: " + e.getMessage());
        }
        
        try {
            Class<?> dummySandboxClass = Class.forName(DummySandbox.class.getName());
            processClass(dummySandboxClass);
        } catch (ClassNotFoundException e) {
            logger.fine("DummySandbox class not found, skipping: " + e.getMessage());
        }
        
        try {
            Class<?> bfclSandboxClass = Class.forName(BFCLSandbox.class.getName());
            processClass(bfclSandboxClass);
        } catch (ClassNotFoundException e) {
            logger.fine("BFCLSandbox class not found, skipping: " + e.getMessage());
        }
        
        try {
            Class<?> appworldSandboxClass = Class.forName(APPWorldSandbox.class.getName());
            processClass(appworldSandboxClass);
        } catch (ClassNotFoundException e) {
            logger.fine("APPWorldSandbox class not found, skipping: " + e.getMessage());
        }
        
        try {
            Class<?> webshopSandboxClass = Class.forName(WebShopSandbox.class.getName());
            processClass(webshopSandboxClass);
        } catch (ClassNotFoundException e) {
            logger.fine("WebShopSandbox class not found, skipping: " + e.getMessage());
        }
        
        logger.info("Finished registering sandbox classes");
    }
    
    /**
     * Parse key-value array to Map<String, String>
     * Format: "key1=value1", "key2=value2"
     * Supports environment variable placeholders: ${VAR_NAME} or ${VAR_NAME:default_value}
     */
    private static Map<String, String> parseKeyValueArray(String[] array) {
        Map<String, String> result = new HashMap<>();
        if (array == null || array.length == 0) {
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
                // Resolve environment variables in value
                value = resolveEnvironmentVariables(value);
                result.put(key, value);
            } else {
                logger.warning("Invalid key-value pair format: " + item);
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
                // No more placeholders, append remaining string
                result.append(value.substring(startIndex));
                break;
            }
            
            int placeholderEnd = value.indexOf("}", placeholderStart);
            if (placeholderEnd == -1) {
                // Malformed placeholder, append as is
                result.append(value.substring(startIndex));
                break;
            }
            
            // Append text before placeholder
            result.append(value.substring(startIndex, placeholderStart));
            
            // Extract placeholder content
            String placeholder = value.substring(placeholderStart + 2, placeholderEnd);
            String envVarName;
            String defaultValue = "";
            
            // Check if default value is specified
            int colonIndex = placeholder.indexOf(':');
            if (colonIndex > 0) {
                envVarName = placeholder.substring(0, colonIndex).trim();
                defaultValue = placeholder.substring(colonIndex + 1).trim();
            } else {
                envVarName = placeholder.trim();
            }
            
            // Get environment variable value
            String envValue = System.getenv(envVarName);
            if (envValue != null) {
                result.append(envValue);
            } else {
                result.append(defaultValue);
                logger.fine("Environment variable '" + envVarName + "' not found, using default: '" + defaultValue + "'");
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
        if (array == null || array.length == 0) {
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
                logger.warning("Invalid key-value pair format: " + item);
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
        if (array == null || array.length == 0) {
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
                        logger.warning("Invalid CPU value: " + value);
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
