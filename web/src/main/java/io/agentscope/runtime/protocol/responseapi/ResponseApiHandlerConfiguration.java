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

import io.agentscope.runtime.engine.Runner;
import io.agentscope.runtime.protocol.ProtocolConfig;
import org.springframework.beans.factory.ObjectProvider;

public class ResponseApiHandlerConfiguration {

    private static volatile ResponseApiHandlerConfiguration INSTANCE;

    private final ResponseApiHandler responseApiHandler;

    public ResponseApiHandlerConfiguration(Runner runner, ResponseApiProtocolConfig responseApiProtocolConfig) {
        this.responseApiHandler = new ResponseApiHandler(runner, responseApiProtocolConfig);
    }

    public static ResponseApiHandlerConfiguration getInstance(Runner runner,
                                                        ObjectProvider<ProtocolConfig> protocolConfigs) {
        ResponseApiHandlerConfiguration inst = INSTANCE;
        if (inst == null) {
            synchronized (ResponseApiHandlerConfiguration.class) {
                if (INSTANCE == null) {
                    ResponseApiProtocolConfig responseApiProtocolConfig = ResponseApiProtocolConfigUtils.getConfigIfAbsent(protocolConfigs);
                    INSTANCE = new ResponseApiHandlerConfiguration(runner, responseApiProtocolConfig);
                }
                inst = INSTANCE;
            }
        }
        return inst;
    }

    public ResponseApiHandler responseApiHandler() {
        return this.responseApiHandler;
    }
}

