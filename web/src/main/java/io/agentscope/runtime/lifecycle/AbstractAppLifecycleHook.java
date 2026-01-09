package io.agentscope.runtime.lifecycle;

public abstract class AbstractAppLifecycleHook implements AppLifecycleHook {

	protected int operation;

	public AbstractAppLifecycleHook(){
		this.operation = ALL;
	}

}
