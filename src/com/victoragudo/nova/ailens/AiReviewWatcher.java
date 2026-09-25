package com.victoragudo.nova.ailens;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.project.Project;
import com.intellij.util.Alarm;

@Service(Service.Level.PROJECT)
public final class AiReviewWatcher implements CaretListener, Disposable {

    private final Project project;
    private final Alarm alarm;

    public AiReviewWatcher(Project project) {
        this.project = project;
        this.alarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, this);
        EditorFactory.getInstance().getEventMulticaster().addCaretListener(this, this);
    }

    public static AiReviewWatcher getInstance(Project project) {
        return project.getService(AiReviewWatcher.class);
    }

    @Override
    public void caretPositionChanged(CaretEvent event) {
        Editor editor = event.getEditor();
        AiLensSettings.Options options = AiLensSettings.getInstance().options();
        if (editor.getProject() != project || !options.reviewOnCaret
                || ApplicationManager.getApplication().isWriteAccessAllowed()) {
            return;
        }
        int line = event.getNewPosition().line;
        Document document = editor.getDocument();
        AiSpan span = AiLensTracker.getInstance(project).spanAt(document, line);
        alarm.cancelAllRequests();
        if (span == null || span.reviewed()) {
            return;
        }
        alarm.addRequest(() -> reviewIfStill(editor, line), options.reviewDwellMillis);
    }

    @Override
    public void dispose() {
    }

    private void reviewIfStill(Editor editor, int line) {
        if (editor.isDisposed() || editor.getCaretModel().getLogicalPosition().line != line) {
            return;
        }
        AiLensTracker.getInstance(project).review(editor.getDocument(), line, line);
    }
}
