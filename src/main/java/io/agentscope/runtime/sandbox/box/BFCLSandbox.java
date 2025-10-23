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
package io.agentscope.runtime.sandbox.box;

import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;
import io.agentscope.runtime.sandbox.manager.registry.RegisterSandbox;

/**
 * BFCL Sandbox implementation
 * Corresponds to Python's BFCLSandbox
 * Training Sandbox class for managing and executing BFCL training-related tasks
 * 
 * <p>This class provides methods to create, manage, and interact with
 * BFCL training environment instances using specialized tool calls.
 * 
 * <p>BFCL supports multiple dataset types:
 * all, all_scoring, multi_turn, single_turn, live, non_live, non_python, python
 * 
 * <p>The dataset subtype can be configured via the DATASET_SUB_TYPE environment variable.
 * Default: multi_turn
 * 
 * <p>Environment variables:
 * <ul>
 *   <li>DATASET_SUB_TYPE: Dataset subtype (default: multi_turn)</li>
 *   <li>OPENAI_API_KEY: OpenAI API key (optional)</li>
 *   <li>BFCL_DATA_PATH: Path to BFCL data file (auto-configured based on DATASET_SUB_TYPE)</li>
 *   <li>BFCL_SPLID_ID_PATH: Path to BFCL split IDs file (auto-configured based on DATASET_SUB_TYPE)</li>
 * </ul>
 */
@RegisterSandbox(
    imageName = "agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-bfcl:latest",
    sandboxType = SandboxType.BFCL,
    runtimeConfig = {"shm_size=8.06gb"},
    securityLevel = "medium",
    timeout = 30,
    description = "bfcl Sandbox",
    environment = {
        "OPENAI_API_KEY=${OPENAI_API_KEY:}",
        "BFCL_DATA_PATH=/agentscope_runtime/training_box/bfcl/multi_turn/${DATASET_SUB_TYPE:multi_turn}_processed.jsonl",
        "BFCL_SPLID_ID_PATH=/agentscope_runtime/training_box/bfcl/multi_turn/${DATASET_SUB_TYPE:multi_turn}_split_ids.json"
    }
)
public class BFCLSandbox extends TrainingSandbox {
    
    public BFCLSandbox(SandboxManager managerApi, String userId, String sessionId) {
        this(managerApi, userId, sessionId, 3000);
    }
    
    public BFCLSandbox(
            SandboxManager managerApi,
            String userId,
            String sessionId,
            int timeout) {
        super(managerApi, userId, sessionId, SandboxType.BFCL, timeout);
    }
}

