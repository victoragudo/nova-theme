package com.victoragudo.nova.ailens;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import com.intellij.util.Consumer;

import java.awt.Component;
import java.awt.event.MouseEvent;

public final class AiLensWidgetFactory implements StatusBarWidgetFactory {

    static final String ID = "NovaAiLens";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "Nova AI Lens";
    }

    @Override
    public StatusBarWidget createWidget(Project project) {
        return new Widget(project);
    }

    private static final class Widget implements StatusBarWidget, StatusBarWidget.TextPresentation, AiLensListener {

        private final Project project;
        private StatusBar statusBar;

        Widget(Project project) {
            this.project = project;
        }

        @Override
        public String ID() {
            return ID;
        }

        @Override
        public void install(StatusBar statusBar) {
            this.statusBar = statusBar;
            project.getMessageBus().connect(this).subscribe(AiLensListener.TOPIC, this);
        }

        @Override
        public WidgetPresentation getPresentation() {
            return this;
        }

        @Override
        public void spansChanged() {
            if (statusBar != null) {
                statusBar.updateWidget(ID);
            }
        }

        @Override
        public String getText() {
            int lines = AiLensTracker.getInstance(project).unreviewedLines();
            return lines == 0 ? "" : "AI " + lines;
        }

        @Override
        public String getTooltipText() {
            int lines = AiLensTracker.getInstance(project).unreviewedLines();
            return lines + " AI-written lines to review. Click to jump to the next one.";
        }

        @Override
        public Consumer<MouseEvent> getClickConsumer() {
            return event -> new AiNavigator(project).jump(true);
        }

        @Override
        public float getAlignment() {
            return Component.CENTER_ALIGNMENT;
        }

        @Override
        public void dispose() {
            statusBar = null;
        }
    }
}
