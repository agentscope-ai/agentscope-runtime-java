/*
 * Copyright 1999-2025 Alibaba Group Holding Ltd.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import com.alibaba.nacos.api.exception.NacosException;
import io.a2a.client.http.JdkA2AHttpClient;
import io.agentscope.core.a2a.agent.A2aAgent;
import io.agentscope.core.a2a.agent.A2aAgentConfig;
import io.agentscope.core.a2a.agent.A2aAgentConfig.A2aAgentConfigBuilder;
import io.agentscope.core.a2a.agent.card.WellKnownAgentCardResolver;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import org.apache.rocketmq.a2a.transport.RocketMQTransport;
import org.apache.rocketmq.a2a.transport.RocketMQTransportConfig;
import reactor.core.publisher.Flux;

public class A2aAgentCallerExample {
    private static final String USER_INPUT_PREFIX = "\u001B[34mYou>\u001B[0m ";
    private static final String AGENT_RESPONSE_PREFIX = "\u001B[32mAgent>\u001B[0m ";
    private static final String ACCESS_KEY = System.getProperty("rocketMQAK");
    private static final String SECRET_KEY = System.getProperty("rocketMQSK");
    private static final String WORK_AGENT_RESPONSE_TOPIC = System.getProperty("workAgentResponseTopic");
    private static final String WORK_AGENT_RESPONSE_GROUP_ID = System.getProperty("workAgentResponseGroupID");
    private static final String ROCKETMQ_NAMESPACE = System.getProperty("rocketMQNamespace");
    private static final String AGENT_NAME = "agentscope-a2a-rocketmq-example-agent";

    // Can change this to false disable streaming.
    static boolean streaming = true;

    public static void main(String[] args) throws NacosException {
        RocketMQTransportConfig rocketMQTransportConfig = new RocketMQTransportConfig();
        rocketMQTransportConfig.setAccessKey(ACCESS_KEY);
        rocketMQTransportConfig.setSecretKey(SECRET_KEY);
        rocketMQTransportConfig.setWorkAgentResponseTopic(WORK_AGENT_RESPONSE_TOPIC);
        rocketMQTransportConfig.setWorkAgentResponseGroupID(WORK_AGENT_RESPONSE_GROUP_ID);
        rocketMQTransportConfig.setNamespace(ROCKETMQ_NAMESPACE);
        rocketMQTransportConfig.setHttpClient(new JdkA2AHttpClient());
        A2aAgentConfig a2aAgentConfig = new A2aAgentConfigBuilder().withTransport(RocketMQTransport.class, rocketMQTransportConfig).build();
        A2aAgent agent = A2aAgent.builder().a2aAgentConfig(a2aAgentConfig).name(AGENT_NAME).agentCardResolver(WellKnownAgentCardResolver.builder().baseUrl("http://localhost:10001").build()).build();
        startExample(agent);
    }

    private static void startExample(A2aAgent agent) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                // 用户输入提示
                System.out.print(USER_INPUT_PREFIX);
                String input = reader.readLine();
                // 退出条件检测
                if (input == null || input.trim().equalsIgnoreCase("exit") || input.trim().equalsIgnoreCase("quit")) {
                    System.out.println(AGENT_RESPONSE_PREFIX + "Bye！");
                    break;
                }
                System.out.println(AGENT_RESPONSE_PREFIX + "I have received your question: " + input);
                // 流式输出带前缀
                System.out.print(AGENT_RESPONSE_PREFIX);
                // 处理输入并获取响应
                processInput(agent, input).doOnNext(System.out::print).then().block();
                // 换行分隔
                System.out.println();
            }
        } catch (IOException e) {
            System.err.println("input error: " + e.getMessage());
        }
    }

    private static Flux<String> processInput(A2aAgent agent, String input) {
        Msg msg = Msg.builder().role(MsgRole.USER).content(TextBlock.builder().text(input).build()).build();
        return agent.stream(msg).map(event -> {
            if (streaming && event.isLast()) {
                // The last message is whole artifact message result, which has been solved and print in before event handle.
                // Weather need to handle the last message, depends on the use case.
                return "";
            }
            Msg message = event.getMessage();
            StringBuilder partText = new StringBuilder();
            message.getContent().stream().filter(block -> block instanceof TextBlock).map(block -> (TextBlock) block)
                    .forEach(block -> partText.append(block.getText()));
            return partText.toString();
        });
    }
}
