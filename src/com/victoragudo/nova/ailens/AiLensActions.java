package com.victoragudo.nova.ailens;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.DumbAwareToggleAction;
import com.intellij.openapi.project.Project;

public final class AiLensActions {

    private AiLensActions() {
    }

    private record LineRange(int start, int end) {

        static LineRange of(Editor editor) {
            Document document = editor.getDocument();
            SelectionModel selection = editor.getSelectionModel();
            if (!selection.hasSelection()) {
                int line = editor.getCaretModel().getLogicalPosition().line;
                return new LineRange(line, line);
            }
            int start = selection.getSelectionStart();
            int end = Math.max(start, selection.getSelectionEnd() - 1);
            return new LineRange(document.getLineNumber(start), document.getLineNumber(end));
        }
    }

    private abstract static class EditorAction extends DumbAwareAction {

        @Override
        public ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }

        @Override
        public void update(AnActionEvent event) {
            Editor editor = event.getData(CommonDataKeys.EDITOR);
            Project project = event.getProject();
            event.getPresentation().setEnabledAndVisible(
                    editor != null && project != null && isApplicable(AiLensTracker.getInstance(project), editor));
        }

        @Override
        public void actionPerformed(AnActionEvent event) {
            Editor editor = event.getData(CommonDataKeys.EDITOR);
            Project project = event.getProject();
            if (editor != null && project != null) {
                perform(AiLensTracker.getInstance(project), editor);
            }
        }

        boolean isApplicable(AiLensTracker tracker, Editor editor) {
            return !tracker.spans(editor.getDocument()).isEmpty();
        }

        abstract void perform(AiLensTracker tracker, Editor editor);
    }

    public static final class MarkReviewed extends EditorAction {

        @Override
        boolean isApplicable(AiLensTracker tracker, Editor editor) {
            return tracker.unreviewedLines(editor.getDocument()) > 0;
        }

        @Override
        void perform(AiLensTracker tracker, Editor editor) {
            Document document = editor.getDocument();
            if (editor.getSelectionModel().hasSelection()) {
                LineRange range = LineRange.of(editor);
                tracker.review(document, range.start(), range.end());
                return;
            }
            AiSpan span = tracker.spanAt(document, editor.getCaretModel().getLogicalPosition().line);
            if (span != null) {
                tracker.review(document, span.startLine(), span.endLine());
            }
        }
    }

    public static final class MarkFileReviewed extends EditorAction {

        @Override
        boolean isApplicable(AiLensTracker tracker, Editor editor) {
            return tracker.unreviewedLines(editor.getDocument()) > 0;
        }

        @Override
        void perform(AiLensTracker tracker, Editor editor) {
            tracker.reviewAll(editor.getDocument());
        }
    }

    public static final class MarkAiWritten extends EditorAction {

        @Override
        boolean isApplicable(AiLensTracker tracker, Editor editor) {
            return true;
        }

        @Override
        void perform(AiLensTracker tracker, Editor editor) {
            LineRange range = LineRange.of(editor);
            tracker.mark(editor.getDocument(), range.start(), range.end(), AiOrigin.MANUAL);
        }
    }

    public static final class ClearFile extends EditorAction {

        @Override
        void perform(AiLensTracker tracker, Editor editor) {
            tracker.clearAll(editor.getDocument());
        }
    }

    private abstract static class JumpAction extends DumbAwareAction {

        private final boolean forward;

        JumpAction(boolean forward) {
            this.forward = forward;
        }

        @Override
        public ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }

        @Override
        public void update(AnActionEvent event) {
            Project project = event.getProject();
            event.getPresentation().setEnabled(project != null && AiLensTracker.getInstance(project).unreviewedLines() > 0);
        }

        @Override
        public void actionPerformed(AnActionEvent event) {
            Project project = event.getProject();
            if (project != null) {
                new AiNavigator(project).jump(forward);
            }
        }
    }

    public static final class NextUnreviewed extends JumpAction {

        public NextUnreviewed() {
            super(true);
        }
    }

    public static final class PreviousUnreviewed extends JumpAction {

        public PreviousUnreviewed() {
            super(false);
        }
    }

    public static final class ToggleCommitLens extends DumbAwareToggleAction {

        @Override
        public ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }

        @Override
        public boolean isSelected(AnActionEvent event) {
            return AiLensSettings.getInstance().options().highlightAiCommits;
        }

        @Override
        public void setSelected(AnActionEvent event, boolean state) {
            AiLensSettings.getInstance().options().highlightAiCommits = state;
            AiCommitLens.refreshOpenProjects(false);
        }
    }
}
