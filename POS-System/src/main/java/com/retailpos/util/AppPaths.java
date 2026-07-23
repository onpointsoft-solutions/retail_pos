package com.retailpos.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/** Central location for writable, per-user application data. */
public final class AppPaths {
    private static final String APP_DIRECTORY = "RetailPOS";

    private AppPaths() { }

    public static Path dataDirectory() {
        String localAppData = System.getenv("LOCALAPPDATA");
        Path baseDirectory = localAppData == null || localAppData.isBlank()
            ? Path.of(System.getProperty("user.home"), "AppData", "Local")
            : Path.of(localAppData);
        return baseDirectory.resolve(APP_DIRECTORY);
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
