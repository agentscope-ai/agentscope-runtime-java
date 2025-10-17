package runtime.domain.tools.service.sandbox.training;

import io.agentscope.runtime.engine.Runner;
import io.agentscope.runtime.sandbox.manager.client.config.KubernetesClientConfig;
import io.agentscope.runtime.sandbox.manager.model.SandboxType;
import io.agentscope.runtime.sandbox.tools.TrainingSandboxTools;
import org.junit.jupiter.api.Test;

public class AppWorldSandboxTest {
    @Test
    public void test(){
        Runner runner = new Runner();
        KubernetesClientConfig kubernetesClientConfig = new KubernetesClientConfig("/Users/xht/Downloads/agentscope-runtime-java/kubeconfig.txt");
        runner.registerClientConfig(kubernetesClientConfig);
        TrainingSandboxTools tools = new TrainingSandboxTools();
        String profiles = tools.getEnvProfiles(SandboxType.TRAINING,"appworld","train",null,"","");
        System.out.println(profiles);
    }
}
