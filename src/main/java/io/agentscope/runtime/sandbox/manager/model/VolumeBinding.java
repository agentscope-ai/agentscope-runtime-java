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
package io.agentscope.runtime.sandbox.manager.model;

/**
 * Volume binding configuration class
 * Used to represent Docker container volume mount configuration
 */
public class VolumeBinding {
    private String hostPath;
    private String containerPath;
    private String mode;

    public VolumeBinding(String hostPath, String containerPath, String mode) {
        this.hostPath = hostPath;
        this.containerPath = containerPath;
        this.mode = mode;
    }

    public VolumeBinding(String hostPath, String containerPath) {
        this(hostPath, containerPath, "rw");
    }

    public String getHostPath() {
        return hostPath;
    }

    public void setHostPath(String hostPath) {
        this.hostPath = hostPath;
    }

    public String getContainerPath() {
        return containerPath;
    }

    public void setContainerPath(String containerPath) {
        this.containerPath = containerPath;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    @Override
    public String toString() {
        return "manager.VolumeBinding{" +
                "hostPath='" + hostPath + '\'' +
                ", containerPath='" + containerPath + '\'' +
                ", mode='" + mode + '\'' +
                '}';
    }
}
