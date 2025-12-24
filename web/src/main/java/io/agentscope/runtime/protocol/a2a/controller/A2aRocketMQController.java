package io.agentscope.runtime.protocol.a2a.controller;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.logging.Logger;
import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.a2a.spec.AgentCard;
import io.a2a.spec.JSONParseError;
import io.a2a.spec.JSONRPCErrorResponse;
import io.a2a.spec.JSONRPCResponse;
import io.agentscope.runtime.engine.Runner;
import io.agentscope.runtime.protocol.ProtocolConfig;
import io.vertx.core.buffer.Buffer;
import jakarta.annotation.PostConstruct;
import org.apache.rocketmq.a2a.common.RocketMQRequest;
import org.apache.rocketmq.a2a.common.RocketMQResponse;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.consumer.MessageListener;
import org.apache.rocketmq.client.apis.consumer.PushConsumer;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.shaded.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Flux;
import static io.agentscope.runtime.protocol.a2a.RocketMQUtils.buildConsumer;
import static io.agentscope.runtime.protocol.a2a.RocketMQUtils.buildMessage;
import static io.agentscope.runtime.protocol.a2a.RocketMQUtils.buildProducer;
import static io.agentscope.runtime.protocol.a2a.RocketMQUtils.checkConfigParam;
import static io.agentscope.runtime.protocol.a2a.RocketMQUtils.toJsonString;

@Controller
@RequestMapping("/a2a-rocketmq")
public class A2aRocketMQController extends A2aController {
    private static Logger logger = Logger.getLogger(A2aRocketMQController.class.getName());
    private Producer producer;
    private PushConsumer pushConsumer;
    private FluxSseSupport fluxSseSupport;
    Executor executor = Executors.newFixedThreadPool(6);

    public A2aRocketMQController(Runner runner, AgentCard agentCard, ObjectProvider<ProtocolConfig> protocolConfigs) {
        super(runner, agentCard, protocolConfigs);
    }

    @PostConstruct
    public void init() {
        try {
            if (!checkConfigParam()) {
                logger.info("checkConfigParam rocketmq config param is not ready, ignore start rocketmq server!!!");
                return;
            }
            this.producer = buildProducer();
            fluxSseSupport = new FluxSseSupport(this.producer);
            pushConsumer = buildConsumer(buildMessageListener());
        } catch (Exception e) {
            logger.info("A2aRocketMQController init error, please check the rocketmq config, e: " + e.getMessage());
        }
    }

    private MessageListener buildMessageListener() {
       return messageView -> {
           try {
               byte[] result = new byte[messageView.getBody().remaining()];
               messageView.getBody().get(result);
               RocketMQRequest request = JSON.parseObject(new String(result, StandardCharsets.UTF_8), RocketMQRequest.class);
               String body = request.getRequestBody();
               JSONRPCResponse<?> nonStreamingResponse = null;
               JSONRPCErrorResponse error = null;
               boolean streaming = isStreamingRequest(body, null);
               try {
                   if (streaming) {
                       Flux<ServerSentEvent<String>> serverSentEventFlux = handleStreamRequest(body, null);
                       CompletableFuture<Boolean> completableFuture = new CompletableFuture();
                       executor.execute(() -> {
                           fluxSseSupport.subscribeObjectRocketmq(serverSentEventFlux.map(i -> (Object)i), request.getWorkAgentResponseTopic(), request.getLiteTopic(), messageView.getMessageId().toString(), completableFuture);
                       });
                       Boolean streamResult = completableFuture.get(15, TimeUnit.MINUTES);
                       return Boolean.TRUE.equals(streamResult) ? ConsumeResult.SUCCESS : ConsumeResult.FAILURE;
                   } else {
                       nonStreamingResponse = handleNonStreamRequest(body, null);
                   }
               } catch (JsonProcessingException e) {
                   logger.info("JSON parsing error: " + e.getMessage());
                   error = new JSONRPCErrorResponse(null, new JSONParseError());
               } finally {
                   if (!streaming) {
                       String responseBody = (error != null) ? JSON.toJSONString(error) : toJsonString(nonStreamingResponse);
                       producer.send(buildMessage(request.getWorkAgentResponseTopic(), request.getLiteTopic(), new RocketMQResponse(request.getLiteTopic(), null, responseBody, messageView.getMessageId().toString(), false, true)));
                   }
               }
           } catch (Exception e) {
               logger.info("error " + e.getMessage());
               return ConsumeResult.FAILURE;
           }
           return ConsumeResult.SUCCESS;
       };
    }

    private static class FluxSseSupport {
        private final Producer producer;

        private FluxSseSupport(Producer producer) {
            this.producer = producer;
        }

        public void subscribeObjectRocketmq(Flux<Object> multi, String WorkAgentResponseTopic, String liteTopic, String msgId, CompletableFuture<Boolean> completableFuture) {
            AtomicLong count = new AtomicLong();
            Flux<Buffer> map = multi.map(new Function<Object, Buffer>() {
                @Override
                public Buffer apply(Object o) {
                    if (o instanceof ServerSentEvent ev) {
                        String id = !StringUtils.isEmpty(ev.id()) ? ev.id() : String.valueOf(count.getAndIncrement());
                        return Buffer.buffer("data: " + ev.data() + "\nid: " + id + "\n\n");
                    }
                    return Buffer.buffer("data: " + toJsonString(o) + "\nid: " + count.getAndIncrement() + "\n\n");
                }
            });
            writeRocketmq(map, WorkAgentResponseTopic, liteTopic, msgId, completableFuture);
        }

        public void writeRocketmq(Flux<Buffer> flux, String WorkAgentResponseTopic, String liteTopic, String msgId, CompletableFuture<Boolean> completableFuture) {
            flux.subscribe(
                event -> {
                    try {
                       producer.send(buildMessage(WorkAgentResponseTopic, liteTopic, new RocketMQResponse(liteTopic, null, event.toString(), msgId, true, false)));
                    } catch (ClientException error) {
                        logger.info("writeRocketmq send stream error: " + error.getMessage());
                    }
                },
                error -> {
                    logger.info("writeRocketmq send stream error: " + error.getMessage());
                    completableFuture.complete(false);
                },
                () -> {
                    try {
                       producer.send(buildMessage(WorkAgentResponseTopic, liteTopic, new RocketMQResponse(liteTopic, null, null, msgId, true, true)));
                    } catch (ClientException e) {
                        logger.info("writeRocketmq send stream error: " + e.getMessage() );
                    }
                    completableFuture.complete(true);
                }
            );
        }
    }

}
