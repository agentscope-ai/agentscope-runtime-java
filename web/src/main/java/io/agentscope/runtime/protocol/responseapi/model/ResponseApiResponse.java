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
 * POJO for Responses API response object.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponseApiResponse {

    private Boolean background;

    private Conversation conversation;

    @JsonProperty("created_at")
    private Integer createdAt;

    private Error error;

    private String id;

    @JsonProperty("incomplete_details")
    private IncompleteDetail incompleteDetail;

    private Object instructions;

    @JsonProperty("max_output_tokens")
    private Integer maxOutputTokens;

    @JsonProperty("max_tool_calls")
    private Integer maxToolCalls;

    private Map<String, Object> metadata;

    private String model;

    private String object;

    private List<OutputMessage> output;

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

    private String status;

    private Integer temperature;

    private ResponseText text;

    @JsonProperty("tool_choice")
    private Object toolChoice;

    private List<Object> tools;

    @JsonProperty("top_logprobs")
    private Integer topLogprobs;

    @JsonProperty("top_p")
    private Integer topP;

    private String truncation;

    private ResponseUsage usage;

    public Boolean getBackground() {
        return background;
    }

    public void setBackground(Boolean background) {
        this.background = background;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    public Integer getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Integer createdAt) {
        this.createdAt = createdAt;
    }

    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        this.error = error;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public IncompleteDetail getIncompleteDetail() {
        return incompleteDetail;
    }

    public void setIncompleteDetail(IncompleteDetail incompleteDetail) {
        this.incompleteDetail = incompleteDetail;
    }

    public Object getInstructions() {
        return instructions;
    }

    public void setInstructions(Object instructions) {
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

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public List<OutputMessage> getOutput() {
        return output;
    }

    public void setOutput(List<OutputMessage> output) {
        this.output = output;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTemperature() {
        return temperature;
    }

    public void setTemperature(Integer temperature) {
        this.temperature = temperature;
    }

    public ResponseText getText() {
        return text;
    }

    public void setText(ResponseText text) {
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

    public ResponseUsage getUsage() {
        return usage;
    }

    public void setUsage(ResponseUsage usage) {
        this.usage = usage;
    }
}

