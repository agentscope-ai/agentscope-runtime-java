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

package io.agentscope.runtime.engine.schemas.agent;

import java.util.HashMap;
import java.util.Map;

/**
 * Function call output class
 * Corresponds to the FunctionCallOutput class in agent_schemas.py of the Python version
 */
public class FunctionCallOutput {
    
    private String callId;
    private String output;
    
    public FunctionCallOutput() {}
    
    public FunctionCallOutput(String callId, String output) {
        this.callId = callId;
        this.output = output;
    }
    
    public String getCallId() {
        return callId;
    }
    
    public void setCallId(String callId) {
        this.callId = callId;
    }
    
    public String getOutput() {
        return output;
    }
    
    public void setOutput(String output) {
        this.output = output;
    }
    
    /**
     * Convert to Map format
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("call_id", callId);
        map.put("output", output);
        return map;
    }
    
    /**
     * Create FunctionCallOutput from Map
     */
    public static FunctionCallOutput fromMap(Map<String, Object> map) {
        FunctionCallOutput output = new FunctionCallOutput();
        output.setCallId((String) map.get("call_id"));
        output.setOutput((String) map.get("output"));
        return output;
    }
}
