package com.victoragudo.nova.ailens;

import com.intellij.diff.comparison.ComparisonManager;
import com.intellij.diff.comparison.ComparisonPolicy;
import com.intellij.diff.comparison.DiffTooBigException;
import com.intellij.diff.fragments.LineFragment;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.impl.LoadTextUtil;
import com.intellij.openapi.progress.DumbProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.PathUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

@Service(Service.Level.APP)
public final class AiEditRecorder implements DocumentListener, Disposable {

    private static final Logger LOG = Logger.getInstance(AiEditRecorder.class);
    private static final String GIT_DIR = "/.git/";
    private static final String GIT_BRANCHES = "/.git/refs/heads/";
    private static final String GIT_REBASE = "/.git/rebase-";
    private static final String GIT_HEAD_LOG = "/.git/logs/HEAD";
    private static final Set<String> GIT_STATE_FILES = Set.of(
            "HEAD", "ORIG_HEAD", "FETCH_HEAD", "MERGE_HEAD", "CHERRY_PICK_HEAD", "REVERT_HEAD", "REBASE_HEAD");
    private static final long GIT_QUIET_MILLIS = 5_000;
    private static final int MAX_RELOAD_DIFF_LENGTH = 2_000_000;

    private final List<RangeMarker> pendingInsertions = new ArrayList<>();
    private final Map<Document, CharSequence> reloadSnapshots = new WeakHashMap<>();
    private final Map<VirtualFile, CharSequence> closedSnapshots = new HashMap<>();
    private int assistantDepth;
    private volatile long gitQuietUntil;

    public AiEditRecorder() {
        EditorFactory.getInstance().getEventMulticaster().addDocumentListener(this, this);
    }

    public static AiEditRecorder getInstance() {
        return ApplicationManager.getApplication().getService(AiEditRecorder.class);
    }

    void enterIfAssistant(String signature) {
        if (isAssistant(signature)) {
            assistantDepth++;
        }
    }

    void exitIfAssistant(String signature) {
        if (isAssistant(signature) && assistantDepth > 0) {
            assistantDepth--;
        }
    }

    void userTyped() {
        assistantDepth = 0;
    }

    @Override
    public void documentChanged(DocumentEvent event) {
        if (assistantDepth <= 0 || event.getNewLength() == 0 || StringUtil.isEmptyOrSpaces(event.getNewFragment())) {
            return;
        }
        Document document = event.getDocument();
        pendingInsertions.add(document.createRangeMarker(event.getOffset(), event.getOffset() + event.getNewLength()));
        if (pendingInsertions.size() == 1) {
            ApplicationManager.getApplication().invokeLater(this::flushInsertions, ModalityState.any());
        }
    }

    void beforeReload(Document document) {
        AiLensSettings.Options options = AiLensSettings.getInstance().options();
        if (!options.trackExternalEdits || isGitQuiet() || document.getTextLength() > MAX_RELOAD_DIFF_LENGTH) {
            return;
        }
        reloadSnapshots.put(document, document.getImmutableCharSequence());
    }

    void afterReload(Document document) {
        CharSequence before = reloadSnapshots.remove(document);
        if (before == null || isGitQuiet()) {
            return;
        }
        for (LineFragment fragment : insertedLines(before, document.getImmutableCharSequence())) {
            record(document, fragment.getStartLine2(), fragment.getEndLine2() - 1, AiOrigin.AGENT);
        }
    }

    void snapshotClosedFiles(List<? extends VFileEvent> events) {
        if (!AiLensSettings.getInstance().options().trackExternalEdits || isGitQuiet()) {
            return;
        }
        for (VFileEvent event : events) {
            VirtualFile file = event.getFile();
            if (!(event instanceof VFileContentChangeEvent) || !event.isFromRefresh() || !isClosedTextFile(file)
                    || projectsContaining(file).isEmpty()) {
                continue;
            }
            try {
                closedSnapshots.put(file, LoadTextUtil.getTextByBinaryPresentation(file.contentsToByteArray(), file));
            } catch (IOException e) {
                LOG.debug("Nova: cannot read the previous content of " + file.getPath(), e);
            }
        }
    }

