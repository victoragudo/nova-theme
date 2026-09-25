package com.victoragudo.nova.ailens;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

public final class AiEditorOpenWatcher implements FileEditorManagerListener {

    private final Project project;

    public AiEditorOpenWatcher(Project project) {
        this.project = project;
    }

    @Override
    public void fileOpened(FileEditorManager source, VirtualFile file) {
        AiReviewWatcher.getInstance(project);
        AiLensTracker.getInstance(project).restore(file);
        AiCommitLens.getInstance(project).scheduleRefresh(file);
    }
}
