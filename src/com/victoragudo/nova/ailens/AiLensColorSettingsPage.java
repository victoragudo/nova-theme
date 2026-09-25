package com.victoragudo.nova.ailens;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.PlainSyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;

import javax.swing.Icon;
import java.util.Map;

public final class AiLensColorSettingsPage implements ColorSettingsPage {

    private static final AttributesDescriptor[] DESCRIPTORS = {
            new AttributesDescriptor("AI-written code//Not reviewed", AiLensColors.UNREVIEWED),
            new AttributesDescriptor("AI-written code//Reviewed", AiLensColors.REVIEWED),
            new AttributesDescriptor("AI-written code//From an AI-assisted commit", AiLensColors.COMMITTED),
    };

    private static final Map<String, TextAttributesKey> TAGS = Map.of(
            "unreviewed", AiLensColors.UNREVIEWED,
            "reviewed", AiLensColors.REVIEWED,
            "committed", AiLensColors.COMMITTED);

    private static final String DEMO = """
            func (s *InvoiceService) Settle(ctx context.Context, id string) error {
            <committed>    invoice, err := s.repo.Find(ctx, id)
                if err != nil {
                    return fmt.Errorf("find invoice %s: %w", id, err)
                }</committed>
            <reviewed>    if invoice.Paid() {
                    return ErrAlreadySettled
                }</reviewed>
            <unreviewed>    invoice.MarkPaid(s.clock.Now())
                return s.repo.Save(ctx, invoice)</unreviewed>
            }
            """;

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public SyntaxHighlighter getHighlighter() {
        return new PlainSyntaxHighlighter();
    }

    @Override
    public String getDemoText() {
        return DEMO;
    }

    @Override
    public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return TAGS;
    }

    @Override
    public AttributesDescriptor[] getAttributeDescriptors() {
        return DESCRIPTORS;
    }

    @Override
    public ColorDescriptor[] getColorDescriptors() {
        return ColorDescriptor.EMPTY_ARRAY;
    }

    @Override
    public String getDisplayName() {
        return "Nova AI Lens";
    }
}
