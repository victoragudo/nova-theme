package com.victoragudo.nova.ailens;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.LineMarkerRendererEx;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.ui.scale.JBUIScale;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

final class AiGutterRenderer implements LineMarkerRendererEx {

    static final AiGutterRenderer UNREVIEWED = new AiGutterRenderer(AiLensColors.UNREVIEWED, 3, 0);
    static final AiGutterRenderer REVIEWED = new AiGutterRenderer(AiLensColors.REVIEWED, 2, 0);
    static final AiGutterRenderer COMMITTED = new AiGutterRenderer(AiLensColors.COMMITTED, 2, 3);

    private final TextAttributesKey key;
    private final int width;
    private final int dash;

    private AiGutterRenderer(TextAttributesKey key, int width, int dash) {
        this.key = key;
        this.width = width;
        this.dash = dash;
    }

    static AiGutterRenderer forSpan(AiSpan span) {
        return span.reviewed() ? REVIEWED : UNREVIEWED;
    }

    @Override
    public void paint(Editor editor, Graphics g, Rectangle r) {
        TextAttributes attributes = editor.getColorsScheme().getAttributes(key);
        Color color = attributes == null ? null : attributes.getErrorStripeColor();
        if (color == null) {
            return;
        }
        g.setColor(color);
        int scaledWidth = JBUIScale.scale(width);
        if (dash == 0) {
            g.fillRect(r.x, r.y, scaledWidth, r.height);
            return;
        }
        int step = JBUIScale.scale(dash);
        for (int y = r.y; y < r.y + r.height; y += step * 2) {
            g.fillRect(r.x, y, scaledWidth, Math.min(step, r.y + r.height - y));
        }
    }

    @Override
    public Position getPosition() {
        return Position.RIGHT;
    }
}
