package io.agentscope;

import io.agentscope.runtime.sandbox.manager.registry.SandboxProvider;

import java.util.Collection;
import java.util.Collections;

public class TestSandboxProvider implements SandboxProvider {

    @Override
    public Collection<Class<?>> getSandboxClasses() {
        return Collections.singletonList(TestBox.class);
    }
}

