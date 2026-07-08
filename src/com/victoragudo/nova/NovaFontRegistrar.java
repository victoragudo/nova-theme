package com.victoragudo.nova;

import com.intellij.ide.AppLifecycleListener;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.colors.FontPreferences;
import com.intellij.openapi.editor.colors.ModifiableFontPreferences;
import com.intellij.openapi.editor.colors.impl.AppEditorFontOptions;
import com.intellij.openapi.util.SystemInfo;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public final class NovaFontRegistrar implements AppLifecycleListener {

    private static final Logger LOG = Logger.getInstance(NovaFontRegistrar.class);
    private static final String FONT_FAMILY = "CommitMono";
    private static final String FONT_APPLIED_KEY = "com.victoragudo.nova.theme.commitmono.applied";
    private static final String[] FONT_RESOURCES = {
            "/fonts/CommitMono-400-Regular.otf",
            "/fonts/CommitMono-400-Italic.otf",
            "/fonts/CommitMono-700-Regular.otf",
            "/fonts/CommitMono-700-Italic.otf"
    };

    @Override
    public void appFrameCreated(List<String> commandLineArgs) {
        LOG.info("Nova: installing bundled CommitMono fonts");
        installFontsIntoUserFontsDir();
        registerFontsInAwt();
        applyEditorFontOnce();
    }

    private void installFontsIntoUserFontsDir() {
        Path fontsDir = userFontsDir();
        if (fontsDir == null) {
            LOG.info("Nova: no user fonts directory for this OS, relying on AWT registration only");
            return;
        }
        try {
            Files.createDirectories(fontsDir);
        } catch (Exception e) {
            LOG.warn("Nova: cannot create fonts directory " + fontsDir, e);
            return;
        }
        for (String resource : FONT_RESOURCES) {
            String fileName = resource.substring(resource.lastIndexOf('/') + 1);
            Path target = fontsDir.resolve(fileName);
            if (Files.exists(target)) {
                continue;
            }
            try (InputStream stream = NovaFontRegistrar.class.getResourceAsStream(resource)) {
                if (stream == null) {
                    LOG.warn("Nova: bundled font not found in plugin: " + resource);
                    continue;
                }
                Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
                LOG.info("Nova: installed " + target);
            } catch (Exception e) {
                LOG.warn("Nova: failed to install font " + fileName + " into " + fontsDir, e);
            }
        }
    }

    private static Path userFontsDir() {
        String home = System.getProperty("user.home");
        if (SystemInfo.isMac) {
            return Path.of(home, "Library", "Fonts");
        }
        if (SystemInfo.isLinux) {
            return Path.of(home, ".local", "share", "fonts");
        }
        return null;
    }

    private void registerFontsInAwt() {
        GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (String resource : FONT_RESOURCES) {
            try (InputStream stream = NovaFontRegistrar.class.getResourceAsStream(resource)) {
                if (stream == null) {
                    continue;
                }
                Font font = Font.createFont(Font.TRUETYPE_FONT, stream);
                environment.registerFont(font);
            } catch (Exception e) {
                LOG.warn("Nova: failed to register font in AWT: " + resource, e);
            }
        }
        LOG.info("Nova: AWT registration done, CommitMono available in AWT: " + awtKnowsFamily());
    }

    private static boolean awtKnowsFamily() {
        for (String family : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            if (FONT_FAMILY.equals(family)) {
                return true;
            }
        }
        return false;
    }

    private void applyEditorFontOnce() {
        PropertiesComponent properties = PropertiesComponent.getInstance();
        if (properties.getBoolean(FONT_APPLIED_KEY, false)) {
            return;
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                FontPreferences preferences = AppEditorFontOptions.getInstance().getFontPreferences();
                if (!(preferences instanceof ModifiableFontPreferences modifiable)) {
                    return;
                }
                int size = preferences.getSize(preferences.getFontFamily());
                modifiable.clearFonts();
                modifiable.register(FONT_FAMILY, size);
                modifiable.setUseLigatures(true);
                properties.setValue(FONT_APPLIED_KEY, true);
                LOG.info("Nova: CommitMono selected as editor font");
            } catch (Exception e) {
                LOG.warn("Nova: failed to apply CommitMono as editor font", e);
            }
        });
    }
}
