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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class SandboxKey {
    private static final Logger logger = LoggerFactory.getLogger(SandboxKey.class);
    private final String userID;
    private final String sessionID;
    private final String sandboxType;
    private final String imageID;

    public SandboxKey(String userID, String sessionID, String sandboxType, String imageID) {
        this.userID = userID;
        this.sessionID = sessionID;
        this.sandboxType = sandboxType;
        this.imageID = imageID;
    }

    public SandboxKey(String userID, String sessionID, String sandboxType) {
        this(userID, sessionID, sandboxType, sandboxType == SandboxType.AGENTBAY ? "linux_latest" : "");
        if (sandboxType == SandboxType.AGENTBAY) {
            logger.warn("Creating SandboxKey without default \"linux_latest\" imageID for AGENTBAY type.");
        }
    }

    public String getUserID() {
        return userID;
    }

    public String getSessionID() {
        return sessionID;
    }

    public String getSandboxType() {
        return sandboxType;
    }

    public String getImageID() {
        return imageID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SandboxKey that = (SandboxKey) o;
        if(sandboxType != SandboxType.AGENTBAY){
            return Objects.equals(userID, that.userID) && Objects.equals(sessionID, that.sessionID) && sandboxType == that.sandboxType;
        }
        return Objects.equals(userID, that.userID) && Objects.equals(sessionID, that.sessionID) && sandboxType == that.sandboxType && Objects.equals(imageID, that.imageID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userID, sessionID, sandboxType, imageID);
    }

    @Override
    public String toString() {
        return "SandboxKey{" + "userID='" + userID + '\'' + ", sessionID='" + sessionID + '\'' + ", sandboxType=" + sandboxType + '\'' + ", imageId=" + imageID + '}';
    }
}
