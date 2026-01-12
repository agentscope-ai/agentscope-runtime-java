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
package com.example.agentscope;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.runtime.engine.agents.agentscope.tools.ToolkitInit;
import io.agentscope.runtime.sandbox.box.BrowserSandbox;
import io.agentscope.runtime.sandbox.manager.ManagerConfig;
import io.agentscope.runtime.sandbox.manager.SandboxService;
import io.agentscope.runtime.sandbox.manager.client.container.BaseClientStarter;
import io.agentscope.runtime.sandbox.manager.client.container.docker.DockerClientStarter;

import java.util.List;

/**
 * StructuredOutputExample - Demonstrates structured output generation.
 */
public class SandboxStructuredExample {

    public static void main(String[] args) throws Exception {

        BaseClientStarter clientConfig = DockerClientStarter.builder().build();
        ManagerConfig managerConfig = ManagerConfig.builder()
                .clientConfig(clientConfig)
                .build();

        SandboxService service = new SandboxService(managerConfig);
        service.start();

        Toolkit toolkit = new Toolkit();
        try {
            BrowserSandbox browserSandbox = new BrowserSandbox(service, "agent-user", "agent-sessopn");
            toolkit.registerTool(ToolkitInit.BrowserNavigateTool(browserSandbox));
            String desktopUrl = browserSandbox.getDesktopUrl();
            System.out.println("GUI Desktop URL: " + desktopUrl);
        } catch (Exception ignored) {
        }

        // Create Agent
        ReActAgent agent =
                ReActAgent.builder()
                        .name("AnalysisAgent")
                        .sysPrompt(
                                "You are an intelligent analysis assistant. "
                                        + "Analyze user requests and provide structured responses.")
                        .model(
                                DashScopeChatModel.builder()
                                        .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                                        .modelName("qwen-max")
                                        .stream(true)
                                        .enableThinking(false)
                                        .formatter(new DashScopeChatFormatter())
                                        .build())
                        .toolkit(toolkit)
                        .memory(new InMemoryMemory())
                        .build();

        runExample(agent);

        System.out.println("\n=== All examples completed ===");
        service.close();
    }

    /**
     * Example 1: Extract product information from natural language description.
     */
    private static void runExample(ReActAgent agent) {
        String query =
                "Please search for the final teams of the 2022 World Cup and the list of players who returned them. Please note that you need to use a browser tool to search using Baidu";

        System.out.println("Query: " + query);
        System.out.println("\nRequesting structured output...\n");

        Msg userMsg =
                Msg.builder()
                        .role(MsgRole.USER)
                        .content(
                                TextBlock.builder()
                                        .text(query)
                                        .build())
                        .build();

        try {
            Msg msg = agent.call(userMsg, WorldCupFinalTeams.class).block();
            WorldCupFinalTeams result = msg.getStructuredData(WorldCupFinalTeams.class);

            System.out.println("Extracted structured data:");
            for (WorldCupFinalTeams.Team team : result.teams) {
                System.out.println("Country: " + team.country);
                System.out.println("Players: " + String.join(", ", team.players));
                System.out.println();
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Schema for FIFA World Cup FINAL teams
     */
    public static class WorldCupFinalTeams {
        public List<Team> teams;

        public WorldCupFinalTeams() {
        }

        public static class Team {
            public String country;
            public Coach coach;
            public List<String> players;

            public Team() {
            }
        }

        public static class Coach {
            public String name;
            public int age;

            public Coach() {
            }
        }
    }
}
