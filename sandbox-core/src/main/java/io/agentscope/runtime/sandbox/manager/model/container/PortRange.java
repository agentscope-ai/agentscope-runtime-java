package io.agentscope.runtime.sandbox.manager.model.container;

public class PortRange {
    private int start = 49152;
    private int end = 59152;

    public PortRange(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public PortRange(){
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }
}
