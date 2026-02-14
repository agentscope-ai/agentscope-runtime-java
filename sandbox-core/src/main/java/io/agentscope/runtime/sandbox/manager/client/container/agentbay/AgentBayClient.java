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

package io.agentscope.runtime.sandbox.manager.client.container.agentbay;

import com.aliyun.agentbay.AgentBay;
import com.aliyun.agentbay.exception.AgentBayException;
import com.aliyun.agentbay.model.DeleteResult;
import com.aliyun.agentbay.model.SessionInfoResult;
import com.aliyun.agentbay.model.SessionResult;
import com.aliyun.agentbay.session.CreateSessionParams;
import com.aliyun.agentbay.session.Session;
import io.agentscope.runtime.sandbox.manager.client.container.BaseClient;
import io.agentscope.runtime.sandbox.manager.client.container.ContainerCreateResult;
import io.agentscope.runtime.sandbox.manager.model.fs.VolumeBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class AgentBayClient extends BaseClient {
    private static final Logger logger = LoggerFactory.getLogger(AgentBayClient.class);

    private AgentBay agentBay;
    private final String apiKey;

    public AgentBayClient(String apiKey){
        this.apiKey = apiKey;
        try{
            agentBay = new AgentBay(apiKey);
        } catch (AgentBayException e) {
            logger.error("Failed to initialize AgentBay client: {}", e.getMessage());
        }
    }

    @Override
    public boolean connect() {
        return false;
    }

    public ContainerCreateResult createContainer(String imageId, Map<String, String> labels) {
        CreateSessionParams params = new CreateSessionParams();
        params.setImageId(imageId);
        params.setLabels(labels);

        try{
            SessionResult sessionResult = agentBay.create(params);
            if(sessionResult.isSuccess()){
                String sessionId = sessionResult.getSessionId();
                logger.info("AgentBay session created successfully: {}", sessionId);
                return new ContainerCreateResult(sessionId);
            } else {
                logger.error("Failed to create AgentBay session: {}", sessionResult.getErrorMessage());
            }
        }
        catch (AgentBayException e){
            logger.error("Failed to create AgentBay session: {}", e.getMessage());
        }
        return new ContainerCreateResult(null, null, null);
    }

    @Override
    public ContainerCreateResult createContainer(String containerName, String imageName, List<String> ports, List<VolumeBinding> volumeBindings, Map<String, String> environment, Map<String, Object> runtimeConfig) {
        return null;
    }

    @Override
    public void startContainer(String containerId) {

    }

    @Override
    public void stopContainer(String containerId) {
        removeContainer(containerId);
    }

    @Override
    public void removeContainer(String containerId) {
        try{
            SessionResult getResult =  agentBay.get(containerId);
            if(!getResult.isSuccess()){
                logger.warn("AgentBay session not found: {}", containerId);
                return;
            }
            DeleteResult deleteResult = agentBay.delete(getResult.getSession(), false);
            if(deleteResult.isSuccess()){
                logger.info("AgentBay session removed successfully: {}", containerId);
            } else {
                logger.warn("Failed to remove AgentBay session: {}", deleteResult.getErrorMessage());
            }

        } catch (AgentBayException e) {
            logger.error("Failed to remove AgentBay session: {}", e.getMessage());
        }
    }

    public Map<String, Object> getSessionInfo(String sessionId) {
        try{
            SessionResult getResult =  agentBay.get(sessionId);
            if(getResult.isSuccess()){
                Session session = getResult.getSession();
                SessionInfoResult infoResult = session.info();
                if(infoResult.isSuccess()){
                    logger.info("AgentBay session info retrieved successfully: {}", sessionId);
                    return Map.of(
                            "sessionId", infoResult.getSessionInfo().getSessionId(),
                            "resourceId", infoResult.getSessionInfo().getResourceId(),
                            "resourceUrl", infoResult.getSessionInfo().getResourceUrl(),
                            "appId", infoResult.getSessionInfo().getAppId(),
                            "resourceType", infoResult.getSessionInfo().getResourceType(),
                            "requestId", infoResult.getRequestId()
                    );
                } else {
                    logger.warn("Failed to get AgentBay session info: {}", infoResult.getErrorMessage());
                    return Map.of("error", infoResult.getErrorMessage());
                }
            } else {
                logger.warn("AgentBay session not found: {}", sessionId);
            }
        } catch (AgentBayException e) {
            logger.error("Failed to get AgentBay session info: {}", e.getMessage());
        }
        return null;
    }

    public Session getSession(String sessionId){
        try{
            SessionResult getResult =  agentBay.get(sessionId);
            if(getResult.isSuccess()){
                return getResult.getSession();
            } else {
                logger.error("AgentBay session not found: {}", sessionId);
            }
        } catch (AgentBayException e) {
            logger.error("Failed to get AgentBay session: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public String getContainerStatus(String containerId) {
        return "running";
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public boolean imageExists(String imageName) {
        return false;
    }

    @Override
    public boolean inspectContainer(String containerIdOrName) {
        return false;
    }

    @Override
    public boolean pullImage(String imageName) {
        return false;
    }
}
