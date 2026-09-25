package com.victoragudo.nova.ailens;

import com.intellij.diff.fragments.LineFragment;
import com.intellij.diff.tools.util.text.LineOffsets;
import com.intellij.diff.tools.util.text.LineOffsetsUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service(Service.Level.PROJECT)
@State(name = "NovaAiLensTracker", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public final class AiLensTracker implements PersistentStateComponent<AiLensTracker.Snapshot>, Disposable {

    private static final int HIGHLIGHTER_LAYER = HighlighterLayer.CARET_ROW - 1;
    private static final Key<AiSpan> SPAN = Key.create("nova.ai.lens.span");

    public static final class StoredSpan {
        public int start;
        public int end;
        public boolean reviewed;
        public String origin;
        public int hash;
    }

    public static final class StoredFile {
        public String url;
        public List<StoredSpan> spans = new ArrayList<>();
    }

    public static final class Snapshot {
        public List<StoredFile> files = new ArrayList<>();
    }

    private final Project project;
    private final Map<Document, List<RangeHighlighter>> highlighters = new HashMap<>();
    private final Map<String, StoredFile> pending = new HashMap<>();

    public AiLensTracker(Project project) {
        this.project = project;
    }

    public static AiLensTracker getInstance(Project project) {
        return project.getService(AiLensTracker.class);
    }

    public void mark(Document document, int startLine, int endLine, AiOrigin origin) {
        update(document, set -> set.mark(startLine, endLine, false, origin));
    }

    public void review(Document document, int startLine, int endLine) {
        update(document, set -> set.review(startLine, endLine));
    }

    public void reviewAll(Document document) {
        review(document, 0, Integer.MAX_VALUE);
    }

    public void clear(Document document, int startLine, int endLine) {
        update(document, set -> set.clear(startLine, endLine));
    }

    public void clearAll(Document document) {
        clear(document, 0, Integer.MAX_VALUE);
    }

    public List<AiSpan> spans(Document document) {
        List<RangeHighlighter> current = highlighters.get(document);
        if (current == null) {
            return List.of();
        }
        List<AiSpan> spans = new ArrayList<>(current.size());
        for (RangeHighlighter highlighter : current) {
            AiSpan stored = highlighter.getUserData(SPAN);
            if (stored == null || !highlighter.isValid() || highlighter.getStartOffset() >= highlighter.getEndOffset()) {
                continue;
            }
            int start = document.getLineNumber(highlighter.getStartOffset());
            int end = document.getLineNumber(highlighter.getEndOffset());
            spans.add(stored.withLines(start, end));
        }
        return spans;
    }

    public AiSpan spanAt(Document document, int line) {
        for (AiSpan span : spans(document)) {
            if (span.contains(line)) {
                return span;
            }
        }
        return null;
    }

    public int unreviewedLines(Document document) {
        int total = 0;
        for (AiSpan span : spans(document)) {
            if (!span.reviewed()) {
                total += span.lineCount();
            }
        }
        return total;
    }

    public int unreviewedLines() {
        int total = 0;
        for (Document document : highlighters.keySet()) {
            total += unreviewedLines(document);
        }
        for (StoredFile stored : pending.values()) {
            total += unreviewedLines(stored);
        }
        return total;
    }

    public int unreviewedLines(Collection<VirtualFile> files) {
        FileDocumentManager documents = FileDocumentManager.getInstance();
        int total = 0;
        for (VirtualFile file : files) {
            Document document = documents.getCachedDocument(file);
            if (document != null) {
                total += unreviewedLines(document);
            }
            StoredFile stored = pending.get(file.getUrl());
            if (stored != null) {
                total += unreviewedLines(stored);
            }
        }
        return total;
    }

    public List<VirtualFile> filesWithUnreviewedLines() {
        FileDocumentManager documents = FileDocumentManager.getInstance();
        List<VirtualFile> files = new ArrayList<>(highlighters.size() + pending.size());
        for (Document document : highlighters.keySet()) {
            VirtualFile file = documents.getFile(document);
            if (file != null && file.isValid() && unreviewedLines(document) > 0) {
                files.add(file);
            }
        }
        VirtualFileManager virtualFiles = VirtualFileManager.getInstance();
        for (StoredFile stored : pending.values()) {
            VirtualFile file = unreviewedLines(stored) > 0 ? virtualFiles.findFileByUrl(stored.url) : null;
            if (file != null && file.isValid()) {
                files.add(file);
            }
        }
        files.sort(Comparator.comparing(VirtualFile::getPath));
        return files;
    }

    public void restore(VirtualFile file) {
        StoredFile stored = pending.remove(file.getUrl());
        if (stored == null) {
            return;
        }
        Document document = FileDocumentManager.getInstance().getDocument(file);
        if (document == null) {
            return;
        }
        List<AiSpan> restored = validSpans(stored, document.getImmutableCharSequence(), LineOffsetsUtil.create(document));
        update(document, set -> restored.forEach(span ->
                set.mark(span.startLine(), span.endLine(), span.reviewed(), span.origin())));
    }

    public void markClosedFile(VirtualFile file, CharSequence before, CharSequence after, List<LineFragment> changes) {
        String url = file.getUrl();
        StoredFile previous = pending.get(url);
        List<AiSpan> carried = previous == null
                ? List.of()
                : shift(validSpans(previous, before, LineOffsetsUtil.create(before)), changes);
        AiSpanSet set = new AiSpanSet(carried);
        for (LineFragment change : changes) {
            set.mark(change.getStartLine2(), change.getEndLine2() - 1, false, AiOrigin.AGENT);
        }
        StoredFile stored = storedFile(url, after, LineOffsetsUtil.create(after), set.spans());
        if (stored.spans.isEmpty()) {
            pending.remove(url);
        } else {
            pending.put(url, stored);
        }
        project.getMessageBus().syncPublisher(AiLensListener.TOPIC).spansChanged();
    }

    @Override
    public Snapshot getState() {
        return ApplicationManager.getApplication().runReadAction((Computable<Snapshot>) this::snapshot);
    }

    @Override
    public void loadState(Snapshot state) {
        pending.clear();
        for (StoredFile file : state.files) {
            if (file.url != null && !file.spans.isEmpty()) {
                pending.put(file.url, file);
            }
        }
    }

    @Override
    public void dispose() {
        highlighters.clear();
        pending.clear();
    }

    private Snapshot snapshot() {
        Snapshot snapshot = new Snapshot();
        Map<String, StoredFile> files = new HashMap<>(pending);
        FileDocumentManager documents = FileDocumentManager.getInstance();
        for (Document document : highlighters.keySet()) {
            VirtualFile file = documents.getFile(document);
            if (file == null) {
                continue;
            }
            StoredFile stored = storedFile(
                    file.getUrl(), document.getImmutableCharSequence(), LineOffsetsUtil.create(document), spans(document));
            if (!stored.spans.isEmpty()) {
                files.put(stored.url, stored);
            }
        }
        snapshot.files.addAll(files.values());
        snapshot.files.sort(Comparator.comparing(file -> file.url));
        return snapshot;
    }

    private static int unreviewedLines(StoredFile stored) {
        int total = 0;
        for (StoredSpan span : stored.spans) {
            if (!span.reviewed) {
                total += span.end - span.start + 1;
            }
        }
        return total;
    }

    private static List<AiSpan> validSpans(StoredFile stored, CharSequence text, LineOffsets offsets) {
        List<AiSpan> spans = new ArrayList<>(stored.spans.size());
        for (StoredSpan span : stored.spans) {
            if (span.start <= span.end && span.end < offsets.getLineCount()
                    && hash(text, offsets, span.start, span.end) == span.hash) {
                spans.add(new AiSpan(span.start, span.end, span.reviewed, AiOrigin.parse(span.origin)));
            }
        }
        return spans;
    }

    private static List<AiSpan> shift(List<AiSpan> spans, List<LineFragment> changes) {
        List<AiSpan> shifted = new ArrayList<>(spans.size());
        for (AiSpan span : spans) {
            int start = shiftLine(span.startLine(), changes);
            int end = shiftLine(span.endLine(), changes);
            if (start <= end) {
                shifted.add(span.withLines(start, end));
            }
        }
        return shifted;
    }

    private static int shiftLine(int line, List<LineFragment> changes) {
        int delta = 0;
        for (LineFragment change : changes) {
            if (line < change.getStartLine1()) {
                break;
            }
            if (line < change.getEndLine1()) {
                int lastChangedLine = Math.max(change.getStartLine2(), change.getEndLine2() - 1);
                return Math.min(change.getStartLine2() + line - change.getStartLine1(), lastChangedLine);
            }
            delta = change.getEndLine2() - change.getEndLine1();
        }
        return line + delta;
    }

    private static StoredFile storedFile(String url, CharSequence text, LineOffsets offsets, List<AiSpan> spans) {
        StoredFile stored = new StoredFile();
        stored.url = url;
        for (AiSpan span : spans) {
            if (span.endLine() >= offsets.getLineCount()) {
                continue;
            }
            StoredSpan storedSpan = new StoredSpan();
            storedSpan.start = span.startLine();
            storedSpan.end = span.endLine();
            storedSpan.reviewed = span.reviewed();
            storedSpan.origin = span.origin().name();
            storedSpan.hash = hash(text, offsets, span.startLine(), span.endLine());
            stored.spans.add(storedSpan);
        }
        return stored;
    }

    private static int hash(CharSequence text, LineOffsets offsets, int startLine, int endLine) {
        return text.subSequence(offsets.getLineStart(startLine), offsets.getLineEnd(endLine)).toString().hashCode();
    }

    private void update(Document document, Consumer<AiSpanSet> change) {
        if (project.isDisposed() || document.getLineCount() == 0) {
            return;
        }
        AiSpanSet set = new AiSpanSet(spans(document));
        change.accept(set);
        apply(document, set.spans());
        project.getMessageBus().syncPublisher(AiLensListener.TOPIC).spansChanged();
    }

    private void apply(Document document, List<AiSpan> spans) {
        MarkupModel markup = DocumentMarkupModel.forDocument(document, project, true);
        List<RangeHighlighter> previous = highlighters.remove(document);
        if (previous != null) {
            previous.forEach(RangeHighlighter::dispose);
        }
        if (spans.isEmpty()) {
            return;
        }
        int lastLine = document.getLineCount() - 1;
        List<RangeHighlighter> created = new ArrayList<>(spans.size());
        for (AiSpan span : spans) {
            int start = Math.min(Math.max(span.startLine(), 0), lastLine);
            int end = Math.min(span.endLine(), lastLine);
            if (end < start) {
                continue;
            }
            AiSpan clamped = span.withLines(start, end);
            RangeHighlighter highlighter = markup.addRangeHighlighter(
                    AiLensColors.forSpan(clamped),
                    document.getLineStartOffset(start),
                    document.getLineEndOffset(end),
                    HIGHLIGHTER_LAYER,
                    HighlighterTargetArea.LINES_IN_RANGE);
            highlighter.putUserData(SPAN, clamped);
            highlighter.setLineMarkerRenderer(AiGutterRenderer.forSpan(clamped));
            highlighter.setErrorStripeTooltip(tooltip(clamped));
            created.add(highlighter);
        }
        if (!created.isEmpty()) {
            highlighters.put(document, created);
        }
    }

    private static String tooltip(AiSpan span) {
        String state = span.reviewed() ? "Reviewed." : "Not reviewed yet.";
        return "Written by " + span.origin().description() + ". " + state;
    }
}
