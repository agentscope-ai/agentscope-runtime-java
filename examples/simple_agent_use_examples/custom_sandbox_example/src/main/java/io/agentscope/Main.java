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

import io.agentscope.runtime.sandbox.manager.ManagerConfig;
import io.agentscope.runtime.sandbox.manager.SandboxService;
import io.agentscope.runtime.sandbox.manager.client.container.BaseClientStarter;

import java.util.Map;

/**
 * Custom Sandbox Example — Full end-to-end demonstration with NO Docker dependency.
 *
 * <p>This example demonstrates two layers of sandbox customization using only fake/stub
 * implementations.</p>
 *
 * <h2>Infrastructure Layer (WHERE the sandbox runs)</h2>
 * <p>{@link CustomClientStarter} + {@link CustomClient} — A fake runtime backend
 * that demonstrates the container lifecycle: connect → imageExists → createContainer
 * → startContainer → ... → stopContainer → removeContainer</p>
 *
 * <h2>Application Layer (WHAT the sandbox does)</h2>
 * <p>{@link CustomSandbox} — A custom sandbox with callTool hooks (before/after/error)
 * and fake tool execution, demonstrating runPython(), runShell(), and convenience methods.</p>
 *
 * <h2>What you'll see in the output</h2>
 * <pre>
 *   Step 1-3: Infrastructure setup (connect to platform)
 *   Step 4:   Sandbox creation triggers container lifecycle
 *   Step 5-7: Tool calls with before/after hooks
 *   Step 8:   Session reuse (same userId + sessionId → same container)
 *   Step 9:   Environment variable injection
 *   Step 10:  Sandbox close triggers cleanup lifecycle
 * </pre>
 */
public class Main {

    public static void main(String[] args) {

        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║  Custom Sandbox Example — Full Lifecycle Demo               ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();

        // =====================================================================
        // Step 1: Create a custom BaseClientStarter
        //
        // This is YOUR runtime backend configuration, analogous to:
        //   DockerClientStarter.builder().host("localhost").port(2375).build()
        //   KubernetesClientStarter.builder().kubeConfigPath("~/.kube/config").build()
        //   AgentRunClientStarter.builder().agentRunAccessKeyId("xxx").build()
        // =====================================================================
        System.out.println("--- Step 1: Create CustomClientStarter ---");
        BaseClientStarter customStarter = CustomClientStarter.builder()
                .host("my-platform.example.com")
                .port(443)
                .label("demo")
                .build();
        System.out.println("  Created with host=my-platform.example.com, port=443\n");

        // =====================================================================
        // Step 2: Plug into ManagerConfig
        //
        // ManagerConfig.builder().clientStarter(...) is the single integration point.
        // Swap in any BaseClientStarter (Docker, K8s, your own) without changing
        // any sandbox code — infrastructure and application layers are decoupled.
        // =====================================================================
        System.out.println("--- Step 2: Plug into ManagerConfig ---");
        ManagerConfig config = ManagerConfig.builder()
                .clientStarter(customStarter)
                .build();
        System.out.println("  ManagerConfig created with custom backend\n");

        // =====================================================================
        // Step 3: Start SandboxService
        //
        // This triggers: CustomClientStarter.startClient() → CustomClient.connect()
        // Watch the [CustomClient] logs below.
        // =====================================================================
        System.out.println("--- Step 3: Start SandboxService (triggers connect()) ---");
        SandboxService sandboxService = new SandboxService(config);
        sandboxService.start();
        System.out.println();

        // =====================================================================
        // Step 4: Create a CustomSandbox
        //
        // This triggers the container lifecycle:
        //   imageExists() → createContainer() → startContainer()
        // Watch the [CustomClient] logs to see each method being called.
        // =====================================================================
        System.out.println("--- Step 4: Create CustomSandbox (triggers container lifecycle) ---");
        CustomSandbox sandbox = new CustomSandbox(sandboxService, "user1", "session1");
        System.out.println("  Sandbox created for userId=user1, sessionId=session1\n");

        // =====================================================================
        // Step 5: Run Python — callTool hooks fire automatically
        //
        // The call flow is:
        //   sandbox.runPython(code)
        //     → callTool("run_ipython_cell", {code: "..."})
        //       → [HOOK:before] logged
        //       → fakeToolExecution() — in real app, this would be super.callTool()
        //       → [HOOK:after] logged with elapsed time
        // =====================================================================
        System.out.println("--- Step 5: Run Python (hooks fire automatically) ---");
        String result1 = sandbox.runPython("x = 42\nprint(f'x = {x}')");
        System.out.println("  Result: " + result1 + "\n");

        // =====================================================================
        // Step 6: Run Shell — same hook lifecycle
        // =====================================================================
        System.out.println("--- Step 6: Run Shell Command ---");
        String result2 = sandbox.runShell("echo 'Hello from custom sandbox!' && uname -a");
        System.out.println("  Result: " + result2 + "\n");

        // =====================================================================
        // Step 7: Direct callTool — low-level API
        //
        // You can also call callTool() directly for tools that don't have
        // convenience methods yet.
        // =====================================================================
        System.out.println("--- Step 7: Direct callTool (low-level) ---");
        String result3 = sandbox.callTool("run_ipython_cell",
                Map.of("code", "import sys; print(f'Python {sys.version}')"));
        System.out.println("  Result: " + result3 + "\n");

        // =====================================================================
        // Step 8: Session Reuse
        //
        // Creating a new sandbox with the SAME userId + sessionId reuses the
        // existing container. In a real sandbox, Python variables would persist.
        // =====================================================================
        System.out.println("--- Step 8: Session Reuse (same userId + sessionId) ---");
        CustomSandbox sandbox2 = new CustomSandbox(sandboxService, "user1", "session1");
        String result4 = sandbox2.runPython("print(f'x is still {x}')");
        System.out.println("  Result: " + result4);
        System.out.println("  (In a real sandbox, variables would persist across calls)\n");

        // =====================================================================
        // Step 9: Environment Variable Injection
        //
        // Pass custom environment variables when creating the sandbox.
        // These are injected into the container/instance at creation time.
        // =====================================================================
        System.out.println("--- Step 9: Environment Variable Injection ---");
        CustomSandbox envSandbox = new CustomSandbox(
                sandboxService, "user2", "session2",
                Map.of("MY_APP_NAME", "CustomSandboxDemo", "MY_APP_VERSION", "1.0.0"));
        String result5 = envSandbox.runShell("echo \"App: $MY_APP_NAME v$MY_APP_VERSION\"");
        System.out.println("  Result: " + result5 + "\n");

        // =====================================================================
        // Step 10: Close sandbox — triggers cleanup lifecycle
        //
        // sandbox.close() calls: stopContainer() → removeContainer()
        // In a real application, use try-with-resources for auto cleanup:
        //   try (CustomSandbox s = new CustomSandbox(service, "u", "s")) { ... }
        // =====================================================================
        System.out.println("--- Step 10: Close sandbox (triggers cleanup lifecycle) ---");
        try {
            sandbox.close();
        } catch (Exception e) {
            // Expected: fake backend doesn't track real containers
            System.out.println("  (Cleanup attempted — in a real backend, stopContainer + removeContainer would run)");
        }

        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║  ✅ All steps completed successfully!                         ║");
        System.out.println("║                                                              ║");
        System.out.println("║  To make this real, replace the fake/stub implementations:   ║");
        System.out.println("║  • CustomClient: connect to your platform's API              ║");
        System.out.println("║  • CustomSandbox.callTool: call super.callTool() instead     ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
    }
}
