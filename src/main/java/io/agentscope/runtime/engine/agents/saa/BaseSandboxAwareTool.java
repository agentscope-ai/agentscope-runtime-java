/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agentscope.runtime.engine.agents.saa;

import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.tools.SandboxTool;

public abstract class BaseSandboxAwareTool<T extends SandboxTool, Req, Res> implements SandboxAwareTool<Req, Res> {

	protected T sandboxTool;

	public BaseSandboxAwareTool(T sandboxTool) {
		this.sandboxTool = sandboxTool;
	}

	@Override
	public SandboxManager getSandboxManager() {
		return sandboxTool.getSandboxManager();
	}

	@Override
	public void setSandboxManager(SandboxManager sandboxManager) {
		this.sandboxTool.setSandboxManager(sandboxManager);
	}

	@Override
	public Sandbox getSandbox() {
		return this.sandboxTool.getSandbox();
	}

	@Override
	public void setSandbox(Sandbox sandbox) {
		this.sandboxTool.setSandbox(sandbox);
	}

	@Override
	public Class<?> getSandboxClass() {
		return sandboxTool.getSandboxClass();
	}
}
