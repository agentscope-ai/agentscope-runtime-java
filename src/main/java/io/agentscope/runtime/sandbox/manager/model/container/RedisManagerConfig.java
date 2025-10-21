package io.agentscope.runtime.sandbox.manager.model.container;

public class RedisManagerConfig {
    private String redisServer = "localhost";
    private Integer redisPort = 6379;
    private Integer redisDb = 0;
    private String redisUser;
    private String redisPassword;
    private String redisPortKey = "_runtime_sandbox_container_occupied_ports";
    private String redisContainerPoolKey = "_runtime_sandbox_container_container_pool";

    public String getRedisServer() {
        return redisServer;
    }

    public void setRedisServer(String redisServer) {
        this.redisServer = redisServer;
    }

    public Integer getRedisPort() {
        return redisPort;
    }

    public void setRedisPort(Integer redisPort) {
        this.redisPort = redisPort;
    }

    public Integer getRedisDb() {
        return redisDb;
    }

    public void setRedisDb(Integer redisDb) {
        this.redisDb = redisDb;
    }

    public String getRedisUser() {
        return redisUser;
    }

    public void setRedisUser(String redisUser) {
        this.redisUser = redisUser;
    }

    public String getRedisPassword() {
        return redisPassword;
    }

    public void setRedisPassword(String redisPassword) {
        this.redisPassword = redisPassword;
    }

    public String getRedisPortKey() {
        return redisPortKey;
    }

    public void setRedisPortKey(String redisPortKey) {
        this.redisPortKey = redisPortKey;
    }

    public String getRedisContainerPoolKey() {
        return redisContainerPoolKey;
    }

    public void setRedisContainerPoolKey(String redisContainerPoolKey) {
        this.redisContainerPoolKey = redisContainerPoolKey;
    }
}


