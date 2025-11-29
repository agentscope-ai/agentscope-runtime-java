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

package io.agentscope.maven.plugin.service;

import com.aliyun.bailian20231229.Client;
import com.aliyun.bailian20231229.models.ApplyTempStorageLeaseRequest;
import com.aliyun.bailian20231229.models.HighCodeDeployRequest;
import com.aliyun.bailian20231229.models.HighCodeDeployResponse;
import com.aliyun.bailian20231229.models.ApplyTempStorageLeaseResponse;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import io.agentscope.maven.plugin.config.BuildConfig;
import io.agentscope.maven.plugin.config.ModelStudioConfig;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Java port of the Python ModelstudioDeployManager:
 *
 * <ol>
 *     <li>Apply temporary storage lease from ModelStudio</li>
 *     <li>Upload artifact (JAR/ZIP) to the pre-signed OSS URL with headers</li>
 *     <li>Call HighCodeDeploy API and extract deploy identifier</li>
 * </ol>
 *
 * This class assumes a Bailian(ModelStudio) Java SDK with API surface similar to the
 * Python package {@code alibabacloud_bailian20231229}.
 */
public class ModelStudioDeployService {

    private final Log log;

    public ModelStudioDeployService(Log log) {
        this.log = log;
    }

    public ModelStudioDeployResult deploy(File artifact, BuildConfig buildConfig, ModelStudioConfig config) throws Exception {
        if (artifact == null || !artifact.exists()) {
            throw new IOException("Artifact for ModelStudio deployment is missing");
        }

        // 1. Init client using provided credentials
        Client client = createClient(config);

        // 2. Request temp storage lease & upload artifact to OSS via pre-signed URL
        String tempStorageLeaseId = uploadWithTempStorageLease(client, config, artifact);

        // 3. Trigger HighCodeDeploy
        String deployId = highCodeDeploy(client, config, artifact.getName(), tempStorageLeaseId,
                buildConfig.getEnvironment(), config.getMetadata());

        String consoleUrl = buildConsoleUrl(config);
        log.info("ModelStudio deploy triggered. DeployId=" + deployId + ", consoleUrl=" + consoleUrl);

        return new ModelStudioDeployResult(consoleUrl, deployId, artifact);
    }

    private Client createClient(ModelStudioConfig cfg) throws Exception {
        if (cfg.getAccessKeyId() == null || cfg.getAccessKeySecret() == null) {
            throw new IllegalStateException("ModelStudio accessKeyId/accessKeySecret must be configured");
        }
        String endpoint = cfg.getEndpoint() != null && !cfg.getEndpoint().isEmpty()
                ? cfg.getEndpoint()
                : "bailian." + cfg.getRegion() + ".aliyuncs.com";
        Config config = new Config()
                .setAccessKeyId(cfg.getAccessKeyId())
                .setAccessKeySecret(cfg.getAccessKeySecret())
                .setEndpoint(endpoint);
        return new Client(config);
    }

