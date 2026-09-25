package com.victoragudo.nova.ailens;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Alarm;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service(Service.Level.PROJECT)
public final class AiCommitLens implements Disposable {

    private static final Logger LOG = Logger.getInstance(AiCommitLens.class);
    private static final String GIT_DIR_NAME = ".git";
    private static final String UNCOMMITTED_SHA = "0000000000000000000000000000000000000000";
    private static final Pattern BLAME_HEADER = Pattern.compile("^([0-9a-f]{40}) \\d+ (\\d+)(?: \\d+)?$");
    private static final String FIELD_SEPARATOR = "\u001f";
    private static final String RECORD_SEPARATOR = "\u001e";
    private static final String SHOW_FORMAT = "--format=%H%x1f%an <%ae>%x1f%B%x1e";
    private static final String NOT_AI = "";
    private static final int SHORT_SHA_LENGTH = 8;
    private static final int REFRESH_DELAY_MILLIS = 400;
    private static final int GIT_TIMEOUT_MILLIS = 20_000;
    private static final int MAX_BLAME_LINES = 20_000;
    private static final int SHOW_BATCH_SIZE = 100;
    private static final int HIGHLIGHTER_LAYER = HighlighterLayer.SYNTAX - 2;

    private record CommitRun(int startLine, int endLine, String sha, String label) {
    }

    private record BlameRequest(VirtualFile file, Document document, VirtualFile root, String path, String text, long stamp) {
    }

    private final Project project;
    private final Alarm alarm;
    private final Set<VirtualFile> queued = new LinkedHashSet<>();
    private final Map<Document, List<RangeHighlighter>> highlighters = new HashMap<>();
    private final Map<String, String> verdicts = new ConcurrentHashMap<>();
    private boolean refreshAllQueued;

