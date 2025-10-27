package io.agentscope.runtime.sandbox.manager.client.config;

import java.util.List;

public class AgentRunClientConfig extends BaseClientConfig {

    private String agentRunAccessKeyId;
    private String agentRunAccessKeySecret;
    private String agentRunAccountId;
    private String agentRunRegionId = "cn-hangzhou";
    private double agentRunCpu = 2.0;
    private int agentRunMemory = 2048;
    private String agentRunVpcId;
    private List<String> agentRunVswitchIds;
    private String agentRunSecurityGroupId;
    private String agentRunPrefix = "agentscope-sandbox_";
    private String agentrunLogProject;
    private String agentrunLogStore;

    public String getAgentRunAccessKeyId() {
        return agentRunAccessKeyId;
    }

    public void setAgentRunAccessKeyId(String agentRunAccessKeyId) {
        this.agentRunAccessKeyId = agentRunAccessKeyId;
    }

    public String getAgentRunAccessKeySecret() {
        return agentRunAccessKeySecret;
    }

    public void setAgentRunAccessKeySecret(String agentRunAccessKeySecret) {
        this.agentRunAccessKeySecret = agentRunAccessKeySecret;
    }

    public String getAgentRunAccountId() {
        return agentRunAccountId;
    }

    public void setAgentRunAccountId(String agentRunAccountId) {
        this.agentRunAccountId = agentRunAccountId;
    }

    public String getAgentRunRegionId() {
        return agentRunRegionId;
    }

    public void setAgentRunRegionId(String agentRunRegionId) {
        this.agentRunRegionId = agentRunRegionId;
    }

    public double getAgentRunCpu() {
        return agentRunCpu;
    }

    public void setAgentRunCpu(double agentRunCpu) {
        this.agentRunCpu = agentRunCpu;
    }

    public int getAgentRunMemory() {
        return agentRunMemory;
    }

    public void setAgentRunMemory(int agentRunMemory) {
        this.agentRunMemory = agentRunMemory;
    }

    public String getAgentRunVpcId() {
        return agentRunVpcId;
    }

    public void setAgentRunVpcId(String agentRunVpcId) {
        this.agentRunVpcId = agentRunVpcId;
    }

    public List<String> getAgentRunVswitchIds() {
        return agentRunVswitchIds;
    }

    public void setAgentRunVswitchIds(List<String> agentRunVswitchIds) {
        this.agentRunVswitchIds = agentRunVswitchIds;
    }

    public String getAgentRunSecurityGroupId() {
        return agentRunSecurityGroupId;
    }

    public void setAgentRunSecurityGroupId(String agentRunSecurityGroupId) {
        this.agentRunSecurityGroupId = agentRunSecurityGroupId;
    }

    public String getAgentRunPrefix() {
        return agentRunPrefix;
    }

    public void setAgentRunPrefix(String agentRunPrefix) {
        this.agentRunPrefix = agentRunPrefix;
    }

    public String getAgentrunLogProject() {
        return agentrunLogProject;
    }

    public void setAgentrunLogProject(String agentrunLogProject) {
        this.agentrunLogProject = agentrunLogProject;
    }

    public String getAgentrunLogStore() {
        return agentrunLogStore;
    }

    public void setAgentrunLogStore(String agentrunLogStore) {
        this.agentrunLogStore = agentrunLogStore;
    }

}

