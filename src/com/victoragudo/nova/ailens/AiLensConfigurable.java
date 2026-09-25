package com.victoragudo.nova.ailens;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.util.Objects;

public final class AiLensConfigurable implements Configurable {

    private static final AiLensSettings.Options DEFAULTS = new AiLensSettings.Options();
    private static final int MIN_DWELL_MILLIS = 100;
    private static final int MAX_DWELL_MILLIS = 10_000;
    private static final int DWELL_STEP_MILLIS = 100;
    private static final int MIN_BATCH = 1;
    private static final int MAX_BATCH = 1_000;

    private final JBCheckBox trackAssistantEdits = new JBCheckBox("Track code inserted by AI assistants in the IDE");
    private final JBCheckBox trackExternalEdits = new JBCheckBox("Track files changed on disk by external AI agents");
    private final JBCheckBox reviewOnCaret = new JBCheckBox("Mark a line as reviewed when the caret rests on it");
    private final JSpinner reviewDwell = new JSpinner(
            new SpinnerNumberModel(DEFAULTS.reviewDwellMillis, MIN_DWELL_MILLIS, MAX_DWELL_MILLIS, DWELL_STEP_MILLIS));
    private final JSpinner externalBatchLimit = new JSpinner(new SpinnerNumberModel(DEFAULTS.externalBatchLimit, MIN_BATCH, MAX_BATCH, 1));
    private final JBCheckBox confirmCommit = new JBCheckBox("Ask before committing AI-written lines that are not reviewed");
    private final JBCheckBox highlightAiCommits = new JBCheckBox("Highlight lines from AI-assisted commits");
    private final JBTextField gitExecutable = new JBTextField();
    private final JBTextField commitPattern = new JBTextField();
    private final JBTextField assistantPattern = new JBTextField();

    @Override
    public String getDisplayName() {
        return "Nova AI Lens";
    }

    @Override
    public JComponent createComponent() {
        return FormBuilder.createFormBuilder()
                .addComponent(trackAssistantEdits)
                .addLabeledComponent("Assistant action and command pattern:", assistantPattern)
                .addComponent(trackExternalEdits)
                .addLabeledComponent("Ignore refreshes touching more files than:", externalBatchLimit)
                .addSeparator()
                .addComponent(reviewOnCaret)
                .addLabeledComponent("Caret rest time (ms):", reviewDwell)
                .addComponent(confirmCommit)
                .addSeparator()
                .addComponent(highlightAiCommits)
                .addLabeledComponent("AI commit pattern:", commitPattern)
                .addLabeledComponent("Git executable:", gitExecutable)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    @Override
    public boolean isModified() {
        AiLensSettings.Options options = AiLensSettings.getInstance().options();
        return options.trackAssistantEdits != trackAssistantEdits.isSelected()
                || options.trackExternalEdits != trackExternalEdits.isSelected()
                || options.reviewOnCaret != reviewOnCaret.isSelected()
                || options.reviewDwellMillis != (int) reviewDwell.getValue()
                || options.externalBatchLimit != (int) externalBatchLimit.getValue()
                || options.confirmCommit != confirmCommit.isSelected()
                || options.highlightAiCommits != highlightAiCommits.isSelected()
                || !Objects.equals(options.gitExecutable, gitExecutable.getText().strip())
                || !Objects.equals(options.commitPattern, commitPattern.getText())
                || !Objects.equals(options.assistantPattern, assistantPattern.getText());
    }

    @Override
    public void apply() throws ConfigurationException {
        if (!AiLensSettings.isValidPattern(commitPattern.getText())) {
            throw new ConfigurationException("The AI commit pattern is not a valid regular expression.");
        }
        if (!AiLensSettings.isValidPattern(assistantPattern.getText())) {
            throw new ConfigurationException("The assistant pattern is not a valid regular expression.");
        }
        AiLensSettings.Options options = new AiLensSettings.Options();
        options.trackAssistantEdits = trackAssistantEdits.isSelected();
        options.trackExternalEdits = trackExternalEdits.isSelected();
        options.reviewOnCaret = reviewOnCaret.isSelected();
        options.reviewDwellMillis = (int) reviewDwell.getValue();
        options.externalBatchLimit = (int) externalBatchLimit.getValue();
        options.confirmCommit = confirmCommit.isSelected();
        options.highlightAiCommits = highlightAiCommits.isSelected();
        options.gitExecutable = gitExecutable.getText().strip();
        options.commitPattern = commitPattern.getText();
        options.assistantPattern = assistantPattern.getText();
        AiLensSettings.getInstance().loadState(options);
        AiCommitLens.refreshOpenProjects(true);
    }

    @Override
    public void reset() {
        AiLensSettings.Options options = AiLensSettings.getInstance().options();
        trackAssistantEdits.setSelected(options.trackAssistantEdits);
        trackExternalEdits.setSelected(options.trackExternalEdits);
        reviewOnCaret.setSelected(options.reviewOnCaret);
        reviewDwell.setValue(options.reviewDwellMillis);
        externalBatchLimit.setValue(options.externalBatchLimit);
        confirmCommit.setSelected(options.confirmCommit);
        highlightAiCommits.setSelected(options.highlightAiCommits);
        gitExecutable.setText(options.gitExecutable);
        commitPattern.setText(options.commitPattern);
        assistantPattern.setText(options.assistantPattern);
    }
}
