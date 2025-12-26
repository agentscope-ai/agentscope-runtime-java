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

package io.agentscope.runtime.protocol.a2a.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.a2a.server.ServerCallContext;
import io.a2a.spec.*;
import io.a2a.util.Utils;
import io.agentscope.runtime.engine.Runner;
import io.agentscope.runtime.protocol.ProtocolConfig;
import io.agentscope.runtime.protocol.a2a.AgentHandlerConfiguration;
import io.agentscope.runtime.protocol.a2a.JSONRPCHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.reactivestreams.FlowAdapters;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Flow;
import java.util.logging.Logger;

@RestController
@RequestMapping("/a2a")
public class A2aController {

    Logger logger = Logger.getLogger(A2aController.class.getName());

    private final JSONRPCHandler jsonRpcHandler;

    public A2aController(Runner runner, AgentCard agentCard, ObjectProvider<ProtocolConfig> protocolConfigs) {
        this.jsonRpcHandler = AgentHandlerConfiguration.getInstance(runner, agentCard, protocolConfigs).jsonrpcHandler();
    }

    @PostMapping(value = "/", consumes = MediaType.APPLICATION_JSON_VALUE, produces = {MediaType.APPLICATION_JSON_VALUE,
            MediaType.TEXT_EVENT_STREAM_VALUE})
    @ResponseBody
    public Object handleRequest(@RequestBody String body, HttpServletRequest httpRequest) {
        ServerCallContext context = buildServerCallContext(httpRequest);
        boolean streaming = isStreamingRequest(body, context);
        context.getState().put(ContextKeys.IS_STREAM_KEY, streaming);
        Object result;
        try {
            if (streaming) {
                result = handleStreamRequest(body, context);
                logger.info("Handling streaming request, returning SSE Flux");
            } else {
                result = handleNonStreamRequest(body, context);
                logger.info("Handling non-streaming request, returning JSON response");
            }
        } catch (JsonProcessingException e) {
            logger.severe("JSON parsing error: " + e.getMessage());
            result = new JSONRPCErrorResponse(null, new JSONParseError());
        }
        return result;
    }

    protected boolean isStreamingRequest(String requestBody, ServerCallContext context) {
        try {
            JsonNode node = Utils.OBJECT_MAPPER.readTree(requestBody);
            JsonNode method = node != null ? node.get("method") : null;
            String methodName = method != null ? method.asText() : null;
            if (methodName != null) {
                if (null != context && null != context.getState()) {
                    context.getState().put(ContextKeys.METHOD_NAME_KEY, methodName);
                }
                return SendStreamingMessageRequest.METHOD.equals(methodName) || TaskResubscriptionRequest.METHOD.equals(methodName);
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    protected Flux<ServerSentEvent<String>> handleStreamRequest(String body, ServerCallContext context)
            throws JsonProcessingException {
        StreamingJSONRPCRequest<?> request = Utils.OBJECT_MAPPER.readValue(body, StreamingJSONRPCRequest.class);
        Flow.Publisher<? extends JSONRPCResponse<?>> publisher;
        if (request instanceof SendStreamingMessageRequest req) {
            publisher = jsonRpcHandler.onMessageSendStream(req, context);
        } else if (request instanceof TaskResubscriptionRequest req) {
            publisher = jsonRpcHandler.onResubscribeToTask(req, context);
        } else {
            return Flux.just(createErrorSSE(generateErrorResponse(request, new UnsupportedOperationError())));
        }

        return Flux.from(FlowAdapters.toPublisher(publisher)).map(this::convertToSSE)
                .delaySubscription(Duration.ofMillis(10));
    }

    private ServerSentEvent<String> convertToSSE(JSONRPCResponse<?> response) {
        try {
            String data = Utils.OBJECT_MAPPER.writeValueAsString(response);
            ServerSentEvent.Builder<String> builder = ServerSentEvent.<String>builder().data(data).event("jsonrpc");
            if (response.getId() != null) {
                builder.id(response.getId().toString());
            }
            return builder.build();
        } catch (Exception e) {
            logger.severe("Error converting response to SSE: " + e.getMessage());
            return ServerSentEvent.<String>builder().data("{\"error\":\"Internal conversion error\"}").event("error")
                    .build();
        }
    }

    private ServerSentEvent<String> createErrorSSE(JSONRPCResponse<?> errorResponse) {
        try {
            String data = Utils.OBJECT_MAPPER.writeValueAsString(errorResponse);
            return ServerSentEvent.<String>builder().data(data).event("error").build();
        } catch (Exception e) {
            return ServerSentEvent.<String>builder().data("{\"error\":\"Internal error\"}").event("error").build();
        }
    }

    protected JSONRPCResponse<?> handleNonStreamRequest(String body, ServerCallContext context)
            throws JsonProcessingException {
        NonStreamingJSONRPCRequest<?> request = Utils.OBJECT_MAPPER.readValue(body, NonStreamingJSONRPCRequest.class);
        if (request instanceof GetTaskRequest req) {
            return jsonRpcHandler.onGetTask(req, context);
        } else if (request instanceof SendMessageRequest req) {
            return jsonRpcHandler.onMessageSend(req, context);
        } else if (request instanceof CancelTaskRequest req) {
            return jsonRpcHandler.onCancelTask(req, context);
        } else if (request instanceof GetTaskPushNotificationConfigRequest req) {
            return jsonRpcHandler.getPushNotificationConfig(req, context);
        } else if (request instanceof SetTaskPushNotificationConfigRequest req) {
            return jsonRpcHandler.setPushNotificationConfig(req, context);
        } else if (request instanceof ListTaskPushNotificationConfigRequest req) {
            return jsonRpcHandler.listPushNotificationConfig(req, context);
        } else if (request instanceof DeleteTaskPushNotificationConfigRequest req) {
            return jsonRpcHandler.deletePushNotificationConfig(req, context);
        } else {
            return generateErrorResponse(request, new UnsupportedOperationError());
        }
    }

    private ServerCallContext buildServerCallContext(HttpServletRequest httpRequest) {
        Map<String, Object> state = new HashMap<>();
        Map<String, String> headers = new HashMap<>();
        state.put(ContextKeys.HEADERS_KEY, headers);
        Enumeration<String> headerNames = httpRequest.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = httpRequest.getHeader(headerName);
            headers.put(headerName, headerValue);
        }
        return new ServerCallContext(null, state, new HashSet<>());
    }

    private JSONRPCErrorResponse generateErrorResponse(JSONRPCRequest<?> request, JSONRPCError error) {
        return new JSONRPCErrorResponse(request.getId(), error);
    }

}
