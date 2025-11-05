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

import java.util.Objects;

public class SandboxKey {
    private final String userID;
    private final String sessionID;
    private final SandboxType sandboxType;

    public SandboxKey(String userID, String sessionID, SandboxType sandboxType) {
        this.userID = userID;
        this.sessionID = sessionID;
        this.sandboxType = sandboxType;
    }

    public String getUserID() {
        return userID;
    }

    public String getSessionID() {
        return sessionID;
    }

    public SandboxType getSandboxType() {
        return sandboxType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SandboxKey that = (SandboxKey) o;
        return Objects.equals(userID, that.userID) && Objects.equals(sessionID, that.sessionID) && sandboxType == that.sandboxType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userID, sessionID, sandboxType);
    }

    @Override
    public String toString() {
        return "SandboxKey{" + "userID='" + userID + '\'' + ", sessionID='" + sessionID + '\'' + ", sandboxType=" + sandboxType + '}';
    }
}
