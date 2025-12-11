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
import org.springframework.beans.factory.ObjectProvider;

/**
 * Utils for {@link ResponseApiProtocolConfig}.
 */
public class ResponseApiProtocolConfigUtils {

    /**
     * Get ResponseAPI protocol configuration from the provided configurations, returning a default one if absent
     *
     * @param protocolConfigs the provider of protocol configurations to search from
     * @return the first found ResponseAPI protocol configuration, or a newly built default one if none found
     */
    public static ResponseApiProtocolConfig getConfigIfAbsent(ObjectProvider<ProtocolConfig> protocolConfigs) {
        return protocolConfigs.stream()
                .filter(protocolConfig -> Protocol.ResponseAPI.equals(protocolConfig.type()))
                .filter(protocolConfig -> ResponseApiProtocolConfig.class.isAssignableFrom(protocolConfig.getClass()))
                .map(protocolConfig -> (ResponseApiProtocolConfig) protocolConfig).findFirst()
                .orElse(new ResponseApiProtocolConfig.Builder().build());
    }
}

