package runtime.domain.tools.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.SandboxType;
import io.agentscope.runtime.sandbox.tools.SandboxTools;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 沙箱测试基类，自动管理沙箱的生命周期
 */
public abstract class BaseSandboxTest {
    

    private static final Set<SandboxType> usedSandboxes = ConcurrentHashMap.newKeySet();
    
    // 获取共享的SandboxManager实例
    private SandboxManager getSandboxManager() {
        try {
            SandboxTools sandboxTools = new SandboxTools();
            return sandboxTools.getSandboxManager();
        } catch (Exception e) {
            System.err.println("无法获取共享SandboxManager实例: " + e.getMessage());
            return new SandboxManager();
        }
    }
    
    @BeforeEach
    void setUpSandboxManager() {
        System.out.println("初始化测试环境，当前已使用的沙箱: " + usedSandboxes);
    }
    
    @AfterEach
    void tearDownSandboxes() {
        if (!usedSandboxes.isEmpty()) {
            System.out.println("开始清理沙箱容器...");
            System.out.println("需要清理的沙箱类型: " + usedSandboxes);
            
            SandboxManager sandboxManager = getSandboxManager();
            for (SandboxType sandboxType : usedSandboxes) {
                try {
                    System.out.println("正在停止并删除 " + sandboxType + " 沙箱...");
                    sandboxManager.stopAndRemoveSandbox(sandboxType);
                    System.out.println(sandboxType + " 沙箱已成功删除");
                } catch (Exception e) {
                    System.err.println("删除 " + sandboxType + " 沙箱时出错: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            usedSandboxes.clear();
        } else {
            System.out.println("没有需要清理的沙箱容器");
        }
    }
    
    /**
     * 记录使用的沙箱类型，用于测试后清理
     * @param sandboxType 沙箱类型
     */
    protected void recordSandboxUsage(SandboxType sandboxType) {
        usedSandboxes.add(sandboxType);
    }
    
}
