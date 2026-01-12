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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

public class SandboxRegistryInitializer {
    private static final Logger logger = LoggerFactory.getLogger(SandboxRegistryInitializer.class);
    private static boolean initialized = false;

    static {
        initialize();
    }

    /**
     * Initialize sandbox registry
     * Scan and register all classes with @RegisterSandbox annotation
     */
    public static synchronized void initialize() {
        if (initialized) {
            logger.info("SandboxRegistryService already initialized, skipping");
            return;
        }

        logger.info("Initializing SandboxRegistryService with annotated classes...");

        try {
            registerViaSpi();
            initialized = true;
            logger.info("SandboxRegistryService initialization completed successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize SandboxRegistryService: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize SandboxRegistryService", e);
        }
    }

    private static void registerViaSpi() {
        Set<Class<?>> registered = new LinkedHashSet<>();

        ServiceLoader<SandboxProvider> loader = ServiceLoader.load(SandboxProvider.class);

        for (SandboxProvider provider : loader) {
            try {
                Collection<Class<?>> classes = provider.getSandboxClasses();
                if (classes == null) {
                    logger.info("Provider {} returned null class list, skipping", provider.getClass().getName());
                    continue;
                }

                for (Class<?> clazz : classes) {
                    if (clazz == null) {
                        continue;
                    }
                    SandboxAnnotationProcessor.processClass(clazz);
                    registered.add(clazz);
                }

                logger.info("Sandbox SPI provider loaded: {}, provided={}", provider.getClass().getName(), classes.size());
            } catch (Exception e) {
                logger.warn("Failed to load sandboxes from provider {}: {}", provider.getClass().getName(), e.getMessage());
            }
        }

        if (registered.isEmpty()) {
            logger.warn("No sandbox classes discovered via SPI; registry is empty until providers are added.");
            return;
        }

        String summary = registered.stream()
                .map(Class::getSimpleName)
                .collect(Collectors.joining(", "));
        logger.info("Registered sandboxes via SPI: {}", summary);
    }
}
