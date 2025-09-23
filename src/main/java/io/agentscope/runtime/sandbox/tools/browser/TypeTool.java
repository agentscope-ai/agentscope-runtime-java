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
import org.springframework.ai.chat.model.ToolContext;
import io.agentscope.runtime.sandbox.tools.SandboxTools;

import java.util.function.BiFunction;

public class TypeTool implements BiFunction<TypeTool.Request, ToolContext, TypeTool.Response> {

    @Override
    public Response apply(Request request, ToolContext toolContext) {
        String result = new SandboxTools().browser_type(
                request.element, request.ref, request.text, request.submit, request.slowly);
        return new Response(result, "Browser type completed");
    }

    public record Request(
            @JsonProperty(required = true, value = "element") String element,
            @JsonProperty(required = true, value = "ref") String ref,
            @JsonProperty(required = true, value = "text") String text,
            @JsonProperty("submit") Boolean submit,
            @JsonProperty("slowly") Boolean slowly
    ) { 
        public Request {
            // Provide default value handling for optional parameters
            if (submit == null) {
                submit = false;
            }
            if (slowly == null) {
                slowly = false;
            }
        }
    }

    @JsonClassDescription("The result contains browser tool output and message")
    public record Response(String result, String message) {}
}
