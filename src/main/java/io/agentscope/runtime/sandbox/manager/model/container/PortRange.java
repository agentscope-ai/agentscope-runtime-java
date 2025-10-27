package io.agentscope.runtime.sandbox.manager.model.container;

public class PortRange {
    private final int start;
    private final int end;

    public PortRange(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }
}