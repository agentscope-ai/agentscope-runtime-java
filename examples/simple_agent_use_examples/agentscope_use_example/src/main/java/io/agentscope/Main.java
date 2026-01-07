package io.agentscope;


import io.agentscope.runtime.sandbox.box.BaseSandbox;
import io.agentscope.runtime.sandbox.manager.ManagerConfig;
import io.agentscope.runtime.sandbox.manager.SandboxService;
import io.agentscope.runtime.sandbox.manager.client.container.BaseClientStarter;
import io.agentscope.runtime.sandbox.manager.client.container.docker.DockerClientStarter;

public class Main {
    public static void main(String[] args) {
//        创建并启动沙箱服务
        BaseClientStarter clientConfig = DockerClientStarter.builder().build();
        ManagerConfig managerConfig = ManagerConfig.builder()
                .clientConfig(clientConfig)
                .baseUrl("http://0.0.0.0:10001")
                .build();
        SandboxService sandboxService = new SandboxService(managerConfig);
        sandboxService.start();

        try {
//            连接到沙箱，指定需要的沙箱类型
            BaseSandbox sandbox = new BaseSandbox(sandboxService, "userId", "sessionId");
            {
                String pythonResult = sandbox.runIpythonCell("a=1");
                System.out.println("Sandbox execution result: " + pythonResult);
            }
            sandbox.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}