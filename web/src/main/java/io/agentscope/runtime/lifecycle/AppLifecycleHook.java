package io.agentscope.runtime.lifecycle;

import io.agentscope.runtime.app.AgentApp;
import io.agentscope.runtime.engine.DeployManager;

public interface AppLifecycleHook {

	int BEFORE_RUN = 1;

	int AFTER_RUN = 1 << 1;

	int BEFORE_STOP = 1 << 2;

	int AFTER_STOP = 1 << 3;

	int JVM_EXIT = 1 << 4;

	int ALL = BEFORE_RUN | AFTER_RUN | BEFORE_STOP | AFTER_STOP | JVM_EXIT;

	default void beforeRun(AgentApp app, DeployManager deployManager) {

	}

	default void afterRun(AgentApp app, DeployManager deployManager) {

	}

	default void beforeStop(AgentApp app, DeployManager deployManager) {

	}

	default void afterStop(AgentApp app, DeployManager deployManager) {

	}

	default void onJvmExit(AgentApp app, DeployManager deployManager) {

	}

	default int priority() {
		return 0;
	}

	int operation();
}
