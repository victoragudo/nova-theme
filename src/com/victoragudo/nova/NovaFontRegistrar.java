package com.victoragudo.nova;

import com.intellij.ide.AppLifecycleListener;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.colors.EditorColorsListener;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.FontPreferences;
import com.intellij.openapi.editor.colors.ModifiableFontPreferences;
import com.intellij.openapi.editor.colors.impl.AppEditorFontOptions;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.InputStream;
import java.util.List;

public final class NovaFontRegistrar implements AppLifecycleListener {

    private static final Logger LOG = Logger.getInstance(NovaFontRegistrar.class);
    private static final String FONT_FAMILY = "CommitMono";
    private static final String NOTIFICATION_GROUP = "Nova Theme";
    private static final String SCHEME_MARKER = "Nova";
    private static final String FONT_APPLIED_KEY = "com.victoragudo.nova.theme.commitmono.applied";
    private static final String FONT_PROMPTED_KEY = "com.victoragudo.nova.theme.commitmono.prompted";
    private static final String[] FONT_RESOURCES = {
            "/fonts/CommitMono-400-Regular.otf",
            "/fonts/CommitMono-400-Italic.otf",
            "/fonts/CommitMono-700-Regular.otf",
            "/fonts/CommitMono-700-Italic.otf"
    };

    @Override
    public void appFrameCreated(List<String> commandLineArgs) {
        registerFontsInAwt();
        ApplicationManager.getApplication().getMessageBus().connect()
                .subscribe(EditorColorsManager.TOPIC, (EditorColorsListener) scheme -> offerBundledFont());
        ApplicationManager.getApplication().invokeLater(this::offerBundledFont);
    }

    private void registerFontsInAwt() {
        GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (String resource : FONT_RESOURCES) {
            try (InputStream stream = NovaFontRegistrar.class.getResourceAsStream(resource)) {
                if (stream == null) {
                    LOG.warn("Nova: bundled font not found in plugin: " + resource);
                    continue;
                }
                environment.registerFont(Font.createFont(Font.TRUETYPE_FONT, stream));
            } catch (Exception e) {
                LOG.warn("Nova: failed to register font in AWT: " + resource, e);
            }
        }
    }

    private void offerBundledFont() {
        PropertiesComponent properties = PropertiesComponent.getInstance();
        if (properties.getBoolean(FONT_APPLIED_KEY, false) || properties.getBoolean(FONT_PROMPTED_KEY, false)) {
            return;
        }
        if (!isNovaSchemeActive() || !isFontAvailable()) {
            return;
        }
        if (FONT_FAMILY.equals(currentEditorFontFamily())) {
            properties.setValue(FONT_APPLIED_KEY, true);
            return;
        }
        properties.setValue(FONT_PROMPTED_KEY, true);
        Notification notification = NotificationGroupManager.getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP)
                .createNotification(
                        "Nova Theme",
                        "Nova bundles the Commit Mono coding font with ligatures. Use it as the editor font?",
                        NotificationType.INFORMATION);
        notification.addAction(NotificationAction.createSimple("Use Commit Mono", () -> {
            applyEditorFont();
            notification.expire();
        }));
        notification.addAction(NotificationAction.createSimple("Keep current font", notification::expire));
        notification.notify(null);
    }

    private static boolean isNovaSchemeActive() {
        try {
            return EditorColorsManager.getInstance().getGlobalScheme().getName().contains(SCHEME_MARKER);
        } catch (Exception e) {
            LOG.warn("Nova: cannot read the active color scheme", e);
            return false;
        }
    }

    private static boolean isFontAvailable() {
        for (String family : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            if (FONT_FAMILY.equals(family)) {
                return true;
            }
        }
        return false;
    }

    private static String currentEditorFontFamily() {
        FontPreferences preferences = AppEditorFontOptions.getInstance().getFontPreferences();
        return preferences.getFontFamily();
    }

    private void applyEditorFont() {
        try {
            FontPreferences preferences = AppEditorFontOptions.getInstance().getFontPreferences();
            if (!(preferences instanceof ModifiableFontPreferences modifiable)) {
                return;
            }
            int size = preferences.getSize(preferences.getFontFamily());
            modifiable.clearFonts();
            modifiable.register(FONT_FAMILY, size);
            modifiable.setUseLigatures(true);
            PropertiesComponent.getInstance().setValue(FONT_APPLIED_KEY, true);
            LOG.info("Nova: CommitMono selected as editor font");
        } catch (Exception e) {
            LOG.warn("Nova: failed to apply CommitMono as editor font", e);
        }
    }
}
