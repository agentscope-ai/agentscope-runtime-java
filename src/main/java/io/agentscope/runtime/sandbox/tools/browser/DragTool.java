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
import org.springframework.ai.chat.model.ToolContext;
import io.agentscope.runtime.sandbox.tools.SandboxTools;

import java.util.function.BiFunction;

/**
 * Browser drag tool
 */
public class DragTool implements BiFunction<DragTool.Request, ToolContext, DragTool.Response> {

    @Override
    public Response apply(Request request, ToolContext toolContext) {
        String result = new SandboxTools().browser_drag(request.startElement, request.startRef, request.endElement, request.endRef);
        return new Response(result, "Browser drag completed");
    }

    public record Request(
            @JsonProperty(required = true, value = "startElement")
            @JsonPropertyDescription("Human-readable source element description")
            String startElement,
            @JsonProperty(required = true, value = "startRef")
            @JsonPropertyDescription("Exact source element reference from the page snapshot")
            String startRef,
            @JsonProperty(required = true, value = "endElement")
            @JsonPropertyDescription("Human-readable target element description")
            String endElement,
            @JsonProperty(required = true, value = "endRef")
            @JsonPropertyDescription("Exact target element reference from the page snapshot")
            String endRef
    ) { }

    @JsonClassDescription("The result contains browser tool output and message")
    public record Response(String result, String message) {}
}