    void recordClosedFiles() {
        if (closedSnapshots.isEmpty()) {
            return;
        }
        Map<VirtualFile, CharSequence> snapshots = new HashMap<>(closedSnapshots);
        closedSnapshots.clear();
        if (isGitQuiet()) {
            return;
        }
        for (Map.Entry<VirtualFile, CharSequence> entry : snapshots.entrySet()) {
            VirtualFile file = entry.getKey();
            if (!file.isValid()) {
                continue;
            }
            CharSequence after = LoadTextUtil.loadText(file);
            List<LineFragment> changes = insertedLines(entry.getValue(), after);
            if (changes.isEmpty()) {
                continue;
            }
            for (Project project : projectsContaining(file)) {
                AiLensTracker.getInstance(project).markClosedFile(file, entry.getValue(), after, changes);
            }
        }
    }

    boolean observeFileEvents(List<? extends VFileEvent> events) {
        int refreshedContent = 0;
        boolean gitStateChanged = false;
        for (VFileEvent event : events) {
            String path = event.getPath();
            if (path.contains(GIT_DIR)) {
                gitStateChanged |= isGitStatePath(path);
            } else if (event instanceof VFileContentChangeEvent && event.isFromRefresh()) {
                refreshedContent++;
            }
        }
        if (gitStateChanged || refreshedContent > AiLensSettings.getInstance().options().externalBatchLimit) {
            gitQuietUntil = System.currentTimeMillis() + GIT_QUIET_MILLIS;
        }
        return gitStateChanged;
    }

    @Override
    public void dispose() {
        pendingInsertions.forEach(RangeMarker::dispose);
        pendingInsertions.clear();
        reloadSnapshots.clear();
        closedSnapshots.clear();
    }

    private boolean isAssistant(String signature) {
        AiLensSettings settings = AiLensSettings.getInstance();
        return signature != null
                && settings.options().trackAssistantEdits
                && settings.assistantRegex().matcher(signature).find();
    }

    private static boolean isClosedTextFile(VirtualFile file) {
        return file != null
                && file.isValid()
                && file.isInLocalFileSystem()
                && !file.isDirectory()
                && file.getLength() <= MAX_RELOAD_DIFF_LENGTH
                && !file.getFileType().isBinary()
                && FileDocumentManager.getInstance().getCachedDocument(file) == null;
    }

    private static List<LineFragment> insertedLines(CharSequence before, CharSequence after) {
        List<LineFragment> fragments;
        try {
            fragments = ComparisonManager.getInstance().compareLines(
                    before, after, ComparisonPolicy.DEFAULT, DumbProgressIndicator.INSTANCE);
        } catch (DiffTooBigException e) {
            return List.of();
        }
        List<LineFragment> inserted = new ArrayList<>(fragments.size());
        for (LineFragment fragment : fragments) {
            if (fragment.getEndLine2() > fragment.getStartLine2()) {
                inserted.add(fragment);
            }
        }
        return inserted;
    }

    private static List<Project> projectsContaining(VirtualFile file) {
        Project[] open = ProjectManager.getInstance().getOpenProjects();
        List<Project> containing = new ArrayList<>(open.length);
        for (Project project : open) {
            if (project.isDisposed()) {
                continue;
            }
            boolean inContent = ApplicationManager.getApplication().runReadAction(
                    (Computable<Boolean>) () -> ProjectFileIndex.getInstance(project).isInContent(file));
            if (inContent) {
                containing.add(project);
            }
        }
        return containing;
    }

    private boolean isGitQuiet() {
        return System.currentTimeMillis() < gitQuietUntil;
    }

    private static boolean isGitStatePath(String path) {
        return GIT_STATE_FILES.contains(PathUtil.getFileName(path))
                || path.contains(GIT_BRANCHES)
                || path.contains(GIT_REBASE)
                || path.endsWith(GIT_HEAD_LOG);
    }

    private void flushInsertions() {
        List<RangeMarker> markers = List.copyOf(pendingInsertions);
        pendingInsertions.clear();
        for (RangeMarker marker : markers) {
            if (marker.isValid() && marker.getEndOffset() > marker.getStartOffset()) {
                recordInsertion(marker);
            }
            marker.dispose();
        }
    }

    private void recordInsertion(RangeMarker marker) {
        Document document = marker.getDocument();
        CharSequence text = document.getImmutableCharSequence();
        int start = marker.getStartOffset();
        int end = marker.getEndOffset();
        while (start < end - 1 && text.charAt(start) == '\n') {
            start++;
        }
        record(document, document.getLineNumber(start), document.getLineNumber(Math.max(start, end - 1)), AiOrigin.ASSISTANT);
    }

    private void record(Document document, int startLine, int endLine, AiOrigin origin) {
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (file == null || !file.isInLocalFileSystem()) {
            return;
        }
        for (Project project : projectsContaining(file)) {
            AiLensTracker.getInstance(project).mark(document, startLine, endLine, origin);
        }
    }
}
