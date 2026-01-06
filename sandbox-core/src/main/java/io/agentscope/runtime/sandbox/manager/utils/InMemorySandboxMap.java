package io.agentscope.runtime.sandbox.manager.utils;

import io.agentscope.runtime.sandbox.manager.model.container.ContainerModel;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxKey;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySandboxMap implements SandboxMap{
    private final Map<String, ContainerModel> idContainerMap = new ConcurrentHashMap<>();
    private final Map<SandboxKey, String> keyIdMap = new ConcurrentHashMap<>();

    @Override
    public void addSandbox(SandboxKey sandboxKey, ContainerModel containerModel) {
        if(sandboxKey == null || containerModel == null){
            return;
        }
        keyIdMap.put(sandboxKey, containerModel.getContainerId());
        idContainerMap.put(containerModel.getContainerId(), containerModel);
    }

    @Override
    public ContainerModel getSandbox(SandboxKey sandboxKey) {
        if(!keyIdMap.containsKey(sandboxKey)){
            return null;
        }
        return idContainerMap.get(keyIdMap.get(sandboxKey));
    }

    @Override
    public boolean removeSandbox(SandboxKey sandboxKey) {
        if (sandboxKey == null) {
            return false;
        }
        String containerId = keyIdMap.remove(sandboxKey);
        if (containerId != null) {
            idContainerMap.remove(containerId);
        }
        return true;
    }

    @Override
    public ContainerModel getSandbox(String containerId) {
        if (containerId == null || containerId.isEmpty()) {
            return null;
        }
        return idContainerMap.get(containerId);
    }

    @Override
    public void removeSandbox(String containerId) {
        if (containerId == null || containerId.isEmpty()) {
            return;
        }
        ContainerModel containerModel = idContainerMap.remove(containerId);
        if (containerModel != null) {
            keyIdMap.values().removeIf(id -> id.equals(containerId));
        }
    }

    @Override
    public Map<String, ContainerModel> getAllSandboxes() {
        if (idContainerMap.isEmpty()) {
            return Map.of();
        }
        return idContainerMap;
    }

    @Override
    public boolean containSandbox(SandboxKey sandboxKey) {
        if (sandboxKey == null) {
            return false;
        }
        return keyIdMap.containsKey(sandboxKey);
    }

    @Override
    public boolean containSandbox(String containerId) {
        if(containerId == null || containerId.isEmpty()){
            return false;
        }
        return idContainerMap.containsKey(containerId);
    }
}
