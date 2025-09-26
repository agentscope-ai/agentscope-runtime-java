package io.agentscope.runtime.autoconfig.deployer;

import io.agentscope.runtime.engine.schemas.agent.AgentRequest;
import io.agentscope.runtime.engine.schemas.agent.Event;
import reactor.core.publisher.Flux;

import java.util.function.Function;

public class DeployManager {

    public void deployStreaming(Function<AgentRequest, Flux<Event>> queryFunction, String endpointName) {

    }
}
