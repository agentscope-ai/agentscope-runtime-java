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

public interface AppLifecycleHook {

	int BEFORE_RUN = 1;

	int AFTER_RUN = 1 << 1;

	int BEFORE_STOP = 1 << 2;

	int AFTER_STOP = 1 << 3;

	int JVM_EXIT = 1 << 4;

	int ALL = BEFORE_RUN | AFTER_RUN | BEFORE_STOP | AFTER_STOP | JVM_EXIT;

	default void beforeRun(HookContext context) {

	}

	default void afterRun(HookContext context) {

	}

	default void beforeStop(HookContext context) {

	}

	default void afterStop(HookContext context) {

	}

	default void onJvmExit(HookContext context) {

	}

	default int priority() {
		return 0;
	}

	int operation();
}
