package io.agentscope.runtime.sandbox.tools;

import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.ContainerModel;
import io.agentscope.runtime.sandbox.manager.model.SandboxType;
import io.agentscope.runtime.sandbox.manager.util.HttpClient;

import java.util.HashMap;
import java.util.Map;

public class BaseSandboxTools {
    final SandboxManager sandboxManager;
    final HttpClient httpClient;

    public BaseSandboxTools(SandboxManager sandboxManager, HttpClient httpClient) {
        this.sandboxManager = sandboxManager;
        this.httpClient = httpClient;
    }

    /**
     * Check if sandbox is running
     *
     * @param sandboxType sandbox model
     * @return whether it is running
     */
    public boolean isSandboxRunning(SandboxType sandboxType, String userID, String sessionID) {
        try {
            String status = sandboxManager.getSandboxStatus(sandboxType, userID, sessionID);
            return "running".equals(status);
        } catch (Exception e) {
            System.err.println("Failed to check sandbox status: " + e.getMessage());
            return false;
        }
    }

    /**
     * Wait for API service inside container to be healthy (/healthz returns 200)
     */
    public void waitUntilHealthy(ContainerModel sandbox, String baseUrl) {
        String authToken = sandbox.getAuthToken();
        String healthUrl = baseUrl + "/healthz";

        Map<String, String> headers = new HashMap<>();
        if (authToken != null) {
            headers.put("Authorization", "Bearer " + authToken);
        }
        headers.put("Host", "localhost:" + sandbox.getPorts()[0]);

        long start = System.currentTimeMillis();
        long timeoutMs = 600_000;
        long sleepMs = 700;
        while (System.currentTimeMillis() - start < timeoutMs) {
            try {
                String resp = httpClient.get(healthUrl, headers);
                if (resp != null && !resp.isEmpty()) {
                    return;
                }
            } catch (Exception ignored) {
                // ignore and retry
            }
            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
