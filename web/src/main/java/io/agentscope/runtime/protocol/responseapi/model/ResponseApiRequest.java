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
package io.agentscope.runtime.protocol.responseapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * POJO for OpenAI Responses API request body.
 * */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponseApiRequest {

    private List<Object> include;

    private Object input;

    private String instructions;

    @JsonProperty("max_output_tokens")
    private Integer maxOutputTokens;

    @JsonProperty("max_tool_calls")
    private Integer maxToolCalls;

    private Map<String, Object> metadata;

    private String model;

    @JsonProperty("parallel_tool_calls")
    private Boolean parallelToolCalls;

    @JsonProperty("previous_response_id")
    private String previousResponseId;

    private ResponsePrompt prompt;

    @JsonProperty("prompt_cache_key")
    private String promptCacheKey;

    @JsonProperty("prompt_cache_retention")
    private String promptCacheRetention;

    private ResponseReasoning reasoning;

    @JsonProperty("safety_identifier")
    private String safetyIdentifier;

    @JsonProperty("service_tier")
    private String serviceTier = "auto";

    private Boolean store = Boolean.TRUE;

    private Boolean stream = Boolean.FALSE;

    @JsonProperty("stream_options")
    private Object streamOptions;

    private float temperature = 1.0f;

    private Object text;

    @JsonProperty("tool_choice")
    private Object toolChoice;

    private List<Object> tools;

    @JsonProperty("top_logprobs")
    private Integer topLogprobs;

    @JsonProperty("top_p")
    private Integer topP;

    private String truncation = "disabled";

    public Boolean getBackground() {
        return background;
    }

    public void setBackground(Boolean background) {
        this.background = background;
    }

    private Boolean background;

    private Conversation conversation;

    public Conversation getConversation() {
        return conversation;
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    public List<Object> getInclude() {
        return include;
    }

    public void setInclude(List<Object> include) {
        this.include = include;
    }

    public Object getInput() {
        return input;
    }

    public void setInput(Object input) {
        this.input = input;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public Integer getMaxOutputTokens() {
        return maxOutputTokens;
    }

    public void setMaxOutputTokens(Integer maxOutputTokens) {
        this.maxOutputTokens = maxOutputTokens;
    }

    public Integer getMaxToolCalls() {
        return maxToolCalls;
    }

    public void setMaxToolCalls(Integer maxToolCalls) {
        this.maxToolCalls = maxToolCalls;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Boolean getParallelToolCalls() {
        return parallelToolCalls;
    }

    public void setParallelToolCalls(Boolean parallelToolCalls) {
        this.parallelToolCalls = parallelToolCalls;
    }

    public String getPreviousResponseId() {
        return previousResponseId;
    }

    public void setPreviousResponseId(String previousResponseId) {
        this.previousResponseId = previousResponseId;
    }

    public ResponsePrompt getPrompt() {
        return prompt;
    }

    public void setPrompt(ResponsePrompt prompt) {
        this.prompt = prompt;
    }

    public String getPromptCacheKey() {
        return promptCacheKey;
    }

    public void setPromptCacheKey(String promptCacheKey) {
        this.promptCacheKey = promptCacheKey;
    }

    public String getPromptCacheRetention() {
        return promptCacheRetention;
    }

    public void setPromptCacheRetention(String promptCacheRetention) {
        this.promptCacheRetention = promptCacheRetention;
    }

    public ResponseReasoning getReasoning() {
        return reasoning;
    }

    public void setReasoning(ResponseReasoning reasoning) {
        this.reasoning = reasoning;
    }

    public String getSafetyIdentifier() {
        return safetyIdentifier;
    }

    public void setSafetyIdentifier(String safetyIdentifier) {
        this.safetyIdentifier = safetyIdentifier;
    }

    public String getServiceTier() {
        return serviceTier;
    }

    public void setServiceTier(String serviceTier) {
        this.serviceTier = serviceTier;
    }

    public Boolean getStore() {
        return store;
    }

    public void setStore(Boolean store) {
        this.store = store;
    }

    public Boolean getStream() {
        return stream;
    }

    public void setStream(Boolean stream) {
        this.stream = stream;
    }

    public Object getStreamOptions() {
        return streamOptions;
    }

    public void setStreamOptions(Object streamOptions) {
        this.streamOptions = streamOptions;
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    public Object getText() {
        return text;
    }

    public void setText(Object text) {
        this.text = text;
    }

    public Object getToolChoice() {
        return toolChoice;
    }

    public void setToolChoice(Object toolChoice) {
        this.toolChoice = toolChoice;
    }

    public List<Object> getTools() {
        return tools;
    }

    public void setTools(List<Object> tools) {
        this.tools = tools;
    }

    public Integer getTopLogprobs() {
        return topLogprobs;
    }

    public void setTopLogprobs(Integer topLogprobs) {
        this.topLogprobs = topLogprobs;
    }

    public Integer getTopP() {
        return topP;
    }

    public void setTopP(Integer topP) {
        this.topP = topP;
    }

    public String getTruncation() {
        return truncation;
    }

    public void setTruncation(String truncation) {
        this.truncation = truncation;
    }
}

