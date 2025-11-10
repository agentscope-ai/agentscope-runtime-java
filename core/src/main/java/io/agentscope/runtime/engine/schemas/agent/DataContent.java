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

import java.util.Map;

/**
 * Data content class
 * Corresponds to the DataContent class in agent_schemas.py of the Python version
 */
public class DataContent extends Content {
    
    private Map<String, Object> data;
    
    public DataContent() {
        super(ContentType.DATA);
    }
    
    public DataContent(Map<String, Object> data) {
        super(ContentType.DATA);
        this.data = data;
    }
    
    public DataContent(Boolean delta, Map<String, Object> data, Integer index) {
        super(ContentType.DATA);
        this.setDelta(delta);
        this.data = data;
        this.setIndex(index);
    }
    
    public Map<String, Object> getData() {
        return data;
    }
    
    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
