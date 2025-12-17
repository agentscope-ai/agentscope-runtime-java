package io.agentscope;

import io.agentscope.runtime.sandbox.manager.registry.SandboxProvider;

import java.util.Collections;
import java.util.Collection;

public class TestSandboxProvider implements SandboxProvider {

    @Override
    public Collection<Class<?>> getSandboxClasses() {
        return Collections.singletonList(TestBox.class);
    }
}

