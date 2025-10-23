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
package io.agentscope.runtime.sandbox.manager.model.fs;

public class OssConfig extends FileSystemConfig {
    private String ossEndpoint;
    private String ossAccessKeyId;
    private String ossAccessKeySecret;
    private String ossBucketName;

    private OssConfig(Builder builder) {
        super(builder);
        this.ossEndpoint = builder.ossEndpoint;
        this.ossAccessKeyId = builder.ossAccessKeyId;
        this.ossAccessKeySecret = builder.ossAccessKeySecret;
        this.ossBucketName = builder.ossBucketName;
    }

    public String getOssEndpoint() {
        return ossEndpoint;
    }

    public String getOssAccessKeyId() {
        return ossAccessKeyId;
    }

    public String getOssAccessKeySecret() {
        return ossAccessKeySecret;
    }

    public String getOssBucketName() {
        return ossBucketName;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends FileSystemConfig.Builder<Builder> {
        private String ossEndpoint = "http://oss-cn-hangzhou.aliyuncs.com";
        private String ossAccessKeyId;
        private String ossAccessKeySecret;
        private String ossBucketName;

        public Builder() {
            super(FileSystemType.OSS);
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder ossEndpoint(String ossEndpoint) {
            this.ossEndpoint = ossEndpoint;
            return this;
        }

        public Builder ossAccessKeyId(String ossAccessKeyId) {
            this.ossAccessKeyId = ossAccessKeyId;
            return this;
        }

        public Builder ossAccessKeySecret(String ossAccessKeySecret) {
            this.ossAccessKeySecret = ossAccessKeySecret;
            return this;
        }

        public Builder ossBucketName(String ossBucketName) {
            this.ossBucketName = ossBucketName;
            return this;
        }

        @Override
        public OssConfig build() {
            return new OssConfig(this);
        }
    }
}
