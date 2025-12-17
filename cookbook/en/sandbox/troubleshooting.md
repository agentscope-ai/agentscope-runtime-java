# Sandbox Troubleshooting
If you encounter any issues while using the browser module, here are some troubleshooting steps:

## Docker Connection Error

If you encounter the following error:

```bash
Warning: Failed to connect to Docker using configured host (localhost:2375): com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.HttpHostConnectException: Connect to http://localhost:2375 [localhost/127.0.0.1, localhost/0:0:0:0:0:0:0:1] failed: Connection refused
Dec 10, 2025 2:02:43 PM io.agentscope.runtime.sandbox.manager.client.DockerClient openDockerClient
INFO: Falling back to default Docker configuration
14:02:43.470 [main] INFO com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.impl.classic.HttpRequestRetryExec -- Recoverable I/O exception (java.io.IOException) caught when processing request to {}->unix://localhost:2375
```

This error usually indicates that the Docker Java SDK cannot connect to the Docker service. If you are using Colima, you need to ensure that the Docker Java SDK is configured to use Colima's Docker service. You can do this by setting the `DOCKER_HOST` environment variable:

```bash
export DOCKER_HOST=unix://$HOME/.colima/docker.sock
```

After setting the `DOCKER_HOST` environment variable, please try running your command again. This should resolve the connection issue.

## Sandbox Startup Timeout

If you encounter the following error:

```bash
Failed to establish connection to sandbox: Sandbox service did not start within timeout: 60s
```

This indicates that the sandbox health check failed. You may need to log into the container to view logs for further troubleshooting.

1. **List running containers**

   ```bash
   docker ps
   ```

   Find the container related to your sandbox and note its **CONTAINER ID** or **NAMES**.

2. **Enter the container**

   ```bash
   docker exec -it <container_id_or_name> /bin/bash
   ```

3. **Navigate to the log directory**

   ```bash
   cd /var/log && ls -l
   ```

4. **Identify and view log files**

    - `agentscope_runtime.err.log` — Error output of the `agentscope_runtime` service
    - `agentscope_runtime.out.log` — Standard output of the `agentscope_runtime` service
    - `supervisord.log` — Supervisor process management log
    - `nginx.err.log` — Nginx error log
    - `nginx.out.log` — Nginx access/standard output log

   Example command to view logs:

   ```bash
   cat agentscope_runtime.err.log
   ```

5. **Common log hints**

    - If you see errors about missing environment variables, make sure the required API keys are set in the environment running the sandbox manager.
    - If you see network errors, check firewall, proxy, or cloud shell network settings.

> Viewing logs inside the container is usually the fastest way to identify the cause of sandbox health check failures.

