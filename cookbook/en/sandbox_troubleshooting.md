# Sandbox Troubleshooting

If you encounter any issues when using the sandbox module, here are some troubleshooting steps:

## Docker Connection Error

If you encounter the following error:

```bash
Failed to connect to Docker using configured host (localhost:2375): com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.HttpHostConnectException: Connect to http://localhost:2375 [localhost/127.0.0.1, localhost/0:0:0:0:0:0:0:1] failed: Connection refused
```

This error usually indicates that the Docker Java SDK cannot connect to the Docker service. Currently, AgentScope Runtime Java has a fallback strategy for Docker. When the client cannot connect to the Docker service at the user-specified port and host, it will automatically fall back to the default local connection method. If all attempts fail, an error will be reported.

In this case, please check if the Docker service is running normally. If you are using Colima virtual machine on Mac, please start it and retry. If you are using Docker Desktop, please run Docker Desktop and retry.

## Sandbox Startup Timeout

If you encounter a timeout error, it means the sandbox health check failed. You may need to log into the container to view logs for further troubleshooting.

1. **List Running Containers**

   ```bash
   docker ps
   ```

   Find the container related to your sandbox and note its **CONTAINER ID** or **NAMES**.

2. **Enter the Container**

   ```bash
   docker exec -it <container_id_or_name> /bin/bash
   ```

3. **Navigate to Log Directory**

   ```bash
   cd /var/log && ls -l
   ```

4. **Identify and View Log Files**

   - `agentscope_runtime.err.log` — Error output from the `agentscope_runtime` service
   - `agentscope_runtime.out.log` — Standard output from the `agentscope_runtime` service
   - `supervisord.log` — Supervisor process management logs
   - `nginx.err.log` — Nginx error logs
   - `nginx.out.log` — Nginx access/standard output logs

   Example command to view logs:

   ```bash
   cat agentscope_runtime.err.log
   ```

5. **Common Log Hints**

   - If you see errors about missing environment variables, please ensure that the required API keys are set in the environment where the sandbox manager is running.
   - If you see network errors, please check firewall, proxy, or cloud Shell network settings.

> Viewing logs inside the container is usually the fastest way to identify the cause of sandbox health check failures.




