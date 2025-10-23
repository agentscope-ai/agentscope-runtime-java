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
package io.agentscope.runtime.sandbox.tools.utils;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import org.springframework.ai.chat.model.ToolContext;
import java.util.UUID;

public class ContextUtils {

    public static String extractUserID(ToolContext toolContext) {
        RunnableConfig runnableConfig = (RunnableConfig) toolContext.getContext().get("config");
        if (runnableConfig != null && runnableConfig.metadata("user_id").isPresent()) {
            return runnableConfig.metadata("user_id").orElse(UUID.randomUUID().toString()).toString();
        }
        return UUID.randomUUID().toString();
    }

    public static String extractSessionID(ToolContext toolContext) {
        RunnableConfig runnableConfig = (RunnableConfig) toolContext.getContext().get("config");
        if (runnableConfig != null && runnableConfig.metadata("session_id").isPresent()) {
            return runnableConfig.metadata("session_id").orElse(UUID.randomUUID().toString()).toString();
        }
        return UUID.randomUUID().toString();
    }

    public static String[] extractUserAndSessionID(ToolContext toolContext) {
        return new String[]{extractUserID(toolContext), extractSessionID(toolContext)};
    }
}
