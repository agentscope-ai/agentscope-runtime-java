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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.runtime.engine.Runner;
import io.agentscope.runtime.sandbox.manager.SandboxService;
import io.agentscope.runtime.sandbox.manager.remote.RemoteWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/sandbox")
public class SandboxServiceController {

    private static final Logger logger = LoggerFactory.getLogger(SandboxServiceController.class);

    private final Map<String, MethodInfo> endpointRegistry = new HashMap<>();

    @Autowired
    private Runner runner;

    @PostConstruct
    public void registerEndpoints() {
        Class<?> clazz = SandboxService.class;

        logger.info("Scanning class: {}", clazz.getName());
        Method[] methods = clazz.getDeclaredMethods();
        logger.info("Found {} methods in SandboxService", methods.length);

        for (Method method : methods) {
            RemoteWrapper annotation = method.getAnnotation(RemoteWrapper.class);
            if (annotation != null) {
                String path = annotation.path();
                if (path == null || path.isEmpty()) {
                    path = "/" + method.getName();
                } else if (!path.startsWith("/")) {
                    path = "/" + path;
                }

                MethodInfo methodInfo = new MethodInfo(method, annotation);
                endpointRegistry.put(path, methodInfo);

                logger.info("Registered endpoint: {} /sandbox{} -> {}", annotation.method(), path, method.getName());
            }
        }

        logger.info("Total endpoints registered: {}", endpointRegistry.size());
    }

    /**
     * Handle POST requests to remote wrapper endpoints
     */
    @PostMapping("/**")
    public ResponseEntity<Map<String, Object>> handlePost(
            @RequestBody(required = false) Map<String, Object> requestData,
            jakarta.servlet.http.HttpServletRequest request) {

        String path = extractPath(request);
        return handleRequest(path, requestData, RequestMethod.POST);
    }

    /**
     * Handle GET requests to remote wrapper endpoints
     */
    @GetMapping("/**")
    public ResponseEntity<Map<String, Object>> handleGet(
            @RequestParam(required = false) Map<String, Object> requestData,
            jakarta.servlet.http.HttpServletRequest request) {

        String path = extractPath(request);
        return handleRequest(path, requestData, RequestMethod.GET);
    }

    /**
     * Handle DELETE requests to remote wrapper endpoints
     */
    @DeleteMapping("/**")
    public ResponseEntity<Map<String, Object>> handleDelete(
            @RequestBody(required = false) Map<String, Object> requestData,
            jakarta.servlet.http.HttpServletRequest request) {

        String path = extractPath(request);
        return handleRequest(path, requestData, RequestMethod.DELETE);
    }

    private String extractPath(jakarta.servlet.http.HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = requestUri.substring(contextPath.length());

        // Remove /sandbox prefix to match registered endpoints
        if (path.startsWith("/sandbox")) {
            path = path.substring("/sandbox".length());
            if (path.isEmpty()) {
                path = "/";
            }
        }

        return path;
    }

    private ResponseEntity<Map<String, Object>> handleRequest(
            String path,
            Map<String, Object> requestData,
            RequestMethod httpMethod) {

        // Todo: fix this
        SandboxService sandboxService = runner.getSandboxService();

        logger.info("Handling {} request for path: {}", httpMethod, path);
        System.out.println("Handling " + httpMethod + " request for path: " + path);

        MethodInfo methodInfo = endpointRegistry.get(path);

        if (methodInfo == null) {
            logger.warn("No endpoint found for path: {}", path);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Endpoint not found: " + path);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }

        if (!methodInfo.annotation.method().name().equals(httpMethod.name())) {
            logger.warn("HTTP method mismatch for path: {} (expected: {}, got: {})", path, methodInfo.annotation.method(), httpMethod);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Method not allowed");
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse);
        }

        try {
            Object[] args = prepareArguments(methodInfo.method, requestData);

            ObjectMapper mapper = new ObjectMapper();
            String sandboxJson = mapper.writeValueAsString(args[0]);
            Map<String, Object> request = Map.of("sandbox", sandboxJson);

            Object result = methodInfo.method.invoke(sandboxService, args);

            Map<String, Object> response = new HashMap<>();
            String successKey = methodInfo.annotation.successKey();
            if (successKey != null && !successKey.isEmpty()) {
                response.put(successKey, result);
            } else {
                response.put("result", result);
            }

            logger.info("Request processed successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error processing request: {}", e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error: " + e.getMessage());
            if (e.getCause() != null) {
                errorResponse.put("detail", e.getCause().getMessage());
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    private Object[] prepareArguments(Method method, Map<String, Object> requestData) {
        if (requestData == null) {
            requestData = new HashMap<>();
        }

        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            String paramName = param.getName();
            Object value = requestData.get(paramName);

            if (value != null) {
                args[i] = convertValue(value, param.getType());
            } else {
                args[i] = getDefaultValue(param.getType());
            }
        }

        return args;
    }

    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }

        if (targetType == String.class) {
            return value.toString();
        }

        if (targetType == int.class || targetType == Integer.class) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(value.toString());
        }

        if (targetType == long.class || targetType == Long.class) {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            return Long.parseLong(value.toString());
        }

        if (targetType == boolean.class || targetType == Boolean.class) {
            if (value instanceof Boolean) {
                return value;
            }
            return Boolean.parseBoolean(value.toString());
        }

        if (targetType.isEnum()) {
            String enumValue = value.toString();
            try {
                @SuppressWarnings({"unchecked", "rawtypes"})
                Object enumConstant = Enum.valueOf((Class<Enum>) targetType, enumValue);
                return enumConstant;
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid enum value '{}' for type {}", enumValue, targetType.getName());
                return null;
            }
        }

        if (Map.class.isAssignableFrom(targetType) && value instanceof Map) {
            return value;
        }

        if (value instanceof String strValue) {
            String trimmed = strValue.trim();
            if (trimmed.startsWith("{") || trimmed.startsWith("[") && !targetType.isPrimitive()) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    return mapper.readValue(strValue, targetType);
                } catch (Exception e) {
                    logger.warn("Failed to parse JSON string to {}: {}", targetType.getName(), e.getMessage());
                }
            }
        }

        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(value, targetType);
    }

    private Object getDefaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0;
        if (type == float.class) return 0.0f;
        return null;
    }

    private static class MethodInfo {
        final Method method;
        final RemoteWrapper annotation;

        MethodInfo(Method method, RemoteWrapper annotation) {
            this.method = method;
            this.annotation = annotation;
            this.method.setAccessible(true);
        }
    }
}

