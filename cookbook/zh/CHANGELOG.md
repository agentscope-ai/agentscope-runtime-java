# CHANGELOG

## v1.0.1

### Fixed
- 修复了使用 AgentApp 的跨域配置问题
- 为沙箱文件系统添加了可读写的零拷贝挂载支持

## v1.0.0

AgentScope Runtime Java v1.0 在高效智能体部署与安全沙箱执行的坚实基础上，推出了统一的 “Agent 作为 API” 开发体验，覆盖完整智能体从本地开发到生产部署的生命周期，并扩展了更多沙箱类型、协议兼容性与更丰富的内置工具集。

**变更背景与必要性**

在 v0.x 版本中，AgentScope 的 Agent 模块（如 `AgentScopeAgent`、`SaaAgent` 等）采用黑盒化模块替换方式，通过直接将 Agent 对象传入 `AgentApp` 或 `Runner` 执行。这种封装在单智能体简单场景可以工作，但在复杂应用、尤其是多智能体组合时暴露了以下严重问题：

1. **自定义 Memory 模块会被黑盒替换**
   用户在开发环境中自定义的 Memory（如 `InMemoryMemory` 或自写类）在生产部署时会被不透明地替换为 `RedisMemory` 等实现，用户无法感知这种变化，也无法控制替换行为，导致线上与本地表现不一致。
2. **Agent State 未得到保留**
   旧框架缺少智能体状态序列化与恢复机制，多轮交互或任务中断后无法保留 agent 内部状态（如思考链、上下文变量），影响长任务的连续性。
3. **无法挂载自定义逻辑（Hooks）**
   由于生命周期全被封装在内部，用户无法在 Agent 或执行阶段添加钩子函数（hooks），例如：
   - 在推理前后插入自定义数据处理
   - 运行时动态修改工具集或 prompt
4. **多智能体与跨框架组合受限**
   黑盒模式无法在不同 agent 实例之间共享会话记录、长短期记忆服务、工具集，也难以将不同 agent 框架（如 ReActAgent）无缝组合。

以上缺陷直接限制了 AgentScope Runtime 在真实生产环境的可扩展性与可维护性，也使得“开发环境与生产环境一致”的目标无法实现。

因此，v1.0.0 重构了 Agent 接入模式，引入 **白盒化适配器模式** —— 通过
* 通过 `AgentHandler` 接口来统一定义智能体逻辑、管理组件声明周期。开发者完全使用原生框架（如 AgentScope）的编码 API 定义自己的智能体。
* `AgentApp` 作为统一入口，用来加载开发者定义的 `AgentHandler` 智能体、完成与 Runtime 内部组件的绑定、管理组件生命周期等。

这使得：

- 开发者完全使用原生 Agent 框架开发智能体，降低复杂度
- 记忆、会话、工具注册等运行时能力可按需插入
- 通过拓展 AgentApp 生命周期可实现更灵活的组件初始化与销毁

主要改进：
- **统一的开发/生产范式**：智能体在开发与生产环境中保持一致的功能性。
- **原生多智能体支持**：完全兼容 AgentScope 的多智能体范式。
- **主流 SDK 与协议集成**：支持 OpenAI SDK 与 Google A2A 协议。
- **可视化 Web UI**：部署后开箱即用的 Web 聊天界面。
- **扩展沙箱类型**：支持 GUI、浏览器、文件系统、移动端、云端（大部分可通过 VNC 可视化）。
- **丰富的内置工具集**：面向生产的搜索、RAG、AIGC、支付等模块。
- **灵活的部署模式**：支持本地线程/进程、Docker、Kubernetes、或托管云端部署。

### Added
- 原生Short/Long Memory、智能体 State 适配器集成至 AgentScope Framework。
- 新增多种 Sandbox 类型，如移动端沙箱、AgentBay无影云沙箱等。
- 新增 AgentHandler，用来作为智能体定义入口，开发者需要继承该接口以提供完整的智能体定义和处理逻辑。
- 新增 AgentApp，用来作为 Runtime 启动入口，实现组件生命周期管理、容器启动等。

