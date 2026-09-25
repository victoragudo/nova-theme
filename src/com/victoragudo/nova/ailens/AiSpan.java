package com.victoragudo.nova.ailens;

public record AiSpan(int startLine, int endLine, boolean reviewed, AiOrigin origin) {

    public int lineCount() {
        return endLine - startLine + 1;
    }

    public boolean contains(int line) {
        return line >= startLine && line <= endLine;
    }

    boolean overlaps(int start, int end) {
        return startLine <= end && endLine >= start;
    }

    boolean touches(AiSpan next) {
        return endLine + 1 >= next.startLine;
    }

    AiSpan withLines(int start, int end) {
        return new AiSpan(start, end, reviewed, origin);
    }
}
