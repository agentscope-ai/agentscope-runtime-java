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
package io.agentscope.runtime.engine.shared;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Abstract base class for service managers
 * Provides common functionality for service registration and lifecycle management
 */
public abstract class ServiceManager implements AutoCloseable {

    Logger logger = Logger.getLogger(ServiceManager.class.getName());

    private final List<ServiceRegistration> services = new ArrayList<>();
    private final Map<String, Service> serviceInstances = new ConcurrentHashMap<>();

    public ServiceManager() {
        // Initialize default services
        registerDefaultServices();
    }

    /**
     * Register default services for this manager, override in subclasses
     */
    protected abstract void registerDefaultServices();

    /**
     * Register a service
     *
     * @param serviceClass The service class to register
     * @param name         Optional service name, defaults to class name with "Service" suffix removed and converted to lowercase
     * @param args         Positional arguments for service initialization
     * @return this For method chaining
     */
    public ServiceManager register(Class<? extends Service> serviceClass, String name, Object... args) {
        if (name == null) {
            name = serviceClass.getSimpleName().replace("Service", "").toLowerCase();
        }

        // Check if service name already exists
        if (serviceInstances.containsKey(name)) {
            throw new IllegalArgumentException("Service with name '" + name + "' is already registered");
        }

        services.add(new ServiceRegistration(serviceClass, name, args));
        logger.info("Registered service: " + name + "(" + serviceClass.getSimpleName() + ")");
        return this;
    }

    /**
     * Register an instantiated service
     *
     * @param name    Service name
     * @param service Service instance
     * @return this For method chaining
     */
    public ServiceManager registerService(String name, Service service) {
        if (serviceInstances.containsKey(name)) {
            throw new IllegalArgumentException("Service with name '" + name + "' is already registered");
        }

        serviceInstances.put(name, service);
        return this;
    }

    /**
     * Start all registered services
     *
     * @return CompletableFuture<Void> Asynchronous startup result
     */
    public CompletableFuture<Void> start() {
        return CompletableFuture.runAsync(() -> {
            try {
                // Start services registered via register()
                for (ServiceRegistration registration : services) {
                    try {
                        Service instance = registration.serviceClass.getDeclaredConstructor()
                                .newInstance();
                        instance.start().get();
                        serviceInstances.put(registration.name, instance);
                        logger.info("Successfully started service: " + registration.name);
                    } catch (Exception e) {
                        logger.severe("Failed to start service: " + registration.name + " - " + e.getMessage());
                        throw new RuntimeException("Failed to start service: " + registration.name, e);
                    }
                }

                // Start services registered via registerService()
                for (Map.Entry<String, Service> entry : serviceInstances.entrySet()) {
                    String name = entry.getKey();
                    Service service = entry.getValue();
                    if (!services.stream().anyMatch(reg -> reg.name.equals(name))) {
                        try {
                            service.start().get();
                        } catch (Exception e) {
                            logger.severe("Failed to start pre-instantiated service: " + name + " - " + e.getMessage());
                            throw new RuntimeException("Failed to start pre-instantiated service: " + name, e);
                        }
                    }
                }

            } catch (Exception e) {
                logger.severe("Failed to start services" + e.getMessage());
                // Ensure proper cleanup on initialization failure
                stop().join();
                throw new RuntimeException("Failed to start services", e);
            }
        });
    }

    /**
     * Stop all services
     *
     * @return CompletableFuture<Void> Asynchronous stop result
     */
    public CompletableFuture<Void> stop() {
        return CompletableFuture.runAsync(() -> {
            logger.info("Stopping all services");
            for (Service service : serviceInstances.values()) {
                try {
                    service.stop().get();
                } catch (Exception e) {
                    logger.severe("Error stopping service" + e.getMessage());
                }
            }
            serviceInstances.clear();
            logger.info("All services stopped");
        });
    }

    @Override
    public void close() throws Exception {
        stop().get();
    }

    /**
     * Enable property access for services, e.g., manager.env, manager.session
     */
    public Service getService(String name) {
        Service service = serviceInstances.get(name);
        if (service == null) {
            throw new IllegalArgumentException("Service '" + name + "' not found");
        }
        return service;
    }

    /**
     * Explicitly retrieve service instance, supports default value
     */
    public Service getService(String name, Service defaultService) {
        return serviceInstances.getOrDefault(name, defaultService);
    }

    /**
     * Check if service exists
     */
    public boolean hasService(String name) {
        return serviceInstances.containsKey(name);
    }

    /**
     * List all registered service names
     */
    public List<String> listServices() {
        return new ArrayList<>(serviceInstances.keySet());
    }

    /**
     * Retrieve all service instances
     */
    public Map<String, Service> getAllServices() {
        return new HashMap<>(serviceInstances);
    }

    /**
     * Check health status of all services
     *
     * @return CompletableFuture<Map<String, Boolean>> Asynchronous health check result
     */
    public CompletableFuture<Map<String, Boolean>> healthCheck() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Boolean> healthStatus = new HashMap<>();
            for (Map.Entry<String, Service> entry : serviceInstances.entrySet()) {
                String name = entry.getKey();
                Service service = entry.getValue();
                try {
                    healthStatus.put(name, service.health().get());
                } catch (Exception e) {
                    logger.severe("Health check failed for service " + name + " - " + e.getMessage());
                    healthStatus.put(name, false);
                }
            }
            return healthStatus;
        });
    }

    /**
     * Internal class for service registration information
     */
    private static class ServiceRegistration {
        final Class<? extends Service> serviceClass;
        final String name;
        final Object[] args;

        ServiceRegistration(Class<? extends Service> serviceClass, String name, Object[] args) {
            this.serviceClass = serviceClass;
            this.name = name;
            this.args = args;
        }
    }
}
