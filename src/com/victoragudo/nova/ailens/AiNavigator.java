package com.victoragudo.nova.ailens;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.List;

final class AiNavigator {

    private final Project project;

    AiNavigator(Project project) {
        this.project = project;
    }

    boolean jump(boolean forward) {
        AiLensTracker tracker = AiLensTracker.getInstance(project);
        List<VirtualFile> files = tracker.filesWithUnreviewedLines();
        if (files.isEmpty()) {
            return false;
        }
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        VirtualFile current = editor == null ? null : FileDocumentManager.getInstance().getFile(editor.getDocument());
        int index = current == null ? -1 : files.indexOf(current);
        if (index >= 0) {
            AiSpan span = nextSpan(tracker, editor.getDocument(), editor.getCaretModel().getLogicalPosition().line, forward);
            if (span != null) {
                return open(current, span);
            }
        }
        int size = files.size();
        int start = index < 0 ? (forward ? 0 : size - 1) : Math.floorMod(index + (forward ? 1 : -1), size);
        VirtualFile target = files.get(start);
        tracker.restore(target);
        Document document = FileDocumentManager.getInstance().getDocument(target);
        if (document == null) {
            return false;
        }
        AiSpan span = nextSpan(tracker, document, forward ? -1 : Integer.MAX_VALUE, forward);
        return span != null && open(target, span);
    }

    private static AiSpan nextSpan(AiLensTracker tracker, Document document, int line, boolean forward) {
        AiSpan found = null;
        for (AiSpan span : tracker.spans(document)) {
            if (span.reviewed()) {
                continue;
            }
            if (forward && span.startLine() > line && (found == null || span.startLine() < found.startLine())) {
                found = span;
            }
            if (!forward && span.endLine() < line && (found == null || span.startLine() > found.startLine())) {
                found = span;
            }
        }
        return found;
    }

    private boolean open(VirtualFile file, AiSpan span) {
        new OpenFileDescriptor(project, file, span.startLine(), 0).navigate(true);
        return true;
    }
}
