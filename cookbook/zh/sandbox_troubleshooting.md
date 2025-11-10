# 沙盒故障排除
如果您在使用浏览器模块时遇到任何问题，以下是一些故障排查步骤：

## Docker连接错误

如果您遇到以下错误：

```bash
Failed to connect to Docker using configured host (localhost:2375): com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.HttpHostConnectException: Connect to http://localhost:2375 [localhost/127.0.0.1, localhost/0:0:0:0:0:0:0:1] failed: Connection refused
```

此错误通常表示 Docker Java SDK 无法连接到Docker服务。目前 AgentScope Runtime Java 对于Docker设置了回退策略，当客户端无法连接到用户指定的 port 及 host 的 Docker 服务时，会自动回退到默认本地连接方式，如果均失败会报错。

遇到这种情况，请检查 Docker 服务是否正常启动，如果在 Mac 上使用的 colima 虚拟机，请启动后重试，如果使用的 Docker desktop，请运行 Docker Desktop 后重试。

## 沙盒启动超时

如果您遇到超时错误，说明沙盒健康检查失败，你可能需要登录到容器中查看日志，以便进行进一步的故障排查。

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
