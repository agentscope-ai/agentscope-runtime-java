package io.agentscope.runtime.sandbox.manager.model.fs;

public class OssConfig extends FileSystemConfig {
    private String ossEndpoint = "http://oss-cn-hangzhou.aliyuncs.com";
    private String ossAccessKeyId;
    private String ossAccessKeySecret;
    private String ossBucketName;

    public OssConfig(){
        super(FileSystemType.OSS);
    }

    public OssConfig(String ossEndpoint, String ossAccessKeyId, String ossAccessKeySecret, String ossBucketName){
        super(FileSystemType.OSS);
        this.ossEndpoint = ossEndpoint;
        this.ossAccessKeyId = ossAccessKeyId;
        this.ossAccessKeySecret = ossAccessKeySecret;
        this.ossBucketName = ossBucketName;
    }

    public String getOssEndpoint() {
        return ossEndpoint;
    }

    public void setOssEndpoint(String ossEndpoint) {
        this.ossEndpoint = ossEndpoint;
    }

    public String getOssAccessKeyId() {
        return ossAccessKeyId;
    }

    public void setOssAccessKeyId(String ossAccessKeyId) {
        this.ossAccessKeyId = ossAccessKeyId;
    }

    public String getOssAccessKeySecret() {
        return ossAccessKeySecret;
    }

    public void setOssAccessKeySecret(String ossAccessKeySecret) {
        this.ossAccessKeySecret = ossAccessKeySecret;
    }

    public String getOssBucketName() {
        return ossBucketName;
    }

    public void setOssBucketName(String ossBucketName) {
        this.ossBucketName = ossBucketName;
    }
}
