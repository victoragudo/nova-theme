package com.victoragudo.nova.ailens;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.CommitContext;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory;

public final class AiLensCheckinHandlerFactory extends CheckinHandlerFactory {

    private static final String TITLE = "Nova AI Lens";
    private static final String COMMIT_ANYWAY = "Commit Anyway";
    private static final String REVIEW_FIRST = "Review First";

    @Override
    public CheckinHandler createHandler(CheckinProjectPanel panel, CommitContext commitContext) {
        return new Handler(panel);
    }

    private static final class Handler extends CheckinHandler {

        private final CheckinProjectPanel panel;

        Handler(CheckinProjectPanel panel) {
            this.panel = panel;
        }

        @Override
        public ReturnResult beforeCheckin() {
            Project project = panel.getProject();
            if (!AiLensSettings.getInstance().options().confirmCommit) {
                return ReturnResult.COMMIT;
            }
            int lines = AiLensTracker.getInstance(project).unreviewedLines(panel.getVirtualFiles());
            if (lines == 0) {
                return ReturnResult.COMMIT;
            }
            String message = lines + " AI-written lines in this commit have not been reviewed yet.";
            int choice = Messages.showOkCancelDialog(
                    project, message, TITLE, COMMIT_ANYWAY, REVIEW_FIRST, Messages.getWarningIcon());
            if (choice == Messages.OK) {
                return ReturnResult.COMMIT;
            }
            new AiNavigator(project).jump(true);
            return ReturnResult.CLOSE_WINDOW;
        }
    }
}
