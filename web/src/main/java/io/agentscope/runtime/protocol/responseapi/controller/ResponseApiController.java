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

package io.agentscope.runtime.protocol.responseapi.controller;

import io.agentscope.runtime.engine.Runner;
import io.agentscope.runtime.protocol.ProtocolConfig;
import io.agentscope.runtime.protocol.responseapi.ResponseApiHandler;
import io.agentscope.runtime.protocol.responseapi.ResponseApiHandlerConfiguration;
import io.agentscope.runtime.protocol.responseapi.model.ResponseApiRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Logger;

/**
 * REST controller for OpenAI Responses API protocol endpoints.
 */
@RestController
@RequestMapping("/compatible-mode/v1")
public class ResponseApiController {

    private static final Logger logger = Logger.getLogger(ResponseApiController.class.getName());

    private final ResponseApiHandler responseApiHandler;

    public ResponseApiController(Runner runner, ObjectProvider<ProtocolConfig> protocolConfigs) {
        this.responseApiHandler = ResponseApiHandlerConfiguration.getInstance(runner, protocolConfigs).responseApiHandler();
    }

    /**
     * Stream chat completions endpoint (OpenAI-compatible Responses API)
     */
    @PostMapping(value = "/responses",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE})
    @ResponseBody
    public Object streamChatCompletions(@RequestBody ResponseApiRequest request) {
        logger.info("Received OpenAI Responses API chat completion request");
        
        // Check if streaming is requested (default to true for Responses API)
        Boolean stream = request.getStream();

        if (Boolean.TRUE.equals(stream)) {
            return responseApiHandler.handleStreamingResponse(request);
        } else {
            logger.info("Non-streaming request received, returning aggregated response");
            return responseApiHandler.handleNonStreamingResponse(request);
        }
    }
}
