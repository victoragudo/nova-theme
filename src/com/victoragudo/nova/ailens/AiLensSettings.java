package com.victoragudo.nova.ailens;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Service(Service.Level.APP)
@State(name = "NovaAiLensSettings", storages = @Storage("nova-ai-lens.xml"))
public final class AiLensSettings implements PersistentStateComponent<AiLensSettings.Options> {

    static final String DEFAULT_COMMIT_PATTERN =
            "(?im)^.*(co-authored-by:.*\\b(claude|anthropic|copilot|cursor|codex|openai|gemini|aider|junie|devin|windsurf|codeium)\\b"
                    + "|generated with \\[?claude code|\\(aider\\)).*$";
    static final String DEFAULT_ASSISTANT_PATTERN =
            "(?i)inlinecompletion|inline completion|copilot|codeium|windsurf|tabnine|supermaven|junie|fullline|full line"
                    + "|next edit|nextedit|aiassistant|ai assistant|llm|claude|cody";

    public static final class Options {
        public boolean trackAssistantEdits = true;
        public boolean trackExternalEdits = true;
        public boolean reviewOnCaret = true;
        public int reviewDwellMillis = 700;
        public boolean confirmCommit = true;
        public boolean highlightAiCommits = true;
        public int externalBatchLimit = 25;
        public String gitExecutable = "git";
        public String commitPattern = DEFAULT_COMMIT_PATTERN;
        public String assistantPattern = DEFAULT_ASSISTANT_PATTERN;
    }

    private Options options = new Options();
    private Pattern commitRegex;
    private Pattern assistantRegex;

    public static AiLensSettings getInstance() {
        return ApplicationManager.getApplication().getService(AiLensSettings.class);
    }

    @Override
    public Options getState() {
        return options;
    }

    @Override
    public void loadState(Options state) {
        options = state;
        commitRegex = null;
        assistantRegex = null;
    }

    Options options() {
        return options;
    }

    Pattern commitRegex() {
        if (commitRegex == null) {
            commitRegex = compile(options.commitPattern, DEFAULT_COMMIT_PATTERN);
        }
        return commitRegex;
    }

    Pattern assistantRegex() {
        if (assistantRegex == null) {
            assistantRegex = compile(options.assistantPattern, DEFAULT_ASSISTANT_PATTERN);
        }
        return assistantRegex;
    }

    static boolean isValidPattern(String pattern) {
        try {
            Pattern.compile(pattern);
            return true;
        } catch (PatternSyntaxException e) {
            return false;
        }
    }

    private static Pattern compile(String pattern, String fallback) {
        return Pattern.compile(isValidPattern(pattern) ? pattern : fallback);
    }
}
