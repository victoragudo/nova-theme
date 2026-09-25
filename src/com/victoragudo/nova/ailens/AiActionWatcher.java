package com.victoragudo.nova.ailens;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.AnActionResult;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.command.CommandEvent;
import com.intellij.openapi.command.CommandListener;

public final class AiActionWatcher implements AnActionListener, CommandListener {

    private static final String UNDO_PREFIX = "Undo";
    private static final String REDO_PREFIX = "Redo";

    @Override
    public void beforeActionPerformed(AnAction action, AnActionEvent event) {
        AiEditRecorder.getInstance().enterIfAssistant(signature(action));
    }

    @Override
    public void afterActionPerformed(AnAction action, AnActionEvent event, AnActionResult result) {
        AiEditRecorder.getInstance().exitIfAssistant(signature(action));
    }

    @Override
    public void beforeEditorTyping(char c, DataContext dataContext) {
        AiEditRecorder.getInstance().userTyped();
    }

    @Override
    public void commandStarted(CommandEvent event) {
        AiEditRecorder.getInstance().enterIfAssistant(signature(event));
    }

    @Override
    public void commandFinished(CommandEvent event) {
        AiEditRecorder.getInstance().exitIfAssistant(signature(event));
    }

    private static String signature(AnAction action) {
        return ActionManager.getInstance().getId(action) + " " + action.getClass().getName();
    }

    private static String signature(CommandEvent event) {
        String name = event.getCommandName();
        if (name == null || name.startsWith(UNDO_PREFIX) || name.startsWith(REDO_PREFIX)) {
            return null;
        }
        return name;
    }
}
