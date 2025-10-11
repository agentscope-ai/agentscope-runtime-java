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
package io.agentscope.runtime.sandbox.tools.browser;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.agentscope.runtime.sandbox.tools.ContextUtils;
import org.springframework.ai.chat.model.ToolContext;
import io.agentscope.runtime.sandbox.tools.SandboxTools;

import java.util.function.BiFunction;

/**
 * Browser key press tool
 */
public class PressKeyTool implements BiFunction<PressKeyTool.Request, ToolContext, PressKeyTool.Response> {

    @Override
    public Response apply(Request request, ToolContext toolContext) {
        String[] userAndSession = ContextUtils.extractUserAndSessionID(toolContext);
        String userID = userAndSession[0];
        String sessionID = userAndSession[1];
        String result = new SandboxTools().browser_press_key(request.key, userID, sessionID);
        return new Response(result, "Browser press key completed");
    }

    public record Request(
            @JsonProperty(required = true, value = "key")
            @JsonPropertyDescription("Name of the key to press or a character to generate")
            String key
    ) { }

    @JsonClassDescription("The result contains browser tool output and message")
    public record Response(String result, String message) {}
}
