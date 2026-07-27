package com.retailpos.repository;

import com.retailpos.model.AppSettings;
import com.retailpos.util.DatabaseManager;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class SettingsRepository {

    public AppSettings load() throws SQLException {
        Map<String,String> kv = new HashMap<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT key,value FROM app_settings")) {
            ResultSet r = ps.executeQuery();
            while (r.next()) kv.put(r.getString("key"), r.getString("value"));
        }
        AppSettings s = new AppSettings();
        if (kv.containsKey("store_name"))          s.setStoreName(kv.get("store_name"));
        if (kv.containsKey("store_address"))        s.setStoreAddress(kv.get("store_address"));
        if (kv.containsKey("store_phone"))          s.setStorePhone(kv.get("store_phone"));
        if (kv.containsKey("store_footer"))         s.setStoreFooter(kv.get("store_footer"));
        if (kv.containsKey("logo_path"))            s.setLogoPath(kv.get("logo_path"));
        if (kv.containsKey("printer_name"))         s.setPrinterName(kv.get("printer_name"));
        if (kv.containsKey("paper_width"))          s.setPaperWidth(Integer.parseInt(kv.getOrDefault("paper_width","80")));
        if (kv.containsKey("tax_rate"))             s.setTaxRate(Double.parseDouble(kv.getOrDefault("tax_rate","16.0")));
        if (kv.containsKey("loyalty_earning_rate")) s.setLoyaltyEarningRate(Double.parseDouble(kv.getOrDefault("loyalty_earning_rate","1.0")));
        if (kv.containsKey("sync_api_url"))         s.setSyncApiUrl(kv.get("sync_api_url"));
        if (kv.containsKey("sync_api_token"))       s.setSyncApiToken(kv.get("sync_api_token"));
        if (kv.containsKey("sync_api_username"))    s.setSyncApiUsername(kv.get("sync_api_username"));
        if (kv.containsKey("sync_api_password"))    s.setSyncApiPassword(kv.get("sync_api_password"));
        if (kv.containsKey("auto_sync"))            s.setAutoSync(Boolean.parseBoolean(kv.getOrDefault("auto_sync","true")));
        if (kv.containsKey("dark_mode"))            s.setDarkMode(Boolean.parseBoolean(kv.getOrDefault("dark_mode","false")));
        if (kv.containsKey("primary_color"))        s.setPrimaryColor(kv.get("primary_color"));
        if (kv.containsKey("backup_path"))          s.setBackupPath(kv.get("backup_path"));
        if (kv.containsKey("backup_time"))          s.setBackupTime(kv.get("backup_time"));
        if (kv.containsKey("auto_print_receipt"))   s.setAutoPrintReceipt(Boolean.parseBoolean(kv.getOrDefault("auto_print_receipt","true")));
        if (kv.containsKey("setup_complete"))       s.setSetupComplete(Boolean.parseBoolean(kv.get("setup_complete")));
        if (kv.containsKey("last_successful_sync")) s.setLastSuccessfulSync(kv.get("last_successful_sync"));
        return s;
    }

    public void save(AppSettings s) throws SQLException {
        String[][] pairs = {
            {"store_name", s.getStoreName()}, {"store_address", s.getStoreAddress()},
            {"store_phone", s.getStorePhone()}, {"store_footer", s.getStoreFooter()},
            {"logo_path", s.getLogoPath()}, {"printer_name", s.getPrinterName()},
            {"paper_width", String.valueOf(s.getPaperWidth())}, {"tax_rate", String.valueOf(s.getTaxRate())},
            {"loyalty_earning_rate", String.valueOf(s.getLoyaltyEarningRate())},
            {"sync_api_url", s.getSyncApiUrl()}, {"sync_api_token", s.getSyncApiToken()},
            {"sync_api_username", s.getSyncApiUsername()}, {"sync_api_password", s.getSyncApiPassword()},
            {"auto_sync", String.valueOf(s.isAutoSync())}, {"dark_mode", String.valueOf(s.isDarkMode())},
            {"primary_color", s.getPrimaryColor()},
            {"backup_path", s.getBackupPath()}, {"backup_time", s.getBackupTime()},
            {"auto_print_receipt", String.valueOf(s.isAutoPrintReceipt())},
            {"setup_complete", String.valueOf(s.isSetupComplete())},
            {"last_successful_sync", s.getLastSuccessfulSync()}
        };
        try (Connection c = DatabaseManager.getConnection()) {
            for (String[] kv : pairs) {
                try (PreparedStatement ps = c.prepareStatement(
                    "INSERT OR REPLACE INTO app_settings(key,value) VALUES(?,?)")) {
                    ps.setString(1, kv[0]); ps.setString(2, kv[1]);
                    ps.executeUpdate();
                }
            }
        }
    }
}
