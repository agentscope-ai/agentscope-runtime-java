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


package io.agentscope.runtime.hook;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.agentscope.runtime.app.AgentApp;
import io.agentscope.runtime.engine.DeployManager;

public class HookContext {
	private final AgentApp app;
	private final DeployManager deployManager;
	private final Map<String, Object> extraInfo = new ConcurrentHashMap<>();

	public HookContext(AgentApp app, DeployManager deployManager) {
		this.app = app;
		this.deployManager = deployManager;
	}

	public AgentApp getApp() {
		return app;
	}

	public DeployManager getDeployManager() {
		return deployManager;
	}

	public Map<String, Object> getExtraInfo() {
		return extraInfo;
	}
}
