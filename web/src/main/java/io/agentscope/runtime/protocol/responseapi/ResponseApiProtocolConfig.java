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

package io.agentscope.runtime.protocol.responseapi;

import io.agentscope.runtime.protocol.Protocol;
import io.agentscope.runtime.protocol.ProtocolConfig;

/**
 * {@link ProtocolConfig} implementation for ResponseAPI protocol.
 */
public class ResponseApiProtocolConfig implements ProtocolConfig {

    private final int completionTimeoutSeconds;

    public ResponseApiProtocolConfig(int completionTimeoutSeconds) {
        this.completionTimeoutSeconds = completionTimeoutSeconds;
    }

    public int getCompletionTimeoutSeconds() {
        return completionTimeoutSeconds;
    }

    @Override
    public Protocol type() {
        return Protocol.ResponseAPI;
    }

    public static class Builder {

        protected int completionTimeoutSeconds = 60;

        public Builder completionTimeoutSeconds(int completionTimeoutSeconds) {
            this.completionTimeoutSeconds = completionTimeoutSeconds;
            return this;
        }

        public ResponseApiProtocolConfig build() {
            return new ResponseApiProtocolConfig(completionTimeoutSeconds);
        }
    }
}

