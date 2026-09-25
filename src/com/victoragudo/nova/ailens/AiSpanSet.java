package com.victoragudo.nova.ailens;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class AiSpanSet {

    private final List<AiSpan> spans;

    AiSpanSet(List<AiSpan> initial) {
        spans = new ArrayList<>(initial.size() + 2);
        spans.addAll(initial);
        normalize();
    }

    List<AiSpan> spans() {
        return List.copyOf(spans);
    }

    void mark(int start, int end, boolean reviewed, AiOrigin origin) {
        if (end < start) {
            return;
        }
        clear(start, end);
        spans.add(new AiSpan(start, end, reviewed, origin));
        normalize();
    }

    void clear(int start, int end) {
        List<AiSpan> kept = new ArrayList<>(spans.size() + 1);
        for (AiSpan span : spans) {
            if (!span.overlaps(start, end)) {
                kept.add(span);
                continue;
            }
            if (span.startLine() < start) {
                kept.add(span.withLines(span.startLine(), start - 1));
            }
            if (span.endLine() > end) {
                kept.add(span.withLines(end + 1, span.endLine()));
            }
        }
        spans.clear();
        spans.addAll(kept);
    }

    void review(int start, int end) {
        for (AiSpan span : List.copyOf(spans)) {
            if (span.reviewed() || !span.overlaps(start, end)) {
                continue;
            }
            mark(Math.max(start, span.startLine()), Math.min(end, span.endLine()), true, span.origin());
        }
    }

    private void normalize() {
        spans.sort(Comparator.comparingInt(AiSpan::startLine));
        List<AiSpan> merged = new ArrayList<>(spans.size());
        for (AiSpan span : spans) {
            if (merged.isEmpty()) {
                merged.add(span);
                continue;
            }
            AiSpan last = merged.get(merged.size() - 1);
            if (last.reviewed() == span.reviewed() && last.touches(span)) {
                AiOrigin origin = last.lineCount() >= span.lineCount() ? last.origin() : span.origin();
                merged.set(merged.size() - 1, new AiSpan(
                        last.startLine(), Math.max(last.endLine(), span.endLine()), last.reviewed(), origin));
            } else {
                merged.add(span);
            }
        }
        spans.clear();
        spans.addAll(merged);
    }
}