### Breaking Changes
以下变更会影响现有 v0.x 用户，需要手动适配：
1. **Agent模块接口迁移**

   - `AgentScopeAgent`、`SaaAgent` 等Agent模块已被移除，相关 API 迁移到 `AgentHandler`、`AgentScopeAgentHandler` 基类中。
   - **示例迁移**：

     ```java
     # v0.x
    public void connect() throws Exception {
        buildContextManager();
        // Create SaaAgent
        AgentScopeAgent agent = buildAgentScopeAgent(apiKey, toolkit);
        // Initialize runner
        runner = Runner.builder().agent(agent).contextManager(contextManager).environmentManager(environmentManager).build();
    }

    private void buildContextManager() throws InterruptedException, ExecutionException {
        // Initialize session history service
        sessionHistoryService = new InMemorySessionHistoryService();
        // Create session
        sessionHistoryService.createSession(USER_ID, Optional.of(SESSION_ID)).get();
        // Initialize memory service
        memoryService = new InMemoryMemoryService();

        // Initialize context manager
        contextManager = new ContextManager(
                ContextComposer.class,
                sessionHistoryService,
                memoryService
        );
        contextManager.start().get();
    }

    private static AgentScopeAgent buildAgentScopeAgent(String apiKey, Toolkit toolkit) {
        ReActAgent.Builder agentBuilder =
                ReActAgent.builder()
                        .name("Assistant")
                        .sysPrompt("You are a helpful AI assistant. Be friendly and concise.")
                        .model(
                                io.agentscope.core.model.DashScopeChatModel.builder()
                                        .apiKey(apiKey)
                                        .modelName("qwen-plus")
                                        .stream(true)
                                        .enableThinking(true)
                                        .formatter(new DashScopeChatFormatter())
                                        .defaultOptions(
                                                GenerateOptions.builder()
                                                        .thinkingBudget(1024)
                                                        .build())
                                        .build())
                        .memory(new InMemoryMemory());

        AgentScopeAgent agent = AgentScopeAgent.builder()
                .agent(agentBuilder)
                .build();
        return agent;
    }

     # v1.0
	 public class MyAgentScopeAgentHandler extends AgentScopeAgentHandler {
     	@Override
     	public Flux<io.agentscope.core.agent.Event> streamQuery(AgentRequest request, Object messages) {
     		String sessionId = request.getSessionId();
     		String userId = request.getUserId();

     		try {
     			// Step 1: Export state from StateService
     			Map<String, Object> state = null;
     			if (stateService != null) {
     				try {
     					state = stateService.exportState(userId, sessionId, null).join();
     				}
     				catch (Exception e) {
     					logger.warn("Failed to export state: {}", e.getMessage());
     				}
     			}
  	
     			// Step 2: Create Toolkit and register tools
     			Toolkit toolkit = new Toolkit();

     //			toolkit.registerTool(new ExampleTool());

     			if (sandboxService != null) {
     				try {
     					// Create a BaseSandbox instance for this session
     					Sandbox sandbox = sandboxService.connect(userId, sessionId, BaseSandbox.class);
  	
     					// Register Python code execution tool (matching Python: execute_python_code)
     					toolkit.registerTool(ToolkitInit.RunPythonCodeTool(sandbox));
     					logger.debug("Registered execute_python_code tool");
     				}
     				catch (Exception e) {
     					logger.warn("Failed to create sandbox or register tools: {}", e.getMessage());
     					// Continue without tools if sandbox creation fails
     				}
     			}
  	
     			// Step 3: Create MemoryAdapter
     			MemoryAdapter memory = null;
     			if (sessionHistoryService != null) {
     				memory = new MemoryAdapter(
     						sessionHistoryService,
     						userId,
     						sessionId
     				);
     			}
  	
     			// Step 4: Create LongTermMemoryAdapter
     			LongTermMemoryAdapter longTermMemory = null;
     			if (memoryService != null) {
     				longTermMemory = new LongTermMemoryAdapter(
     						memoryService,
     						userId,
     						sessionId
     				);
     			}
  	
     			// Step 5: Create ReActAgent
     			ReActAgent.Builder agentBuilder = ReActAgent.builder()
     					.name("Friday")
     					.sysPrompt("You're a helpful assistant named Friday.")
     					.toolkit(toolkit)
     					.model(
     							DashScopeChatModel.builder()
     									.apiKey(apiKey)
     									.modelName("qwen-max")
     //									.enableThinking(true)
     									.stream(true)
     									.formatter(new DashScopeChatFormatter())
     									.build());

     			// Add long-term memory if available
     			if (longTermMemory != null) {
     				agentBuilder.longTermMemory(longTermMemory)
     						.longTermMemoryMode(LongTermMemoryMode.BOTH);
     				logger.debug("Long-term memory configured");
     			}
  	
     			if (memory != null) {
     				agentBuilder.memory(memory);
     				logger.debug("Memory adapter configured");
     			}
  	
     			ReActAgent agent = agentBuilder.build();
  	
     			// Step 6: Load state if available
     			if (state != null && !state.isEmpty()) {
     				try {
     					agent.loadStateDict(state);
     					logger.debug("Loaded state for session: {}", sessionId);
     				}
     				catch (Exception e) {
     					logger.warn("Failed to load state: {}", e.getMessage());
     				}
     			}
  	
     			// Step 7: Convert messages parameter to List<Msg>
     			// Python version: msgs parameter is already a list of Msg
     			List<Msg> agentMessages;
     			if (messages instanceof List) {
     				@SuppressWarnings("unchecked")
     				List<Msg> msgList = (List<Msg>) messages;
     				agentMessages = msgList;
     			}
     			else if (messages instanceof Msg) {
     				agentMessages = List.of((Msg) messages);
     			}
     			else {
     				logger.warn("Unexpected messages type: {}, using empty list",
     						messages != null ? messages.getClass().getName() : "null");
     				agentMessages = List.of();
     			}
  	
     			// Step 8: Stream agent responses (matching Python: async for msg, last in stream_printing_messages(...))
     			// Configure streaming options - match Python version's streaming behavior
     			StreamOptions streamOptions = StreamOptions.builder()
     					.eventTypes(EventType.REASONING, EventType.TOOL_RESULT)
     					.incremental(true)
     					.build();
  	
     			// Python version: agent(msgs) - passes the entire list
     			// In Java, ReActAgent.stream() accepts a single Msg, so we use the first message
     			// or combine all messages into memory first, then call with the last message
     			Msg queryMessage;
     			if (agentMessages.isEmpty()) {
     				// No messages provided, create a default empty message
     				queryMessage = Msg.builder()
     						.role(io.agentscope.core.message.MsgRole.USER)
     						.build();
     			}
     			else if (agentMessages.size() == 1) {
     				// Single message - use it directly
     				queryMessage = agentMessages.get(0);
     			}
     			else {
     				// Multiple messages - add all but the last to memory, then use the last one for query
     				// This matches Python behavior where all messages are in memory and the last is the query
     				for (int i = 0; i < agentMessages.size() - 1; i++) {
     					agent.getMemory().addMessage(agentMessages.get(i));
     				}
     				queryMessage = agentMessages.get(agentMessages.size() - 1);
     			}
  	
     			// Stream agent responses
     			Flux<io.agentscope.core.agent.Event> agentScopeEvents = agent.stream(queryMessage, streamOptions);
  	
     			return agentScopeEvents
     					.doOnNext(event -> {
     						// Step 9: Handle intermediate events if needed
     						// (e.g., logging)
     						logger.info("Agent event: {}", event);
     					})
     					.doFinally(signalType -> {
     						// Step 10: Save state after completion
     						if (stateService != null) {
     							try {
     								Map<String, Object> finalState = agent.stateDict();
     								if (finalState != null && !finalState.isEmpty()) {
     									stateService.saveState(userId, finalState, sessionId, null)
     											.exceptionally(e -> {
     												logger.error("Failed to save state: {}", e.getMessage(), e);
     												return null;
     											});
     								}
     							}
     							catch (Exception e) {
     								logger.error("Error saving state: {}", e.getMessage(), e);
     							}
     						}
     					})
     					.doOnError(error -> {
     						logger.error("Error in agent stream: {}", error.getMessage(), error);
     					});
  	
     		}
     		catch (Exception e) {
     			logger.error("Error in streamQuery: {}", e.getMessage(), e);
     			return Flux.error(e);
     		}
     	}
     }
     ```

2. **Tool 抽象接口变更**

   - 原 `SandboxTool` 抽象被移除，使用原生Sandbox方法

   - 示例迁移：

     ```java
     # v0.x
	  Toolkit toolkit = new Toolkit();
	  toolkit.registerTool(ToolkitInit.RunPythonCodeTool());
     
     # v1.0
	  Toolkit toolkit = new Toolkit();
	  Sandbox sandbox = sandboxService.connect(userId, sessionId, BaseSandbox.class);
	  // 需要明确指定 sandbox 实例
	  toolkit.registerTool(ToolkitInit.RunPythonCodeTool(sandbox));
     ```

### Removed
- `ContextManager`和`EnvironmentManager`已被移除，现在由 Agent 进行上下文管理
- `AgentScopeAgent`、`SaaAgent` 已被移除，相关逻辑迁移到 `AgentHandler`、`AgentScopeAgentHandler` 中以供用户白盒化开发。

------
