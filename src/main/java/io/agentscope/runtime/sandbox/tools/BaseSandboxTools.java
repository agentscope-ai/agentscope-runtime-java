package io.agentscope.runtime.sandbox.tools;

import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.container.ContainerModel;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;
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
}
