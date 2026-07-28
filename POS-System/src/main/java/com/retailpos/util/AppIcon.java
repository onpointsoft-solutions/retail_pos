package com.retailpos.util;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.Taskbar;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public final class AppIcon {
    private static final int[] SIZES = {16, 24, 32, 48, 64, 128, 256};
    private static final List<Image> IMAGES = loadImages();

    private AppIcon() {
    }

    public static void apply(Window window) {
        if (window != null && !IMAGES.isEmpty()) {
            window.setIconImages(IMAGES);
        }
    }

    public static void applyToTaskbar() {
        if (IMAGES.isEmpty() || !Taskbar.isTaskbarSupported()) {
            return;
        }
        try {
            Taskbar taskbar = Taskbar.getTaskbar();
            if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                taskbar.setIconImage(IMAGES.get(IMAGES.size() - 1));
            }
        } catch (UnsupportedOperationException | SecurityException ignored) {
        }
    }

    private static List<Image> loadImages() {
        try (InputStream stream = AppIcon.class.getResourceAsStream("/app-icon.png")) {
            if (stream == null) {
                return List.of();
            }
            BufferedImage source = ImageIO.read(stream);
            if (source == null) {
                return List.of();
            }
            List<Image> images = new ArrayList<>(SIZES.length);
            for (int size : SIZES) {
                images.add(source.getScaledInstance(size, size, Image.SCALE_SMOOTH));
            }
            return List.copyOf(images);
        } catch (Exception ignored) {
            return List.of();
        }
    }
}
