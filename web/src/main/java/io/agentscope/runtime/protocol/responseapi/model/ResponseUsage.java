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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseUsage {

    @JsonProperty("input_tokens")
    private Integer inputTokens;

    @JsonProperty("output_tokens")
    private Integer outputTokens;

    @JsonProperty("total_tokens")
    private Integer totalTokens;

    @JsonProperty("input_tokens_details")
    private Map<String, InputTokenDetail> inputTokensDetails;

    @JsonProperty("output_tokens_details")
    private Map<String, OutputTokenDetail> outputTokensDetails;

    public Integer getInputTokens() {
        return inputTokens;
    }

    public void setInputTokens(Integer inputTokens) {
        this.inputTokens = inputTokens;
    }

    public Integer getOutputTokens() {
        return outputTokens;
    }

    public void setOutputTokens(Integer outputTokens) {
        this.outputTokens = outputTokens;
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Integer totalTokens) {
        this.totalTokens = totalTokens;
    }

    public Map<String, InputTokenDetail> getInputTokensDetails() {
        return inputTokensDetails;
    }

    public void setInputTokensDetails(Map<String, InputTokenDetail> inputTokensDetails) {
        this.inputTokensDetails = inputTokensDetails;
    }

    public Map<String, OutputTokenDetail> getOutputTokensDetails() {
        return outputTokensDetails;
    }

    public void setOutputTokensDetails(Map<String, OutputTokenDetail> outputTokensDetails) {
        this.outputTokensDetails = outputTokensDetails;
    }
}

