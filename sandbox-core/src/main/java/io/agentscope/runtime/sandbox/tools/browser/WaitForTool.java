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

import io.agentscope.runtime.sandbox.box.BrowserSandbox;
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.tools.SandboxTool;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class WaitForTool extends BrowserSandboxTool {

    private static final Logger logger = LoggerFactory.getLogger(WaitForTool.class);

    public WaitForTool() {
        super("browser_wait_for", "browser", "Wait for a condition in the browser");
        schema = new HashMap<>();
        
        Map<String, Object> timeProperty = new HashMap<>();
        timeProperty.put("type", "number");
        timeProperty.put("description", "time in seconds");
        
        Map<String, Object> textProperty = new HashMap<>();
        textProperty.put("type", "string");
        textProperty.put("description", "Text to wait for");
        
        Map<String, Object> textGoneProperty = new HashMap<>();
        textGoneProperty.put("type", "string");
        textGoneProperty.put("description", "Text to wait to disappear");

        Map<String, Object> properties = new HashMap<>();
        properties.put("time", timeProperty);
        properties.put("text", textProperty);
        properties.put("textGone", textGoneProperty);

        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("description", "Request object to wait for condition");
    }

    @Override
    public SandboxTool bind(Sandbox sandbox) {
        this.sandbox = sandbox;
        return this;
    }

    public String browser_wait_for(Double time, String text, String textGone) {
        try {
            if(sandbox instanceof BrowserSandbox browserSandbox){
                return browserSandbox.waitFor(time, text, textGone);
            }
            throw new RuntimeException("Only BrowserSandbox supported in browser wait for tool");
        } catch (Exception e) {
            String errorMsg = "Browser Wait For Error: " + e.getMessage();
            logger.error(errorMsg);
            return errorMsg;
        }
    }

    public record Request(
            @JsonProperty("time") @JsonPropertyDescription("time in seconds") Double time,
            @JsonProperty("text") String text,
            @JsonProperty("textGone") String textGone
    ) {
        public Request {
            if (time == null) {
                time = 0.0;
            }
            if (text == null) {
                text = "";
            }
            if (textGone == null) {
                textGone = "";
            }
        }
    }

    @JsonClassDescription("The result contains browser tool output and message")
    public record Response(String result, String message) {}
}
