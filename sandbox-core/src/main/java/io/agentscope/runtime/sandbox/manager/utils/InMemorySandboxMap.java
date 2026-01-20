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

package io.agentscope.runtime.sandbox.manager.utils;

import io.agentscope.runtime.sandbox.manager.model.container.ContainerModel;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxKey;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySandboxMap implements SandboxMap{
    private final Map<String, ContainerModel> idContainerMap = new ConcurrentHashMap<>();
    private final Map<SandboxKey, String> keyIdMap = new ConcurrentHashMap<>();
    private final Map<String, Long> refCountMap = new ConcurrentHashMap<>();

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

    @Override
    public long getTTL(String containerId) {
        return -1;
    }

    @Override
    public long incrementRefCount(String containerId) {
        if (containerId == null || containerId.isEmpty()) return 0;
        return refCountMap.merge(containerId, 1L, Long::sum);
    }

    @Override
    public long decrementRefCount(String containerId) {
        if (containerId == null || containerId.isEmpty()) return 0;
        return refCountMap.compute(containerId, (k, v) -> {
            if (v == null || v <= 0) return 0L;
            return v - 1;
        });
    }

    @Override
    public long getRefCount(String containerId) {
        if (containerId == null || containerId.isEmpty()) return 0;
        return refCountMap.getOrDefault(containerId, 0L);
    }
}
