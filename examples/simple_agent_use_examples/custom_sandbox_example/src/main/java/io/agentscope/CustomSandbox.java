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
package io.agentscope;

import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.manager.SandboxService;
import io.agentscope.runtime.sandbox.manager.fs.FileSystemConfig;
import io.agentscope.runtime.sandbox.manager.registry.RegisterSandbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * CustomSandbox - A fake/stub implementation demonstrating how to create a custom sandbox type.
 *
 * <h2>What is a Sandbox?</h2>
 * <p>A {@link Sandbox} is the application-layer abstraction that defines <b>what happens</b>
 * inside a runtime instance (container, VM, pod, etc.). It's independent of the infrastructure
 * layer ({@link CustomClient} / {@link CustomClientStarter}) which defines <b>where</b> it runs.</p>
 *
 * <h2>Key extension points demonstrated</h2>
 * <ul>
 *   <li>Extend {@link Sandbox} and annotate with {@link RegisterSandbox}</li>
 *   <li>Override {@link #callTool} to add before/after/error hooks for observability</li>
 *   <li>Provide domain-specific convenience methods (e.g. {@link #runPython}, {@link #runShell})</li>
 *   <li>Inject custom environment variables into the sandbox instance</li>
 * </ul>
 *
 * <h2>Fake/stub approach</h2>
 * <p>This example uses a fake {@link #callTool} implementation instead of calling
 * {@code super.callTool()}, so it runs <b>without any real runtime backend</b>.
 * In a real application, you would call {@code super.callTool(name, arguments)} which
 * sends an HTTP request to the sandbox instance's API endpoint:</p>
 * <pre>
 *   POST http://{ip}:{port}/fastapi/tools/{toolName}
 *   Body: {"arguments": {...}}
 * </pre>
 *
 * @see Sandbox
 * @see CustomClient
 * @see CustomClientStarter
 */
@RegisterSandbox(
        imageName = "agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-base:latest",
        sandboxType = "custom",
        securityLevel = "medium",
        timeout = 60,
        description = "Custom sandbox with tool execution hooks"
)
public class CustomSandbox extends Sandbox {

    private static final Logger logger = LoggerFactory.getLogger(CustomSandbox.class);

    /**
     * Simple constructor - uses default settings.
     */
    public CustomSandbox(SandboxService managerApi, String userId, String sessionId) {
        super(managerApi, userId, sessionId, "custom");
    }

    /**
     * Constructor with custom environment variables injected into the container.
     */
    public CustomSandbox(
            SandboxService managerApi,
            String userId,
            String sessionId,
            Map<String, String> environment) {
        super(managerApi, userId, sessionId, "custom",
                io.agentscope.runtime.sandbox.manager.fs.local.LocalFileSystemConfig.builder().build(),
                environment);
    }

    // ==================== Hook: Override callTool ====================

    /**
     * Override callTool to add before/after hooks around every tool invocation.
     *
     * <p>This is the core extension point. By overriding this method, you can:</p>
     * <ul>
     *   <li>Log or audit every tool call (who called what, when, with what args)</li>
     *   <li>Validate or transform arguments before execution</li>
     *   <li>Measure execution time for performance monitoring</li>
     *   <li>Intercept results for post-processing or error handling</li>
     *   <li>Implement rate limiting, access control, or retry logic</li>
     * </ul>
     *
     * <p><b>In a real application:</b> replace {@link #fakeToolExecution} with
     * {@code super.callTool(name, arguments)}, which sends an HTTP POST to the
     * sandbox instance created by your {@link CustomClient}.</p>
     */
    @Override
    public String callTool(String name, Map<String, Object> arguments) {
        // ---- Before Hook ----
        long startTime = System.currentTimeMillis();
        beforeToolCall(name, arguments);

        String result;
        try {
            // ---- Actual Execution ----
            // In a REAL application, you would call:
            //   result = super.callTool(name, arguments);
            // which sends an HTTP request to the sandbox instance:
            //   POST http://{ip}:{port}/fastapi/tools/{name}
            //   Body: {"arguments": {"code": "..."}}
            //
            // For this teaching example, we use a fake implementation
            // so the entire example runs without any real runtime backend.
            result = fakeToolExecution(name, arguments);
        } catch (Exception e) {
            // ---- Error Hook ----
            onToolError(name, arguments, e);
            throw e;
        }

        // ---- After Hook ----
        long elapsed = System.currentTimeMillis() - startTime;
        afterToolCall(name, arguments, result, elapsed);

        return result;
    }

    /**
     * Fake tool execution for demonstration purposes.
     *
     * <p>Simulates the response that a real sandbox instance would return.
     * This lets you see the full hook lifecycle without needing Docker or any runtime.</p>
     *
     * <p><b>What a real sandbox does:</b></p>
     * <ul>
     *   <li>{@code run_ipython_cell} — Executes Python code in an IPython kernel inside the sandbox.
     *       Variables persist across calls within the same session.</li>
     *   <li>{@code run_shell_command} — Executes a shell command (bash) inside the sandbox.</li>
     * </ul>
     *
     * @param name the tool name
     * @param arguments the tool arguments
     * @return simulated tool output
     */
    private String fakeToolExecution(String name, Map<String, Object> arguments) {
        switch (name) {
            case "run_ipython_cell":
                String code = String.valueOf(arguments.getOrDefault("code", ""));
                return "[fake output] Python executed: " + code;
            case "run_shell_command":
                String command = String.valueOf(arguments.getOrDefault("command", ""));
                return "[fake output] Shell executed: " + command;
            default:
                return "[fake output] Unknown tool '" + name + "' called with: " + arguments;
        }
    }

    /**
     * Hook: called BEFORE every tool execution.
     * Override this in further subclasses for custom pre-processing.
     */
    protected void beforeToolCall(String toolName, Map<String, Object> arguments) {
        logger.info("[HOOK:before] Tool '{}' called with args: {}", toolName, arguments);
    }

    /**
     * Hook: called AFTER every successful tool execution.
     * Override this in further subclasses for custom post-processing.
     */
    protected void afterToolCall(String toolName, Map<String, Object> arguments,
                                 String result, long elapsedMs) {
        // Truncate long results for cleaner logging
        String preview = result != null && result.length() > 200
                ? result.substring(0, 200) + "..."
                : result;
        logger.info("[HOOK:after] Tool '{}' completed in {}ms, result: {}", toolName, elapsedMs, preview);
    }

    /**
     * Hook: called when a tool execution throws an exception.
     * Override this in further subclasses for custom error handling.
     */
    protected void onToolError(String toolName, Map<String, Object> arguments, Exception e) {
        logger.error("[HOOK:error] Tool '{}' failed: {}", toolName, e.getMessage());
    }

    // ==================== Convenience Methods ====================

    /**
     * Execute Python code in the sandbox via IPython.
     *
     * <p>In a real sandbox, the container runs an IPython kernel. Variables persist
     * across multiple calls within the same session, enabling stateful computation.</p>
     *
     * <p>This convenience method wraps {@link #callTool} with the correct tool name
     * and argument format — a pattern you can follow for your own domain-specific tools.</p>
     *
     * @param code Python code to execute
     * @return execution output or error message
     */
    public String runPython(String code) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("code", code);
        return callTool("run_ipython_cell", arguments);
    }

    /**
     * Execute a shell command in the sandbox.
     *
     * @param command shell command to execute
     * @return command output
     */
    public String runShell(String command) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("command", command);
        return callTool("run_shell_command", arguments);
    }
}
