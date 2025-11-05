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
package io.agentscope.runtime.sandbox.tools.base;

import io.agentscope.runtime.sandbox.box.BaseSandbox;
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.tools.SandboxTool;

public abstract class BaseSandboxTool extends SandboxTool {

	protected BaseSandboxTool(String name, String toolType, String description) {
		super(name, toolType, description);
	}

	protected BaseSandboxTool(String name, String toolType, String description, SandboxManager sandboxManager) {
		super(name, toolType, description, sandboxManager);
	}

	@Override
	public Class<? extends Sandbox> getSandboxClass() {
		return BaseSandbox.class;
	}
}
