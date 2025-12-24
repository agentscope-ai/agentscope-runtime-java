package io.agentscope.runtime.protocol.a2a.controller;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
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
import io.vertx.ext.web.RoutingContext;
import jakarta.annotation.PostConstruct;
import org.apache.rocketmq.a2a.common.RocketMQRequest;
import org.apache.rocketmq.a2a.common.RocketMQResponse;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.SessionCredentialsProvider;
import org.apache.rocketmq.client.apis.StaticSessionCredentialsProvider;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.consumer.FilterExpression;
import org.apache.rocketmq.client.apis.consumer.FilterExpressionType;
import org.apache.rocketmq.client.apis.consumer.PushConsumer;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.apis.producer.ProducerBuilder;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.apache.rocketmq.shaded.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Flux;
import static io.a2a.util.Utils.OBJECT_MAPPER;

@Controller
@RequestMapping("/a2a-rocketmq")
public class A2aRocketMQController extends A2aController {
    private static Logger logger = Logger.getLogger(A2aRocketMQController.class.getName());
    private static final String ROCKETMQ_ENDPOINT = System.getProperty("rocketMQEndpoint", "");
    private static final String ROCKETMQ_NAMESPACE = System.getProperty("rocketMQNamespace", "");
    private static final String BIZ_TOPIC = System.getProperty("bizTopic", "");
    private static final String BIZ_CONSUMER_GROUP = System.getProperty("bizConsumerGroup", "");
    private static final String ACCESS_KEY = System.getProperty("rocketMQAK", "");
    private static final String SECRET_KEY = System.getProperty("rocketMQSK", "");

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
            producer = buildProducer();
            fluxSseSupport = new FluxSseSupport(producer);
            pushConsumer = buildConsumer();
        } catch (Exception e) {
            logger.info("A2aRocketMQController init error, please check the rocketmq config, e: " + e.getMessage());
        }
    }

    private static class FluxSseSupport {
        private final Producer producer;

        private FluxSseSupport(Producer producer) {
            this.producer = producer;
        }

        public void subscribeObjectRocketmq(Flux<Object> multi, RoutingContext rc,  String WorkAgentResponseTopic, String liteTopic, String msgId, CompletableFuture<Boolean> completableFuture) {
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
            writeRocketmq(map, rc, WorkAgentResponseTopic, liteTopic, msgId, completableFuture);
        }

        public void writeRocketmq(Flux<Buffer> flux, RoutingContext rc, String WorkAgentResponseTopic, String liteTopic, String msgId, CompletableFuture<Boolean> completableFuture) {
            flux.subscribe(
                event -> {
                    // onNext: 接收到事件
                    try {
                        SendReceipt send = producer.send(buildMessage(WorkAgentResponseTopic, liteTopic, new RocketMQResponse(liteTopic, null, event.toString(), msgId, true, false)));
                        logger.info("rocketmq send stream success: " + send.getMessageId() + " time " +  System.currentTimeMillis());
                    } catch (ClientException error) {
                        logger.info("rocketmq send stream error: " + error.getMessage());
                    }
                },
                error -> {
                    logger.info("send stream error: " + error.getMessage());
                    completableFuture.complete(false);
                },
                () -> {
                    logger.info("send stream completed.");
                    try {
                        SendReceipt send = producer.send(buildMessage(WorkAgentResponseTopic, liteTopic, new RocketMQResponse(liteTopic, null, null, msgId, true, true)));
                        logger.info("rocketmq send stream success: " + send.getMessageId() + " time " + System.currentTimeMillis());
                    } catch (ClientException e) {
                        logger.info("rocketmq send stream error: " + e.getMessage() );
                    }
                    completableFuture.complete(true);
                }
            );
        }
    }

    private Producer buildProducer() throws ClientException {
        final ClientServiceProvider provider = ClientServiceProvider.loadService();
        SessionCredentialsProvider sessionCredentialsProvider = new StaticSessionCredentialsProvider(ACCESS_KEY, SECRET_KEY);
        ClientConfiguration clientConfiguration = ClientConfiguration.newBuilder()
            .setEndpoints(ROCKETMQ_ENDPOINT)
            .setNamespace(ROCKETMQ_NAMESPACE)
            .setCredentialProvider(sessionCredentialsProvider)
            .setRequestTimeout(Duration.ofSeconds(15))
            .build();
        final ProducerBuilder builder = provider.newProducerBuilder().setClientConfiguration(clientConfiguration);
        return builder.build();
    }

    private PushConsumer buildConsumer() throws ClientException {
        final ClientServiceProvider provider = ClientServiceProvider.loadService();
        SessionCredentialsProvider sessionCredentialsProvider = new StaticSessionCredentialsProvider(ACCESS_KEY, SECRET_KEY);
        ClientConfiguration clientConfiguration = ClientConfiguration.newBuilder()
            .setEndpoints(ROCKETMQ_ENDPOINT)
            .setNamespace(ROCKETMQ_NAMESPACE)
            .setCredentialProvider(sessionCredentialsProvider)
            .build();
        String tag = "*";
        FilterExpression filterExpression = new FilterExpression(tag, FilterExpressionType.TAG);
        return provider.newPushConsumerBuilder()
            .setClientConfiguration(clientConfiguration)
            .setConsumerGroup(BIZ_CONSUMER_GROUP)
            .setSubscriptionExpressions(Collections.singletonMap(BIZ_TOPIC, filterExpression))
            .setMessageListener(messageView -> {
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
                                fluxSseSupport.subscribeObjectRocketmq(serverSentEventFlux.map(i -> (Object)i), null, request.getWorkAgentResponseTopic(), request.getLiteTopic(), messageView.getMessageId().toString(), completableFuture);
                            });
                            Boolean streamResult = completableFuture.get(15, TimeUnit.MINUTES);
                            if (null != streamResult && streamResult) {
                                return ConsumeResult.SUCCESS;
                            } else {
                                return ConsumeResult.FAILURE;
                            }
                        } else {
                            nonStreamingResponse = handleNonStreamRequest(body, null);
                        }
                    } catch (JsonProcessingException e) {
                        logger.info("JSON parsing error: " + e.getMessage());
                        error = new JSONRPCErrorResponse(null, new JSONParseError());
                    } finally {
                        if (!streaming) {
                            RocketMQResponse response = null;
                            if (error != null) {
                                response = new RocketMQResponse(request.getLiteTopic(), null, JSON.toJSONString(error), messageView.getMessageId().toString(), false, true);
                            } else {
                                response = new RocketMQResponse(request.getLiteTopic(), null, toJsonString(nonStreamingResponse), messageView.getMessageId().toString(), false, true);
                            }
                            SendReceipt send = this.producer.send(buildMessage(request.getWorkAgentResponseTopic(), request.getLiteTopic(), response));
                            logger.info("send response success:" + send.getMessageId() + ", time: " + System.currentTimeMillis() );
                        }
                    }
                } catch (Exception e) {
                    logger.info("error " + e.getMessage());
                    return ConsumeResult.FAILURE;
                }
                return ConsumeResult.SUCCESS;
            }).build();
    }

    private static Message buildMessage(String topic, String liteTopic, RocketMQResponse response) {
        if (StringUtils.isEmpty(topic) || StringUtils.isEmpty(liteTopic)) {
            logger.info("buildMessage param error topic: " + topic + ", liteTopic: " + liteTopic + ", response: " + response);
            return null;
        }
        String missionJsonStr = JSON.toJSONString(response);
        final ClientServiceProvider provider = ClientServiceProvider.loadService();
        final Message message = provider.newMessageBuilder()
            .setTopic(topic)
            .setBody(missionJsonStr.getBytes(StandardCharsets.UTF_8))
            .setLiteTopic(liteTopic)
            .build();
        return message;
    }

    private static String toJsonString(Object o) {
        try {
            return OBJECT_MAPPER.writeValueAsString(o);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static boolean checkConfigParam() {
        if (StringUtils.isEmpty(ROCKETMQ_ENDPOINT) || StringUtils.isEmpty(BIZ_TOPIC) || StringUtils.isEmpty(BIZ_CONSUMER_GROUP)) {
            if (StringUtils.isEmpty(ROCKETMQ_ENDPOINT)) {
                logger.info("rocketMQEndpoint is empty");
            }
            if (StringUtils.isEmpty(BIZ_TOPIC)) {
                logger.info("bizTopic is empty");
            }
            if (StringUtils.isEmpty(BIZ_CONSUMER_GROUP)) {
                logger.info("bizConsumerGroup is empty");
            }
            return false;
        }
        return true;
    }
}
