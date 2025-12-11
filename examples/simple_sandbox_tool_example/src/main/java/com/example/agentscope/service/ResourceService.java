package com.example.agentscope.service;

import io.agentscope.runtime.engine.services.sandbox.SandboxService;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ResourceService {
    @Autowired
    public SandboxService sandboxService;

    @PreDestroy
    public void cleanup() {
        System.out.println("CloseOperation: Releasing resources...");
        sandboxService.stop();
    }
}
