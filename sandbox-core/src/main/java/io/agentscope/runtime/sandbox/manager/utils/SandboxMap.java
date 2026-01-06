package io.agentscope.runtime.sandbox.manager.utils;

import io.agentscope.runtime.sandbox.manager.model.container.ContainerModel;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxKey;

import java.util.Map;

public interface SandboxMap {
    public void addSandbox(SandboxKey sandboxKey, ContainerModel containerModel);

    public ContainerModel getSandbox(SandboxKey sandboxKey);

    public boolean removeSandbox(SandboxKey sandboxKey);

    public ContainerModel getSandbox(String containerId);

    public void removeSandbox(String containerId);

    public Map<String, ContainerModel> getAllSandboxes();

    public boolean containSandbox(SandboxKey sandboxKey);

    public boolean containSandbox(String containerId);
}
