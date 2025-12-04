/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.agentscope.runtime.protocol.a2a;

import io.agentscope.runtime.protocol.Protocol;
import io.agentscope.runtime.protocol.ProtocolConfig;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Utils for {@link A2aProtocolConfig}.
 *
 * @author xiweng.yy
 */
public class A2aProtocolConfigUtils {

    /**
     * Get A2A protocol configuration from the provided configurations, returning a default one if absent
     *
     * @param protocolConfigs the provider of protocol configurations to search from
     * @return the first found A2A protocol configuration, or a newly built default one if none found
     */
    public static A2aProtocolConfig getConfigIfAbsent(ObjectProvider<ProtocolConfig> protocolConfigs) {
        return protocolConfigs.stream()
                .filter(protocolConfig -> Protocol.A2A.equals(protocolConfig.type()))
                .filter(protocolConfig -> A2aProtocolConfig.class.isAssignableFrom(protocolConfig.getClass()))
                .map(protocolConfig -> (A2aProtocolConfig) protocolConfig).findFirst()
                .orElse(new A2aProtocolConfig.Builder().build());
    }
}
