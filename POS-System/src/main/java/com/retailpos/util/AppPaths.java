package com.retailpos.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Central location for writable, per-user application data.
 *
 * <p>Path resolution per platform:
 * <ul>
 *   <li><b>Linux / Unix</b> — honours the XDG Base Directory Specification.
 *       Data lives in {@code $XDG_DATA_HOME/RetailPOS}
 *       (default {@code ~/.local/share/RetailPOS}).
 *       If the JVM system property {@code app.data.dir} is set (injected by
 *       the launcher script), that path is used directly instead.</li>
 *   <li><b>Windows</b> — uses {@code %LOCALAPPDATA%\RetailPOS}
 *       (default {@code %USERPROFILE%\AppData\Local\RetailPOS}).</li>
 *   <li><b>macOS</b> — uses {@code ~/Library/Application Support/RetailPOS}.</li>
 * </ul>
 */
public final class AppPaths {
    private static final String APP_DIRECTORY = "RetailPOS";

    private AppPaths() { }

    public static Path dataDirectory() {
        // Allow the launcher (or tests) to override via system property
        String override = System.getProperty("app.data.dir");
        if (override != null && !override.isBlank()) {
            return Path.of(override);
        }

        String os = System.getProperty("os.name", "").toLowerCase();
        String home = System.getProperty("user.home");

        if (os.contains("win")) {
            // Windows — %LOCALAPPDATA%\RetailPOS
            String localAppData = System.getenv("LOCALAPPDATA");
            Path base = (localAppData != null && !localAppData.isBlank())
                    ? Path.of(localAppData)
                    : Path.of(home, "AppData", "Local");
            return base.resolve(APP_DIRECTORY);

        } else if (os.contains("mac")) {
            // macOS — ~/Library/Application Support/RetailPOS
            return Path.of(home, "Library", "Application Support", APP_DIRECTORY);

        } else {
            // Linux / Unix — XDG Base Directory Specification
            // $XDG_DATA_HOME defaults to ~/.local/share
            String xdgDataHome = System.getenv("XDG_DATA_HOME");
            Path base = (xdgDataHome != null && !xdgDataHome.isBlank())
                    ? Path.of(xdgDataHome)
                    : Path.of(home, ".local", "share");
            return base.resolve(APP_DIRECTORY);
        }
    }

    public static Path databaseFile() throws IOException {
        return ensureDirectory(dataDirectory()).resolve("retail_pos.db");
    }

    public static Path backupDirectory() throws IOException {
        return ensureDirectory(dataDirectory().resolve("backups"));
    }

    public static Path imageDirectory() throws IOException {
        return ensureDirectory(dataDirectory().resolve("images"));
    }

    public static Path receiptDirectory() throws IOException {
        return ensureDirectory(dataDirectory().resolve("receipts"));
    }

    public static Path copyImage(Path source, String prefix) throws IOException {
        String fileName = source.getFileName().toString();
        int extensionIndex = fileName.lastIndexOf('.');
        String extension = extensionIndex >= 0 ? fileName.substring(extensionIndex) : ".img";
        Path destination = imageDirectory().resolve(prefix + "-" + UUID.randomUUID() + extension.toLowerCase());
        return Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
    }

    public static Path resolveDataPath(String configuredPath) throws IOException {
        Path path = configuredPath == null || configuredPath.isBlank()
            ? backupDirectory() : Path.of(configuredPath);
        return ensureDirectory(path.isAbsolute() ? path : dataDirectory().resolve(path));
    }

    private static Path ensureDirectory(Path directory) throws IOException {
        return Files.createDirectories(directory);
    }
}
