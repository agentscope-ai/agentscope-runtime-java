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

package io.agentscope;

import java.util.List;
import io.a2a.spec.AgentInterface;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.EventType;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.runtime.LocalDeployManager;
import io.agentscope.runtime.adapters.agentscope.AgentScopeAgentHandler;
import io.agentscope.runtime.app.AgentApp;
import io.agentscope.runtime.engine.schemas.AgentRequest;
import io.agentscope.runtime.protocol.a2a.A2aProtocolConfig;
import io.agentscope.runtime.protocol.a2a.ConfigurableAgentCard;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.a2a.common.RocketMQA2AConstant;
import reactor.core.publisher.Flux;

/**
 * Example demonstrating how to use AgentScope to proxy ReActAgent
 */
public class AgentScopeDeployRocketMQExample {
	private static final String ROCKETMQ_ENDPOINT = System.getProperty("rocketMQEndpoint", "");
	private static final String ROCKETMQ_NAMESPACE = System.getProperty("rocketMQNamespace", "");
	private static final String BIZ_TOPIC = System.getProperty("bizTopic", "");
	private static final String BIZ_CONSUMER_GROUP = System.getProperty("bizConsumerGroup", "");
	private static final String ACCESS_KEY = System.getProperty("rocketMQAK", "");
	private static final String SECRET_KEY = System.getProperty("rocketMQSK", "");
	private static final String DASHSCOPE_API_KEY = System.getProperty("apiKey", "");

	public static void main(String[] args) {
		if (!checkConfigParam()) {
			System.exit(1);
		}
		runAgent();
	}

	private static void runAgent() {
		AgentInterface agentInterface = new AgentInterface(RocketMQA2AConstant.ROCKETMQ_PROTOCOL, buildRocketMQUrl());
		ConfigurableAgentCard agentCard = new ConfigurableAgentCard.Builder().url(buildRocketMQUrl()).preferredTransport(RocketMQA2AConstant.ROCKETMQ_PROTOCOL).additionalInterfaces(List.of(agentInterface)).description("use rocketmq as transport").build();
		AgentApp agentApp = new AgentApp(agent(agentBuilder(dashScopeChatModel(DASHSCOPE_API_KEY))));
		agentApp.deployManager(LocalDeployManager.builder().protocolConfigs(List.of(new A2aProtocolConfig(agentCard, 60, 10))).port(10001).build());
		agentApp.cors(registry -> registry.addMapping("/**").allowedOriginPatterns("*").allowedMethods("GET", "POST", "PUT", "DELETE").allowCredentials(true));
		agentApp.run();
	}

	public static ReActAgent.Builder agentBuilder(DashScopeChatModel model) {
		return ReActAgent.builder().model(model).name("agentscope-a2a-example-agent").sysPrompt("You are an example of A2A(Agent2Agent) Protocol Agent. You can answer some simple question according to your knowledge.");
	}

	public static AgentScopeAgentHandler agent(ReActAgent.Builder builder) {
		return new AgentScopeAgentHandler() {
			@Override
			public boolean isHealthy() {
				return true;
			}

			@Override
			public Flux<?> streamQuery(AgentRequest request, Object messages) {
				ReActAgent agent = builder.build();
				StreamOptions streamOptions = StreamOptions.builder().eventTypes(EventType.REASONING, EventType.TOOL_RESULT).incremental(true).build();
				if (messages instanceof List<?>) {
					return agent.stream((List<Msg>)messages, streamOptions);
				} else if (messages instanceof Msg) {
					return agent.stream((Msg)messages, streamOptions);
				} else {
					Msg msg = Msg.builder().role(MsgRole.USER).build();
					return agent.stream(msg, streamOptions);
				}
			}
			@Override
			public String getName() {
				return builder.build().getName();
			}

			@Override
			public String getDescription() {
				return builder.build().getDescription();
			}
		};
	}
	public static DashScopeChatModel dashScopeChatModel(String dashScopeApiKey) {
		if (StringUtils.isEmpty(dashScopeApiKey)) {
			throw new IllegalStateException("DashScope API Key is empty, please set environment variable `AI_DASHSCOPE_API_KEY`");
		}
		return DashScopeChatModel.builder().apiKey(dashScopeApiKey).modelName("qwen-max").stream(true).enableThinking(true).build();
	}

	private static String buildRocketMQUrl() {
		if (StringUtils.isEmpty(ROCKETMQ_ENDPOINT) || StringUtils.isEmpty(BIZ_TOPIC)) {
			throw new RuntimeException("buildRocketMQUrl param error, please check rocketmq config");
		}
		return "http://" + ROCKETMQ_ENDPOINT + "/" + ROCKETMQ_NAMESPACE + "/" + BIZ_TOPIC;
	}

	private static boolean checkConfigParam() {
		if (StringUtils.isEmpty(ROCKETMQ_ENDPOINT) || StringUtils.isEmpty(BIZ_TOPIC) || StringUtils.isEmpty(BIZ_CONSUMER_GROUP) || StringUtils.isEmpty(DASHSCOPE_API_KEY)) {
			if (StringUtils.isEmpty(ROCKETMQ_ENDPOINT)) {
				System.err.println("rocketMQEndpoint is empty");
			}
			if (StringUtils.isEmpty(BIZ_TOPIC)) {
				System.err.println("bizTopic is empty");
			}
			if (StringUtils.isEmpty(BIZ_CONSUMER_GROUP)) {
				System.err.println("bizConsumerGroup is empty");
			}
			if (StringUtils.isEmpty(DASHSCOPE_API_KEY)) {
				System.err.println("apiKey is empty");
			}
			return false;
		}
		return true;
	}
}
