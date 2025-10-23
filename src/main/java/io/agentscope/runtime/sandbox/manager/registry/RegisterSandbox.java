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

import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for registering Sandbox classes
 * Corresponds to Python's @SandboxRegistryService.register decorator
 * 
 * <p>Usage example:
 * <pre>{@code
 * @RegisterSandbox(
 *     imageName = "agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-base:latest",
 *     sandboxType = SandboxType.BASE,
 *     securityLevel = "medium",
 *     timeout = 30,
 *     description = "Base Sandbox"
 * )
 * public class BaseSandbox extends Sandbox {
 *     // ...
 * }
 * }</pre>
 * 
 * <p>For custom sandbox types, use customType:
 * <pre>{@code
 * @RegisterSandbox(
 *     imageName = "my-registry/my-custom-sandbox:latest",
 *     customType = "my_custom_type",
 *     securityLevel = "high",
 *     timeout = 60,
 *     description = "My Custom Sandbox"
 * )
 * public class MyCustomSandbox extends Sandbox {
 *     // ...
 * }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RegisterSandbox {
    /**
     * Docker image name (required)
     */
    String imageName();
    
    /**
     * Sandbox type (enum)
     * Defaults to BASE type
     * Ignored if customType is specified
     */
    SandboxType sandboxType() default SandboxType.BASE;
    
    /**
     * Custom sandbox type name
     * If specified, takes precedence over sandboxType
     * Empty string means not using custom type
     */
    String customType() default "";
    
    /**
     * Security level
     * Valid values: "low", "medium", "high"
     * Defaults to "medium"
     */
    String securityLevel() default "medium";
    
    /**
     * Timeout in seconds
     * Defaults to 300 seconds (5 minutes)
     */
    int timeout() default 300;
    
    /**
     * Sandbox description
     * Defaults to empty string
     */
    String description() default "";
    
    /**
     * Environment variable configuration
     * Format: "KEY1=VALUE1", "KEY2=VALUE2"
     * Defaults to empty array
     */
    String[] environment() default {};
    
    /**
     * Resource limits configuration
     * Format: "memory=1g", "cpu=2.0"
     * Supported keys: memory, cpu
     * Defaults to empty array
     */
    String[] resourceLimits() default {};
    
    /**
     * Runtime configuration
     * Format: "key1=value1", "key2=value2"
     * Defaults to empty array
     */
    String[] runtimeConfig() default {};
}

