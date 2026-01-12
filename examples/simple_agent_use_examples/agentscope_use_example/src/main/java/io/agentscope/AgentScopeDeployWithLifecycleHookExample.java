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

package io.agentscope;

import java.util.Objects;

import io.agentscope.runtime.app.AgentApp;
import io.agentscope.runtime.hook.AbstractAppLifecycleHook;
import io.agentscope.runtime.hook.HookContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentScopeDeployWithLifecycleHookExample {
	
	private static final Logger LOG = LoggerFactory.getLogger(AgentScopeDeployWithLifecycleHookExample.class);

	public static void main(String[] args) {
		String[] commandLine = new String[2];
		commandLine[0] = "-f";
		commandLine[1] = Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource(".env")).getPath();
		AgentApp app = new AgentApp(commandLine);
		app.hooks(new AbstractAppLifecycleHook() {
			@Override
			public int operation() {
				return BEFORE_RUN | AFTER_RUN | JVM_EXIT;
			}

			@Override
			public void beforeRun(HookContext context) {
				LOG.info("beforeRun");
			}

			@Override
			public void afterRun(HookContext context) {
				LOG.info("afterRun");
			}

			@Override
			public void onJvmExit(HookContext context) {
				LOG.info("onJvmExit");
			}
		});
		app.run();
	}
}
