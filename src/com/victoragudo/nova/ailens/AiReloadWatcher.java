package com.victoragudo.nova.ailens;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManagerListener;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;

import java.util.List;

public final class AiReloadWatcher implements FileDocumentManagerListener, BulkFileListener {

    @Override
    public void beforeFileContentReload(VirtualFile file, Document document) {
        AiEditRecorder.getInstance().beforeReload(document);
    }

    @Override
    public void fileContentReloaded(VirtualFile file, Document document) {
        AiEditRecorder.getInstance().afterReload(document);
    }

    @Override
    public void before(List<? extends VFileEvent> events) {
        AiEditRecorder recorder = AiEditRecorder.getInstance();
        if (recorder.observeFileEvents(events)) {
            AiCommitLens.refreshOpenProjects(false);
        }
        recorder.snapshotClosedFiles(events);
    }

    @Override
    public void after(List<? extends VFileEvent> events) {
        AiEditRecorder.getInstance().recordClosedFiles();
    }
}
