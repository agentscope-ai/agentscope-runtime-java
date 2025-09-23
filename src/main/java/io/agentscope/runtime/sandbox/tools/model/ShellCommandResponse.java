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
package io.agentscope.runtime.sandbox.tools.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ShellCommandResponse {
    @JsonProperty("result")
    private String result;

    @JsonProperty("error")
    private String error;

    @JsonProperty("success")
    private boolean success;

    public ShellCommandResponse() {}

    public ShellCommandResponse(String result, String error, boolean success) {
        this.result = result;
        this.error = error;
        this.success = success;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    @Override
    public String toString() {
        return "ShellCommandResponse{" +
                "result='" + result + '\'' +
                ", error='" + error + '\'' +
                ", success=" + success +
                '}';
    }
}
