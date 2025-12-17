# CHANGELOG

## v1.0.0

AgentScope Runtime Java v1.0 builds upon a solid foundation of efficient agent deployment and secure sandbox execution, introducing a unified "Agent as API" development experience that covers the complete agent lifecycle from local development to production deployment, and extends support for more sandbox types, protocol compatibility, and a richer built-in toolset.

**Change Background and Necessity**

In v0.x versions, AgentScope's Agent modules (such as `AgentScopeAgent`, `SaaAgent`, etc.) used a black-box module replacement approach, by directly passing Agent objects to `AgentApp` or `Runner` for execution. This encapsulation works in simple single-agent scenarios, but exposes the following serious problems in complex applications, especially when combining multiple agents:

1. **Custom Memory modules are replaced in a black-box manner**
   User-defined Memory modules in development environments (such as `InMemoryMemory` or custom classes) are transparently replaced with implementations like `RedisMemory` in production deployment. Users cannot perceive this change or control the replacement behavior, leading to inconsistent behavior between production and local environments.

2. **Agent State is not preserved**
   The old framework lacked agent state serialization and recovery mechanisms. After multiple rounds of interaction or task interruption, it was unable to preserve agent internal state (such as thinking chains, context variables), affecting continuity of long tasks.

3. **Unable to mount custom logic (Hooks)**
   Since the entire lifecycle was encapsulated internally, users could not add hook functions at the Agent or execution stage, for example:
   - Insert custom data processing before/after reasoning
   - Dynamically modify toolset or prompt at runtime

4. **Multi-agent and cross-framework combination limitations**
   The black-box mode cannot share session records, long-term and short-term memory services, and toolsets between different agent instances, and it is difficult to seamlessly combine different agent frameworks (such as ReActAgent).

The above defects directly limit the extensibility and maintainability of AgentScope Runtime in real production environments, and also make the goal of "consistent development and production environments" unachievable.

Therefore, v1.0.0 refactored the Agent integration pattern, introducing a **white-box adapter pattern** — through:
* Using the `AgentHandler` interface to uniformly define agent logic and manage component lifecycle. Developers fully use native framework (such as AgentScope) coding APIs to define their own agents.
* `AgentApp` as a unified entry point, used to load developer-defined `AgentHandler` agents, complete binding with Runtime internal components, manage component lifecycle, etc.

This enables:

- Developers to fully use native Agent frameworks to develop agents, reducing complexity
- Runtime capabilities such as memory, session, and tool registration can be inserted as needed
- More flexible component initialization and destruction can be achieved by extending AgentApp lifecycle

Key improvements:
- **Unified Development/Production Paradigm**: Agents maintain consistent functionality in development and production environments.
- **Native Multi-Agent Support**: Fully compatible with AgentScope's multi-agent paradigm.
- **Mainstream SDK and Protocol Integration**: Supports OpenAI SDK and Google A2A protocol.
- **Visual Web UI**: Out-of-the-box web chat interface available immediately after deployment.
- **Extended Sandbox Types**: Supports GUI, browser, file system, mobile, cloud (most can be visualized via VNC).
- **Rich Built-in Toolset**: Production-oriented modules for search, RAG, AIGC, payment, etc.
- **Flexible Deployment Modes**: Supports local thread/process, Docker, Kubernetes, or managed cloud deployment.

### Added
- Native Short/Long Memory and agent State adapter integration into AgentScope Framework.
- Added multiple Sandbox types, such as mobile sandbox, AgentBay cloud sandbox, etc. (in development).
- Added AgentHandler, used as the agent definition entry point. Developers need to inherit this interface to provide complete agent definition and processing logic.
- Added AgentApp, used as the Runtime startup entry point, implementing component lifecycle management, container startup, etc.

### Breaking Changes
The following changes will affect existing v0.x users and require manual adaptation:

1. **Agent Module Interface Migration**

   - `AgentScopeAgent`, `SaaAgent` and other Agent modules have been removed. Related APIs have been migrated to `AgentHandler`, `AgentScopeAgentHandler` base classes.
   - **Migration Example**:

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

2. **Tool Abstract Interface Changes**

   - The original `SandboxTool` abstraction has been removed, using native Sandbox methods

   - Migration example:

     ```java
     # v0.x
	Toolkit toolkit = new Toolkit();
	toolkit.registerTool(ToolkitInit.RunPythonCodeTool());

     # v1.0
	Toolkit toolkit = new Toolkit();
	Sandbox sandbox = sandboxService.connect(userId, sessionId, BaseSandbox.class);
	// Need to explicitly specify sandbox instance
	toolkit.registerTool(ToolkitInit.RunPythonCodeTool(sandbox));
     ```

### Removed
- `ContextManager` and `EnvironmentManager` have been removed. Context management is now handled by the Agent.
- `AgentScopeAgent`, `SaaAgent` have been removed. Related logic has been migrated to `AgentHandler`, `AgentScopeAgentHandler` for white-box development by users.

------
