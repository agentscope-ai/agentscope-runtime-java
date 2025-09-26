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
package io.agentscope.runtime.engine.a2a;

import io.agentscope.runtime.engine.memory.context.ServerCallContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.a2a.spec.CancelTaskRequest;
import io.a2a.spec.DeleteTaskPushNotificationConfigRequest;
import io.a2a.spec.GetTaskPushNotificationConfigRequest;
import io.a2a.spec.GetTaskRequest;
import io.a2a.spec.JSONParseError;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.JSONRPCErrorResponse;
import io.a2a.spec.JSONRPCRequest;
import io.a2a.spec.JSONRPCResponse;
import io.a2a.spec.ListTaskPushNotificationConfigRequest;
import io.a2a.spec.NonStreamingJSONRPCRequest;
import io.a2a.spec.SendMessageRequest;
import io.a2a.spec.SendStreamingMessageRequest;
import io.a2a.spec.SetTaskPushNotificationConfigRequest;
import io.a2a.spec.StreamingJSONRPCRequest;
import io.a2a.spec.TaskResubscriptionRequest;
import io.a2a.spec.UnsupportedOperationError;
import io.a2a.util.Utils;
import jakarta.servlet.http.HttpServletRequest;
import org.reactivestreams.FlowAdapters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Flow;

@RestController
@RequestMapping("/a2a")
public class A2aController {

    private static final Logger LOGGER = LoggerFactory.getLogger(A2aController.class);

    private final JSONRPCHandler jsonRpcHandler;

    public A2aController() {
        this.jsonRpcHandler = AgentHandlerConfiguration.jsonrpcHandler();
    }

    @PostMapping(value = "/", consumes = MediaType.APPLICATION_JSON_VALUE, produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE})
    @ResponseBody
    public Object handleRequest(@RequestBody String body, HttpServletRequest httpRequest) {
        ServerCallContext context = buildServerCallContext(httpRequest);
        boolean streaming = isStreamingRequest(body);
        Object result = null;
        try {
            if (streaming) {
                result = handleStreamRequest(body);
                LOGGER.info("Handling streaming request, returning SSE Flux");
            } else {
                result = handleNonStreamRequest(body);
                LOGGER.info("Handling non-streaming request, returning JSON response");
            }
        }
        catch (JsonProcessingException e) {
            LOGGER.error("JSON parsing error: {}", e.getMessage());
            result = new JSONRPCErrorResponse(null, new JSONParseError());
        }
        return result;
    }

    private static boolean isStreamingRequest(String requestBody) {
        try {
            JsonNode node = Utils.OBJECT_MAPPER.readTree(requestBody);
            JsonNode method = node != null ? node.get("method") : null;
            return method != null && (SendStreamingMessageRequest.METHOD.equals(method.asText())
                    || TaskResubscriptionRequest.METHOD.equals(method.asText()));
        }
        catch (Exception e) {
            return false;
        }
    }

    private Flux<ServerSentEvent<String>> handleStreamRequest(String body) throws JsonProcessingException {
        StreamingJSONRPCRequest<?> request = Utils.OBJECT_MAPPER.readValue(body, StreamingJSONRPCRequest.class);
        Flow.Publisher<? extends JSONRPCResponse<?>> publisher;
        if (request instanceof SendStreamingMessageRequest req) {
            publisher = jsonRpcHandler.onMessageSendStream(req);
        }
        else if (request instanceof TaskResubscriptionRequest req) {
            publisher = jsonRpcHandler.onResubscribeToTask(req);
        }
        else {
            return Flux.just(createErrorSSE(generateErrorResponse(request, new UnsupportedOperationError())));
        }

        return Flux.from(FlowAdapters.toPublisher(publisher))
                .map(this::convertToSSE)
                .delaySubscription(Duration.ofMillis(10));
    }

    private ServerSentEvent<String> convertToSSE(JSONRPCResponse<?> response) {
        try {
            String data = Utils.OBJECT_MAPPER.writeValueAsString(response);
            return ServerSentEvent.<String>builder()
                    .data(data)
                    .id(response.getId() != null ? response.getId().toString() : null)
                    .event("jsonrpc")
                    .build();
        } catch (Exception e) {
            LOGGER.error("Error converting response to SSE: {}", e.getMessage());
            return ServerSentEvent.<String>builder()
                    .data("{\"error\":\"Internal conversion error\"}")
                    .event("error")
                    .build();
        }
    }

    private ServerSentEvent<String> createErrorSSE(JSONRPCResponse<?> errorResponse) {
        try {
            String data = Utils.OBJECT_MAPPER.writeValueAsString(errorResponse);
            return ServerSentEvent.<String>builder()
                    .data(data)
                    .event("error")
                    .build();
        } catch (Exception e) {
            return ServerSentEvent.<String>builder()
                    .data("{\"error\":\"Internal error\"}")
                    .event("error")
                    .build();
        }
    }

    private JSONRPCResponse<?> handleNonStreamRequest(String body) throws JsonProcessingException {
        NonStreamingJSONRPCRequest<?> request = Utils.OBJECT_MAPPER.readValue(body, NonStreamingJSONRPCRequest.class);
        if (request instanceof GetTaskRequest req) {
            return jsonRpcHandler.onGetTask(req);
        }
        else if (request instanceof SendMessageRequest req) {
            return jsonRpcHandler.onMessageSend(req);
        }
        else if (request instanceof CancelTaskRequest req) {
            return jsonRpcHandler.onCancelTask(req);
        }
        else if (request instanceof GetTaskPushNotificationConfigRequest req) {
            return jsonRpcHandler.getPushNotificationConfig(req);
        }
        else if (request instanceof SetTaskPushNotificationConfigRequest req) {
            return jsonRpcHandler.setPushNotificationConfig(req);
        }
        else if (request instanceof ListTaskPushNotificationConfigRequest req) {
            return jsonRpcHandler.listPushNotificationConfig(req);
        }
        else if (request instanceof DeleteTaskPushNotificationConfigRequest req) {
            return jsonRpcHandler.deletePushNotificationConfig(req);
        }
        else {
            return generateErrorResponse(request, new UnsupportedOperationError());
        }
    }

    private static ServerCallContext buildServerCallContext(HttpServletRequest httpRequest) {
        Map<String, Object> state = new HashMap<>();
        Enumeration<String> headerNames = httpRequest.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = httpRequest.getHeader(headerName);
            state.put(headerName, headerValue);
        }
        return new ServerCallContext(null, state);
    }

    private static JSONRPCErrorResponse generateErrorResponse(JSONRPCRequest<?> request, JSONRPCError error) {
        return new JSONRPCErrorResponse(request.getId(), error);
    }

}
