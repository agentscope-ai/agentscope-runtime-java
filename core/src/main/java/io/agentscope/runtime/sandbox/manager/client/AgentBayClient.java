package io.agentscope.runtime.sandbox.manager.client;

import com.aliyun.agentbay.AgentBay;
import com.aliyun.agentbay.exception.AgentBayException;
import com.aliyun.agentbay.model.DeleteResult;
import com.aliyun.agentbay.model.SessionInfoResult;
import com.aliyun.agentbay.model.SessionResult;
import com.aliyun.agentbay.session.CreateSessionParams;
import com.aliyun.agentbay.session.Session;
import io.agentscope.runtime.sandbox.manager.model.fs.VolumeBinding;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class AgentBayClient extends BaseClient{
    Logger logger = Logger.getLogger(AgentBayClient.class.getName());

    private AgentBay agentBay;
    private String apiKey;

    public AgentBayClient(String apiKey){
        this.apiKey = apiKey;
        try{
            agentBay = new AgentBay(apiKey);
        } catch (AgentBayException e) {
            logger.severe("Failed to initialize AgentBay client: " + e.getMessage());
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
                logger.info("AgentBay session created successfully: " + sessionId);
                return new ContainerCreateResult(sessionId);
            } else {
                logger.severe("Failed to create AgentBay session: " + sessionResult.getErrorMessage());
            }
        }
        catch (AgentBayException e){
            logger.severe("Failed to create AgentBay session: " + e.getMessage());
        }
        return createContainer(null, null, null, null, null, null);
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
                logger.warning("AgentBay session not found: " + containerId);
                return;
            }
            DeleteResult deleteResult = agentBay.delete(getResult.getSession(), false);
            if(deleteResult.isSuccess()){
                logger.info("AgentBay session removed successfully: " + containerId);
            } else {
                logger.warning("Failed to remove AgentBay session: " + deleteResult.getErrorMessage());
            }

        } catch (AgentBayException e) {
            logger.severe("Failed to remove AgentBay session: " + e.getMessage());
        }
    }

    public Map<String, Object> getSessionInfo(String sessionId) {
        try{
            SessionResult getResult =  agentBay.get(sessionId);
            if(getResult.isSuccess()){
                Session session = getResult.getSession();
                SessionInfoResult infoResult = session.info();
                if(infoResult.isSuccess()){
                    logger.info("AgentBay session info retrieved successfully: " + sessionId);
                    return Map.of(
                            "sessionId", infoResult.getSessionInfo().getSessionId(),
                            "resourceId", infoResult.getSessionInfo().getResourceId(),
                            "resourceUrl", infoResult.getSessionInfo().getResourceUrl(),
                            "appId", infoResult.getSessionInfo().getAppId(),
                            "resourceType", infoResult.getSessionInfo().getResourceType(),
                            "requestId", infoResult.getRequestId()
                    );
                } else {
                    logger.warning("Failed to get AgentBay session info: " + infoResult.getErrorMessage());
                    return Map.of("error", infoResult.getErrorMessage());
                }
            } else {
                logger.warning("AgentBay session not found: " + sessionId);
            }
        } catch (AgentBayException e) {
            logger.severe("Failed to get AgentBay session info: " + e.getMessage());
        }
        return null;
    }

    @Override
    public String getContainerStatus(String containerId) {
        return "";
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
