package io.agentscope.runtime.protocol.a2a;

import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.rocketmq.a2a.common.RocketMQResponse;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.shaded.commons.lang3.StringUtils;

import static io.a2a.util.Utils.OBJECT_MAPPER;

public class RocketMQUtils {
    private static Logger logger = Logger.getLogger(RocketMQUtils.class.getName());

    public static final String ROCKETMQ_ENDPOINT = System.getProperty("rocketMQEndpoint", "");
    public static final String ROCKETMQ_NAMESPACE = System.getProperty("rocketMQNamespace", "");
    public static final String BIZ_TOPIC = System.getProperty("bizTopic", "");
    public static final String BIZ_CONSUMER_GROUP = System.getProperty("bizConsumerGroup", "");
    public static final String ACCESS_KEY = System.getProperty("rocketMQAK", "");
    public static final String SECRET_KEY = System.getProperty("rocketMQSK", "");

    public static Message buildMessage(String topic, String liteTopic, RocketMQResponse response) {
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

    public static String toJsonString(Object o) {
        try {
            return OBJECT_MAPPER.writeValueAsString(o);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static boolean checkConfigParam() {
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