    public AiCommitLens(Project project) {
        this.project = project;
        this.alarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, this);
    }

    public static AiCommitLens getInstance(Project project) {
        return project.getService(AiCommitLens.class);
    }

    public void scheduleRefresh(VirtualFile file) {
        synchronized (queued) {
            queued.add(file);
        }
        reschedule();
    }

    public void scheduleRefreshAll() {
        synchronized (queued) {
            refreshAllQueued = true;
        }
        reschedule();
    }

    static void refreshOpenProjects(boolean resetVerdicts) {
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            if (project.isDisposed()) {
                continue;
            }
            AiCommitLens lens = getInstance(project);
            if (resetVerdicts) {
                lens.verdicts.clear();
            }
            lens.scheduleRefreshAll();
        }
    }

    @Override
    public void dispose() {
        highlighters.clear();
        verdicts.clear();
    }

    private void reschedule() {
        if (project.isDisposed()) {
            return;
        }
        alarm.cancelAllRequests();
        alarm.addRequest(this::flush, REFRESH_DELAY_MILLIS);
    }

    private void flush() {
        if (project.isDisposed()) {
            return;
        }
        List<VirtualFile> files;
        synchronized (queued) {
            files = new ArrayList<>(queued);
            if (refreshAllQueued) {
                files.addAll(List.of(FileEditorManager.getInstance(project).getOpenFiles()));
            }
            queued.clear();
            refreshAllQueued = false;
        }
        boolean enabled = AiLensSettings.getInstance().options().highlightAiCommits;
        for (VirtualFile file : new LinkedHashSet<>(files)) {
            Document document = file.isValid() ? FileDocumentManager.getInstance().getDocument(file) : null;
            if (document == null) {
                continue;
            }
            if (!enabled) {
                apply(document, List.of());
                continue;
            }
            BlameRequest request = request(file, document);
            if (request != null) {
                ApplicationManager.getApplication().executeOnPooledThread(() -> blame(request));
            }
        }
    }

    private BlameRequest request(VirtualFile file, Document document) {
        if (!file.isInLocalFileSystem() || document.getLineCount() > MAX_BLAME_LINES) {
            return null;
        }
        VirtualFile root = gitRoot(file);
        if (root == null) {
            return null;
        }
        String path = VfsUtilCore.getRelativePath(file, root, '/');
        if (path == null) {
            return null;
        }
        return new BlameRequest(file, document, root, path, document.getText(), document.getModificationStamp());
    }

    private static VirtualFile gitRoot(VirtualFile file) {
        for (VirtualFile dir = file.getParent(); dir != null; dir = dir.getParent()) {
            if (dir.findChild(GIT_DIR_NAME) != null) {
                return dir;
            }
        }
        return null;
    }

    private void blame(BlameRequest request) {
        List<CommitRun> runs;
        try {
            runs = aiRuns(request);
        } catch (ExecutionException | IOException e) {
            LOG.debug("Nova: git blame failed for " + request.path(), e);
            return;
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed()) {
                return;
            }
            if (request.document().getModificationStamp() != request.stamp()) {
                scheduleRefresh(request.file());
                return;
            }
            apply(request.document(), runs);
        }, ModalityState.any());
    }

    private List<CommitRun> aiRuns(BlameRequest request) throws ExecutionException, IOException {
        String[] lineShas = blameLines(request);
        if (lineShas.length == 0) {
            return List.of();
        }
        resolveVerdicts(request.root(), lineShas);
        List<CommitRun> runs = new ArrayList<>();
        int line = 0;
        while (line < lineShas.length) {
            String sha = lineShas[line];
            String label = sha == null ? NOT_AI : verdicts.getOrDefault(sha, NOT_AI);
            int end = line;
            while (end + 1 < lineShas.length && sha != null && sha.equals(lineShas[end + 1])) {
                end++;
            }
            if (!label.isEmpty()) {
                runs.add(new CommitRun(line, end, sha, label));
            }
            line = end + 1;
        }
        return runs;
    }

    private String[] blameLines(BlameRequest request) throws ExecutionException, IOException {
        GeneralCommandLine command = git(request.root(), "blame", "--porcelain", "--contents", "-", "--", request.path());
        CapturingProcessHandler handler = new CapturingProcessHandler(command);
        try (OutputStream input = handler.getProcessInput()) {
            input.write(request.text().getBytes(StandardCharsets.UTF_8));
        }
        ProcessOutput output = handler.runProcess(GIT_TIMEOUT_MILLIS);
        if (output.isTimeout() || output.getExitCode() != 0) {
            return new String[0];
        }
        String[] shas = new String[request.document().getLineCount()];
        for (String line : output.getStdoutLines()) {
            Matcher header = BLAME_HEADER.matcher(line);
            if (!header.matches()) {
                continue;
            }
            int index = Integer.parseInt(header.group(2)) - 1;
            String sha = header.group(1);
            if (index >= 0 && index < shas.length && !UNCOMMITTED_SHA.equals(sha)) {
                shas[index] = sha;
            }
        }
        return shas;
    }

    private void resolveVerdicts(VirtualFile root, String[] lineShas) throws ExecutionException {
        Set<String> unknown = new LinkedHashSet<>();
        for (String sha : lineShas) {
            if (sha != null && !verdicts.containsKey(sha)) {
                unknown.add(sha);
            }
        }
        List<String> pendingShas = new ArrayList<>(unknown);
        Pattern pattern = AiLensSettings.getInstance().commitRegex();
        for (int from = 0; from < pendingShas.size(); from += SHOW_BATCH_SIZE) {
            List<String> batch = pendingShas.subList(from, Math.min(from + SHOW_BATCH_SIZE, pendingShas.size()));
            List<String> arguments = new ArrayList<>(batch.size() + 3);
            arguments.add("show");
            arguments.add("-s");
            arguments.add(SHOW_FORMAT);
            arguments.addAll(batch);
            ProcessOutput output = new CapturingProcessHandler(git(root, arguments.toArray(String[]::new)))
                    .runProcess(GIT_TIMEOUT_MILLIS);
            if (output.isTimeout() || output.getExitCode() != 0) {
                return;
            }
            for (String record : output.getStdout().split(RECORD_SEPARATOR)) {
                String[] fields = record.strip().split(FIELD_SEPARATOR, 3);
                if (fields.length == 3) {
                    verdicts.put(fields[0], label(pattern, fields[1] + "\n" + fields[2]));
                }
            }
            batch.forEach(sha -> verdicts.putIfAbsent(sha, NOT_AI));
        }
    }

    private static String label(Pattern pattern, String commitText) {
        Matcher matcher = pattern.matcher(commitText);
        return matcher.find() ? matcher.group().strip() : NOT_AI;
    }

    private static GeneralCommandLine git(VirtualFile root, String... arguments) {
        return new GeneralCommandLine(AiLensSettings.getInstance().options().gitExecutable)
                .withParameters(arguments)
                .withWorkDirectory(root.getPath())
                .withCharset(StandardCharsets.UTF_8);
    }

    private void apply(Document document, List<CommitRun> runs) {
        List<RangeHighlighter> previous = highlighters.remove(document);
        if (previous != null) {
            previous.forEach(RangeHighlighter::dispose);
        }
        if (runs.isEmpty()) {
            return;
        }
        MarkupModel markup = DocumentMarkupModel.forDocument(document, project, true);
        int lastLine = document.getLineCount() - 1;
        List<RangeHighlighter> created = new ArrayList<>(runs.size());
        for (CommitRun run : runs) {
            if (run.startLine() > lastLine) {
                continue;
            }
            RangeHighlighter highlighter = markup.addRangeHighlighter(
                    AiLensColors.COMMITTED,
                    document.getLineStartOffset(run.startLine()),
                    document.getLineEndOffset(Math.min(run.endLine(), lastLine)),
                    HIGHLIGHTER_LAYER,
                    HighlighterTargetArea.LINES_IN_RANGE);
            highlighter.setLineMarkerRenderer(AiGutterRenderer.COMMITTED);
            highlighter.setErrorStripeTooltip(
                    "AI-assisted commit " + run.sha().substring(0, SHORT_SHA_LENGTH) + ": " + run.label());
            created.add(highlighter);
        }
        highlighters.put(document, created);
    }
}
