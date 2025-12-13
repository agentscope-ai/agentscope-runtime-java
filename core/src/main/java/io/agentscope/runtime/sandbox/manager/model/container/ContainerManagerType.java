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
package io.agentscope.runtime.sandbox.manager.model.container;

/**
 * Container management type enumeration
 */
public enum ContainerManagerType {
    DOCKER("docker"), KUBERNETES("kubernetes"), CLOUD("cloud"), AGENTRUN("agentrun"), FC("fc");

    private final String value;

    ContainerManagerType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ContainerManagerType fromString(String value) {
        for (ContainerManagerType type : ContainerManagerType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown container manager type: " + value);
    }
}
