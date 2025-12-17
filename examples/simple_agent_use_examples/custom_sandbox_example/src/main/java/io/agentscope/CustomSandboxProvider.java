package io.agentscope;

import io.agentscope.runtime.sandbox.manager.registry.SandboxProvider;

import java.util.Collection;
import java.util.Collections;

public class CustomSandboxProvider implements SandboxProvider {

    @Override
    public Collection<Class<?>> getSandboxClasses() {
        return Collections.singletonList(CustomSandbox.class);
    }
}