    /**
     * Apply temporary storage lease and upload artifact with returned pre-signed URL + headers.
     *
     * @return tempStorageLeaseId
     */
    private String uploadWithTempStorageLease(Client client,
                                             ModelStudioConfig cfg,
                                             File artifact) throws Exception {
        ApplyTempStorageLeaseRequest leaseRequest = new ApplyTempStorageLeaseRequest()
                .setFileName(artifact.getName())
                .setSizeInBytes(artifact.length());
        RuntimeOptions runtime = new RuntimeOptions();
        Map<String, String> headers = new HashMap<>();

        String workspaceId = cfg.getWorkspaceId() != null ? cfg.getWorkspaceId() : "default";
        ApplyTempStorageLeaseResponse response =
                client.applyTempStorageLeaseWithOptions(workspaceId, leaseRequest, headers, runtime);

        Map<String, Object> map = response.toMap();
        Object bodyObj = map.get("body");
        if (!(bodyObj instanceof Map)) {
            throw new IllegalStateException("Unexpected response from ApplyTempStorageLease: no body map");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) bodyObj;
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("Data");
        if (data == null) {
            throw new IllegalStateException("ApplyTempStorageLease response missing Data");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> param = (Map<String, Object>) data.get("Param");
        if (param == null) {
            throw new IllegalStateException("ApplyTempStorageLease response missing Data.Param");
        }
        String url = (String) param.get("Url");
        @SuppressWarnings("unchecked")
        Map<String, String> ossHeaders = (Map<String, String>) param.get("Headers");
        String tempStorageLeaseId = (String) data.get("TempStorageLeaseId");

        if (url == null || ossHeaders == null || tempStorageLeaseId == null) {
            throw new IllegalStateException("ApplyTempStorageLease response missing Url/Headers/TempStorageLeaseId");
        }

        // Upload artifact using HTTP PUT
        uploadFileViaHttpPut(url, ossHeaders, artifact);
        log.info("Uploaded artifact to temporary OSS via pre-signed URL");
        return tempStorageLeaseId;
    }

    private void uploadFileViaHttpPut(String urlStr,
                                      Map<String, String> headers,
                                      File artifact) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PUT");
        conn.setDoOutput(true);
        for (Map.Entry<String, String> e : headers.entrySet()) {
            conn.setRequestProperty(e.getKey(), e.getValue());
        }

        try (OutputStream os = conn.getOutputStream();
             FileInputStream fis = new FileInputStream(artifact)) {
            byte[] buf = new byte[8192];
            int read;
            while ((read = fis.read(buf)) != -1) {
                os.write(buf, 0, read);
            }
        }

        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new IOException("Failed to upload artifact to OSS via pre-signed URL, HTTP " + code);
        }
    }

    private String highCodeDeploy(Client client,
                                  ModelStudioConfig cfg,
                                  String filename,
                                  String tempStorageLeaseId,
                                  Map<String, String> environment,
                                  Map<String, String> metadata) throws Exception {
        HighCodeDeployRequest request = new HighCodeDeployRequest()
                .setSourceCodeName(filename)
                .setSourceCodeOssUrl(tempStorageLeaseId)
                .setAgentName(cfg.getDeployName() != null ? cfg.getDeployName() : cfg.getServiceName())
                .setAgentId(cfg.getAgentId())
                .setAgentDesc(cfg.getAgentDescription())
                .setTelemetryEnabled(cfg.isTelemetryEnabled());

        RuntimeOptions runtime = new RuntimeOptions();
        Map<String, String> headers = new HashMap<>();

        String workspaceId = cfg.getWorkspaceId() != null ? cfg.getWorkspaceId() : "default";
        HighCodeDeployResponse response =
                client.highCodeDeployWithOptions(workspaceId, request, headers, runtime);

        return extractDeployIdentifier(response);
    }

    /**
     * Rough Java equivalent of Python _extract_deploy_identifier.
     */
    @SuppressWarnings("unchecked")
    private String extractDeployIdentifier(HighCodeDeployResponse resp) {
        if (resp == null) {
            return "";
        }
        Map<String, Object> map = resp.toMap();
        if (map == null) {
            return "";
        }
        Object bodyObj = map.get("body");
        if (bodyObj instanceof Map) {
            Map<String, Object> body = (Map<String, Object>) bodyObj;
            Object successObj = body.get("success");
            if (successObj instanceof Boolean && !(Boolean) successObj) {
                String code = String.valueOf(body.getOrDefault("errorCode", body.get("code")));
                String msg = String.valueOf(body.getOrDefault("errorMsg", body.get("message")));
                throw new RuntimeException("ModelStudio deploy failed: " + code + " " + msg);
            }
            for (String key : new String[]{"data", "result", "deployId"}) {
                Object v = body.get(key);
                if (v instanceof String && !((String) v).isEmpty()) {
                    return (String) v;
                }
            }
            Object dataObj = body.get("data");
            if (dataObj instanceof Map) {
                Map<String, Object> data = (Map<String, Object>) dataObj;
                for (String key : new String[]{"id", "deployId"}) {
                    Object v = data.get(key);
                    if (v instanceof String && !((String) v).isEmpty()) {
                        return (String) v;
                    }
                }
            }
        }
        return "";
    }

    private String buildConsoleUrl(ModelStudioConfig cfg) {
        String endpoint = cfg.getEndpoint() != null && !cfg.getEndpoint().isEmpty()
                ? cfg.getEndpoint()
                : "bailian." + cfg.getRegion() + ".aliyuncs.com";
        boolean isPre = endpoint.contains("bailian-pre") || endpoint.contains("pre");
        String base = isPre
                ? "https://pre-bailian.console.aliyun.com/?tab=app#"
                : "https://bailian.console.aliyun.com/?tab=app#";
        return base + "/app-center";
    }

    public static class ModelStudioDeployResult {
        private final String endpoint;
        private final String deploymentId;
        private final File uploadedArtifact;

        public ModelStudioDeployResult(String endpoint, String deploymentId, File uploadedArtifact) {
            this.endpoint = endpoint;
            this.deploymentId = deploymentId;
            this.uploadedArtifact = uploadedArtifact;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public String getDeploymentId() {
            return deploymentId;
        }

        public File getUploadedArtifact() {
            return uploadedArtifact;
        }
    }
}

