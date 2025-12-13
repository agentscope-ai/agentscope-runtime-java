package io.agentscope.runtime.sandbox.manager.client;

import io.agentscope.runtime.sandbox.manager.client.config.FcClientConfig;
import io.agentscope.runtime.sandbox.manager.model.fs.VolumeBinding;

import java.util.List;
import java.util.Map;

public class FcClient extends BaseClient {
    public FcClient(FcClientConfig fcClientConfig) {
        super();
    }

    @Override
    public boolean connect() {
        return false;
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

    }

    @Override
    public void removeContainer(String containerId) {

    }

    @Override
    public String getContainerStatus(String containerId) {
        return "";
    }

    @Override
    public boolean isConnected() {
        return false;
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