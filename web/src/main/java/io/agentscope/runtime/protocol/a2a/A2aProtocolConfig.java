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

/**
 * {@link io.agentscope.runtime.protocol.ProtocolConfig} implementation for A2A protocol.
 *
 * @author xiweng.yy
 */
public class A2aProtocolConfig implements ProtocolConfig {

    private final ConfigurableAgentCard agentCard;

    private final int agentCompletionTimeoutSeconds;

    private final int consumptionCompletionTimeoutSeconds;

    public A2aProtocolConfig(ConfigurableAgentCard agentCard, int agentCompletionTimeoutSeconds, int consumptionCompletionTimeoutSeconds) {
        this.agentCard = agentCard;
        this.agentCompletionTimeoutSeconds = agentCompletionTimeoutSeconds;
        this.consumptionCompletionTimeoutSeconds = consumptionCompletionTimeoutSeconds;
    }

    public ConfigurableAgentCard getAgentCard() {
        return agentCard;
    }

    public int getAgentCompletionTimeoutSeconds() {
        return agentCompletionTimeoutSeconds;
    }

    public int getConsumptionCompletionTimeoutSeconds() {
        return consumptionCompletionTimeoutSeconds;
    }

    @Override
    public Protocol type() {
        return Protocol.A2A;
    }

    public static class Builder {

        protected ConfigurableAgentCard agentCard;

        protected int agentCompletionTimeoutSeconds = 60;

        protected int consumptionCompletionTimeoutSeconds = 10;

        public Builder agentCard(ConfigurableAgentCard agentCard) {
            this.agentCard = agentCard;
            return this;
        }

        public Builder agentCompletionTimeoutSeconds(int agentCompletionTimeoutSeconds) {
            this.agentCompletionTimeoutSeconds = agentCompletionTimeoutSeconds;
            return this;
        }

        public Builder consumptionCompletionTimeoutSeconds(int consumptionCompletionTimeoutSeconds) {
            this.consumptionCompletionTimeoutSeconds = consumptionCompletionTimeoutSeconds;
            return this;
        }

        public A2aProtocolConfig build() {
            if (null == agentCard) {
                agentCard = new ConfigurableAgentCard.Builder().build();
            }
            return new A2aProtocolConfig(agentCard, agentCompletionTimeoutSeconds, consumptionCompletionTimeoutSeconds);
        }
    }
}
