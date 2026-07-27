package com.retailpos.service;

import com.google.gson.Gson;
import com.retailpos.model.AppSettings;
import com.retailpos.repository.SettingsRepository;
import com.retailpos.util.DatabaseManager;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class LicenseService {
    public enum Status { TRIAL, ACTIVE, GRACE, EXPIRED, INVALID }

    public static final int TRIAL_DAYS = 30;
    public static final int OFFLINE_GRACE_DAYS = 7;
    private static final Duration VALIDATION_INTERVAL = Duration.ofHours(24);
    private static final LicenseService INSTANCE = new LicenseService();
    private final Gson gson = new Gson();

    private LicenseService() {}

    public static LicenseService getInstance() {
        return INSTANCE;
    }

    public synchronized LicenseSnapshot checkAccess() {
        ensureLocalState();
        String licenseKey = getSetting("license_key");
        if (isBlank(licenseKey)) {
            return checkTrial();
        }

        Instant now = Instant.now();
        Instant expiresAt = parseInstant(getSetting("license_expires_at"));
        Instant lastValidated = parseInstant(getSetting("license_last_validated_at"));
        boolean machineChanged = !getMachineId().equals(getSetting("license_machine_id"));
        boolean validationDue = lastValidated == null
            || machineChanged
            || now.isBefore(lastValidated.minus(Duration.ofMinutes(5)))
            || Duration.between(lastValidated, now).compareTo(VALIDATION_INTERVAL) >= 0
            || expiresAt == null
            || !expiresAt.isAfter(now);

        if (validationDue) {
            try {
                return validatePaidLicense(licenseKey, getApiUrl());
            } catch (Exception exception) {
                if (expiresAt != null && expiresAt.isAfter(now) && lastValidated != null
                    && now.isBefore(lastValidated.plus(Duration.ofDays(OFFLINE_GRACE_DAYS)))) {
                    return snapshot(
                        Status.GRACE,
                        getSetting("license_plan_name", "Licensed"),
                        expiresAt,
                        "Offline grace period. Connect to the internet to validate your license."
                    );
                }
                return snapshot(
                    Status.EXPIRED,
                    getSetting("license_plan_name", "License"),
                    expiresAt,
                    "License validation is required. " + friendlyMessage(exception)
                );
            }
        }

        return snapshot(
            Status.ACTIVE,
            getSetting("license_plan_name", "Licensed"),
            expiresAt,
            "License active"
        );
    }

    public synchronized LicenseSnapshot activate(String licenseKey, String apiUrl) throws Exception {
        if (isBlank(licenseKey)) throw new IllegalArgumentException("Enter a BizFlow POS license key.");
        String normalizedUrl = normalizeApiUrl(apiUrl);
        if (isBlank(normalizedUrl)) throw new IllegalArgumentException("Enter the licensing backend URL.");

        AppSettings settings = new SettingsRepository().load();
        Map<String, Object> body = baseRequest();
        body.put("license_key", licenseKey.trim().toUpperCase());
        body.put("store_name", settings.getStoreName());
        Map<String, Object> response = post(normalizedUrl + "license/activate", body);
        savePaidResponse(licenseKey.trim().toUpperCase(), normalizedUrl, response);
        return paidSnapshot(response, Status.ACTIVE, "Activation successful");
    }

    public synchronized LicenseSnapshot refreshNow() throws Exception {
        String licenseKey = getSetting("license_key");
        if (isBlank(licenseKey)) return checkTrial();
        return validatePaidLicense(licenseKey, getApiUrl());
    }

    public String getMachineId() {
        try {
            String computer = System.getenv().getOrDefault("COMPUTERNAME", "");
            String processor = System.getenv().getOrDefault("PROCESSOR_IDENTIFIER", "");
            String source = computer + "|" + processor + "|" + System.getProperty("os.name", "")
                + "|" + System.getProperty("os.arch", "") + "|" + System.getProperty("user.home", "");
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder value = new StringBuilder();
            for (byte item : digest) value.append(String.format("%02x", item));
            return value.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not identify this workstation", exception);
        }
    }

    public String getApiUrl() {
        try {
            return normalizeApiUrl(new SettingsRepository().load().getSyncApiUrl());
        } catch (Exception exception) {
            return "";
        }
    }

    private LicenseSnapshot checkTrial() {
        Instant startedAt = parseInstant(getSetting("trial_started_at"));
        if (startedAt == null) {
            startedAt = Instant.now();
            putSetting("trial_started_at", startedAt.toString());
        }
        if (Instant.now().isBefore(startedAt.minus(Duration.ofMinutes(5)))) {
            return snapshot(
                Status.INVALID,
                "Free Trial",
                startedAt,
                "The workstation clock is earlier than the recorded trial start. Correct the date and time to continue."
            );
        }

        Instant lastTrialCheck = parseInstant(getSetting("trial_last_checked_at"));
        boolean trialCheckDue = lastTrialCheck == null
            || Duration.between(lastTrialCheck, Instant.now()).compareTo(VALIDATION_INTERVAL) >= 0;
        if (trialCheckDue) {
            try {
                Map<String, Object> request = baseRequest();
                request.put("trial_started_at", startedAt.toString());
                Map<String, Object> response = post(getApiUrl() + "license/trial", request);
                Instant serverStarted = parseInstant(String.valueOf(response.get("trial_started_at")));
                if (serverStarted != null && serverStarted.isBefore(startedAt)) {
                    startedAt = serverStarted;
                    putSetting("trial_started_at", startedAt.toString());
                }
                putSetting("trial_last_checked_at", Instant.now().toString());
            } catch (Exception ignored) {
                // First-run and offline use remain available during the local trial.
            }
        }

        Instant expiresAt = startedAt.plus(Duration.ofDays(TRIAL_DAYS));
        if (Instant.now().isBefore(expiresAt)) {
            return snapshot(Status.TRIAL, "Free Trial", expiresAt, "Free 30-day trial");
        }
        return snapshot(
            Status.EXPIRED,
            "Free Trial",
            expiresAt,
            "Your 30-day free trial has ended. Activate BizFlow POS to continue."
        );
    }

    private LicenseSnapshot validatePaidLicense(String licenseKey, String apiUrl) throws Exception {
        Map<String, Object> body = baseRequest();
        body.put("license_key", licenseKey);
        Map<String, Object> response = post(normalizeApiUrl(apiUrl) + "license/validate", body);
        savePaidResponse(licenseKey, normalizeApiUrl(apiUrl), response);
        return paidSnapshot(response, Status.ACTIVE, "License validated");
    }

    private Map<String, Object> baseRequest() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("machine_id", getMachineId());
        body.put("device_name", deviceName());
        body.put("app_version", "2.0.0");
        return body;
    }

    private void savePaidResponse(
        String key,
        String apiUrl,
        Map<String, Object> response
    ) {
        String businessId = String.valueOf(response.getOrDefault("business_id", "")).trim();
        String syncToken = String.valueOf(response.getOrDefault("sync_token", "")).trim();
        if (businessId.isBlank() || syncToken.isBlank()) {
            throw new IllegalStateException(
                "The licensing backend did not provide a secure business sync identity."
            );
        }
        String existingBusinessId = getSetting("license_business_id");
        if (!isBlank(existingBusinessId) && !existingBusinessId.equalsIgnoreCase(businessId)) {
            throw new IllegalStateException(
                "This database already belongs to another business. "
                    + "Use a new BizFlow POS database before activating a different business."
            );
        }
        boolean firstBusinessBinding = isBlank(existingBusinessId);
        putSetting("license_key", key);
        putSetting("license_status", "ACTIVE");
        putSetting("license_plan_code", String.valueOf(response.getOrDefault("plan_code", "")));
        putSetting("license_plan_name", String.valueOf(response.getOrDefault("plan_name", "Licensed")));
        putSetting("license_expires_at", String.valueOf(response.getOrDefault("expires_at", "")));
        putSetting("license_last_validated_at", Instant.now().toString());
        putSetting("license_machine_id", getMachineId());
        putSetting("license_business_id", businessId);
        putSetting("sync_api_url", apiUrl);
        putSetting("sync_api_token", syncToken);
        if (firstBusinessBinding) {
            putSetting("last_successful_sync", "");
        }
    }

    private LicenseSnapshot paidSnapshot(
        Map<String, Object> response,
        Status status,
        String message
    ) {
        return snapshot(
            status,
            String.valueOf(response.getOrDefault("plan_name", "Licensed")),
            parseInstant(String.valueOf(response.get("expires_at"))),
            message
        );
    }

    private LicenseSnapshot snapshot(Status status, String plan, Instant expiresAt, String message) {
        long daysRemaining = expiresAt == null ? 0
            : Math.max(0, (long)Math.ceil(Duration.between(Instant.now(), expiresAt).toHours() / 24.0));
        return new LicenseSnapshot(
            status,
            status == Status.TRIAL || status == Status.ACTIVE || status == Status.GRACE,
            plan,
            expiresAt,
            daysRemaining,
            message,
            getMachineId()
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> post(String endpoint, Map<String, Object> body) throws Exception {
        if (isBlank(endpoint) || endpoint.startsWith("license/")) {
            throw new IllegalStateException("Licensing backend URL is not configured.");
        }
        HttpURLConnection connection = (HttpURLConnection)new URL(endpoint).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(6_000);
        connection.setReadTimeout(15_000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        byte[] payload = gson.toJson(body).getBytes(StandardCharsets.UTF_8);
        try (OutputStream output = connection.getOutputStream()) {
            output.write(payload);
        }

        int status = connection.getResponseCode();
        InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
        String json = stream == null ? "" : new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        Map<String, Object> response = json.isBlank() ? new LinkedHashMap<>() : gson.fromJson(json, Map.class);
        if (status >= 400) {
            throw new IllegalStateException(String.valueOf(
                response.getOrDefault("message", response.getOrDefault("error", "License server error " + status))
            ));
        }
        return response;
    }

    private void ensureLocalState() {
        if (isBlank(getSetting("trial_started_at"))) putSetting("trial_started_at", Instant.now().toString());
        if (getSetting("license_status") == null) putSetting("license_status", "TRIAL");
    }

    private String getSetting(String key) {
        return getSetting(key, null);
    }

    private String getSetting(String key, String defaultValue) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT value FROM app_settings WHERE key=?")) {
            statement.setString(1, key);
            ResultSet row = statement.executeQuery();
            return row.next() ? row.getString(1) : defaultValue;
        } catch (Exception exception) {
            return defaultValue;
        }
    }

    private void putSetting(String key, String value) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "INSERT OR REPLACE INTO app_settings(key,value) VALUES(?,?)")) {
            statement.setString(1, key);
            statement.setString(2, value == null ? "" : value);
            statement.executeUpdate();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not save license state", exception);
        }
    }

    private static Instant parseInstant(String value) {
        if (isBlank(value) || "null".equalsIgnoreCase(value)) return null;
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            try {
                return java.time.LocalDateTime.parse(value.replace(' ', 'T'))
                    .toInstant(java.time.ZoneOffset.UTC);
            } catch (Exception ignoredAgain) {
                return null;
            }
        }
    }

    private static String normalizeApiUrl(String value) {
        if (value == null) return "";
        String normalized = value.trim();
        if (!normalized.endsWith("/")) normalized += "/";
        return normalized;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String friendlyMessage(Exception exception) {
        String message = exception.getMessage();
        return isBlank(message) ? "Connect to the internet and try again." : message;
    }

    private static String deviceName() {
        String computer = System.getenv("COMPUTERNAME");
        if (!isBlank(computer)) return computer;
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
            return "Windows workstation";
        }
    }

    public static final class LicenseSnapshot {
        private final Status status;
        private final boolean allowed;
        private final String planName;
        private final Instant expiresAt;
        private final long daysRemaining;
        private final String message;
        private final String machineId;

        public LicenseSnapshot(
            Status status,
            boolean allowed,
            String planName,
            Instant expiresAt,
            long daysRemaining,
            String message,
            String machineId
        ) {
            this.status = status;
            this.allowed = allowed;
            this.planName = planName;
            this.expiresAt = expiresAt;
            this.daysRemaining = daysRemaining;
            this.message = message;
            this.machineId = machineId;
        }

        public Status getStatus() { return status; }
        public boolean isAllowed() { return allowed; }
        public String getPlanName() { return planName; }
        public Instant getExpiresAt() { return expiresAt; }
        public long getDaysRemaining() { return daysRemaining; }
        public String getMessage() { return message; }
        public String getMachineId() { return machineId; }

        public String getDisplayText() {
            if (status == Status.TRIAL) return "Trial · " + daysRemaining + " days left";
            if (status == Status.GRACE) return "License offline · " + daysRemaining + " days left";
            if (status == Status.ACTIVE) return planName + " · " + daysRemaining + " days left";
            return "Activation required";
        }
    }
}
