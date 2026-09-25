package com.victoragudo.nova.ailens;

import com.intellij.openapi.editor.colors.TextAttributesKey;

public final class AiLensColors {

    public static final TextAttributesKey UNREVIEWED = TextAttributesKey.createTextAttributesKey("NOVA_AI_UNREVIEWED");
    public static final TextAttributesKey REVIEWED = TextAttributesKey.createTextAttributesKey("NOVA_AI_REVIEWED");
    public static final TextAttributesKey COMMITTED = TextAttributesKey.createTextAttributesKey("NOVA_AI_COMMITTED");

    private AiLensColors() {
    }

    static TextAttributesKey forSpan(AiSpan span) {
        return span.reviewed() ? REVIEWED : UNREVIEWED;
    }
}
