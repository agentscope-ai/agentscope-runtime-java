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
package io.agentscope.runtime.lifecycle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import io.agentscope.runtime.engine.Runner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Separate listener for application shutdown events.
 *
 */
@Component
public class RunnerShutdownListener implements ApplicationListener<ContextClosedEvent> {

	private static final Logger logger = LoggerFactory.getLogger(RunnerShutdownListener.class);

	private final Runner runner;

	/**
	 * Constructor with Runner dependency injection.
	 *
	 * @param runner the Runner instance (optional, may be null if not configured)
	 */
	@Autowired(required = false)
	public RunnerShutdownListener(Runner runner) {
		this.runner = runner;
	}

	/**
	 * Handle application shutdown event.
	 *
	 * <p>It stops the runner, calls shutdown_handler, and then calls after_finish callback.</p>
	 *
	 * @param event the context closed event
	 */
	@Override
	public void onApplicationEvent(@NonNull ContextClosedEvent event) {
		// Only handle the root context close event (avoid duplicate calls)
		if (event.getApplicationContext().getParent() != null) {
			return;
		}

		if (runner == null) {
			return;
		}

		try {
			logger.info("[AgentAppShutdownListener] Shutting down Runner...");

			// Stop the runner first
			try {
				runner.stop();
				runner.shutdown();
				logger.info("[AgentAppShutdownListener] Runner shutdown completed");
			}
			catch (Exception e) {
				logger.error("[AgentAppShutdownListener] Error during shutdown: {}", e.getMessage());
				throw e;
			}

		}
		catch (Exception e) {
			logger.error("[AgentAppShutdownListener] Error during shutdown: {}",
					e.getMessage(), e);
		}
	}
}
