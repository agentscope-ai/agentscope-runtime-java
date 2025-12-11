# 沙盒故障排除
如果您在使用浏览器模块时遇到任何问题，以下是一些故障排查步骤：

## Docker 连接错误

如果您遇到以下错误：

```bash
警告: Failed to connect to Docker using configured host (localhost:2375): com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.HttpHostConnectException: Connect to http://localhost:2375 [localhost/127.0.0.1, localhost/0:0:0:0:0:0:0:1] failed: Connection refused
12月 10, 2025 2:02:43 下午 io.agentscope.runtime.sandbox.manager.client.DockerClient openDockerClient
信息: Falling back to default Docker configuration
14:02:43.470 [main] INFO com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.impl.classic.HttpRequestRetryExec -- Recoverable I/O exception (java.io.IOException) caught when processing request to {}->unix://localhost:2375
```

此错误通常表示 Docker Java SDK 无法连接到 Docker 服务。如果您使用的是 Colima ，需要确保 Docker Java SDK 配置为使用 Colima 的 Docker 服务。您可以通过设置 `DOCKER_HOST` 环境变量来实现：

```bash
export DOCKER_HOST=unix://$HOME/.colima/docker.sock
```

设置 `DOCKER_HOST` 环境变量后，请重新尝试运行您的命令。这应该可以解决连接问题。

## 沙盒启动超时

如果您遇到以下错误：

```bash
Failed to establish connection to sandbox: Sandbox service did not start within timeout: 60s
```

说明沙盒健康检查失败，你可能需要登录到容器中查看日志，以便进行进一步的故障排查。

1. **列出正在运行的容器**

   ```bash
   docker ps
   ```

   找到与你的沙盒相关的容器，记下它的 **CONTAINER ID** 或 **NAMES**。

2. **进入容器**

   ```bash
   docker exec -it <container_id_or_name> /bin/bash
   ```

3. **进入日志目录**

   ```bash
   cd /var/log && ls -l
   ```

4. **识别并查看日志文件**

    - `agentscope_runtime.err.log` — `agentscope_runtime` 服务的错误输出
    - `agentscope_runtime.out.log` — `agentscope_runtime` 服务的标准输出
    - `supervisord.log` — Supervisor 进程管理日志
    - `nginx.err.log` — Nginx 错误日志
    - `nginx.out.log` — Nginx 访问/标准输出日志

   查看日志的示例命令：

   ```bash
   cat agentscope_runtime.err.log
   ```

5. **常见的日志提示**

    - 如果看到缺少环境变量的错误，请确保运行沙盒管理器的环境中已设置所需的 API 密钥。
    - 如果看到网络错误，请检查防火墙、代理或云端 Shell 网络设置。

> 在容器内部查看日志通常是最快定位沙盒健康检查失败原因的方法。
