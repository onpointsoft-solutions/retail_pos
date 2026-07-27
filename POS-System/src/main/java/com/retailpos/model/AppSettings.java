package com.retailpos.model;

public class AppSettings {
    private String storeName = "Victorious Shop";
    private String storeAddress = "";
    private String storePhone = "";
    private String storeFooter = "Thank you for shopping with us!";
    private String logoPath = "";
    private String printerName = "";
    private int paperWidth = 80; // 58 or 80
    private double taxRate = 16.0;
    private double loyaltyEarningRate = 1.0; // points per KES 1 spent
    private String syncApiUrl = "https://pos.mobilemealscenter.co.ke/api/";
    private String syncApiToken = "";
    private String syncApiUsername = "admin";
    private String syncApiPassword = "";
    private boolean autoSync = true;
    private boolean darkMode = false;
    private String primaryColor = "#2563EB";
    private String backupPath = "backups";
    private String backupTime = "23:00";
    private boolean autoPrintReceipt = true;
    private boolean setupComplete = false;
    private String lastSuccessfulSync = "";

    public AppSettings() {}

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public String getStoreAddress() { return storeAddress; }
    public void setStoreAddress(String storeAddress) { this.storeAddress = storeAddress; }
    public String getStorePhone() { return storePhone; }
    public void setStorePhone(String storePhone) { this.storePhone = storePhone; }
    public String getStoreFooter() { return storeFooter; }
    public void setStoreFooter(String storeFooter) { this.storeFooter = storeFooter; }
    public String getLogoPath() { return logoPath; }
    public void setLogoPath(String logoPath) { this.logoPath = logoPath; }
    public String getPrinterName() { return printerName; }
    public void setPrinterName(String printerName) { this.printerName = printerName; }
    public int getPaperWidth() { return paperWidth; }
    public void setPaperWidth(int paperWidth) { this.paperWidth = paperWidth; }
    public double getTaxRate() { return taxRate; }
    public void setTaxRate(double taxRate) { this.taxRate = taxRate; }
    public double getLoyaltyEarningRate() { return loyaltyEarningRate; }
    public void setLoyaltyEarningRate(double loyaltyEarningRate) { this.loyaltyEarningRate = loyaltyEarningRate; }
    public String getSyncApiUrl() { return syncApiUrl; }
    public void setSyncApiUrl(String syncApiUrl) { this.syncApiUrl = syncApiUrl; }
    public String getSyncApiToken() { return syncApiToken; }
    public void setSyncApiToken(String syncApiToken) { this.syncApiToken = syncApiToken; }
    public String getSyncApiUsername() { return syncApiUsername; }
    public void setSyncApiUsername(String syncApiUsername) { this.syncApiUsername = syncApiUsername; }
    public String getSyncApiPassword() { return syncApiPassword; }
    public void setSyncApiPassword(String syncApiPassword) { this.syncApiPassword = syncApiPassword; }
    public boolean isAutoSync() { return autoSync; }
    public void setAutoSync(boolean autoSync) { this.autoSync = autoSync; }
    public boolean isDarkMode() { return darkMode; }
    public void setDarkMode(boolean darkMode) { this.darkMode = darkMode; }
    public String getPrimaryColor() { return primaryColor; }
    public void setPrimaryColor(String primaryColor) { this.primaryColor = primaryColor; }
    public String getBackupPath() { return backupPath; }
    public void setBackupPath(String backupPath) { this.backupPath = backupPath; }
    public String getBackupTime() { return backupTime; }
    public void setBackupTime(String backupTime) { this.backupTime = backupTime; }
    public boolean isAutoPrintReceipt() { return autoPrintReceipt; }
    public void setAutoPrintReceipt(boolean autoPrintReceipt) { this.autoPrintReceipt = autoPrintReceipt; }
    public boolean isSetupComplete() { return setupComplete; }
    public void setSetupComplete(boolean setupComplete) { this.setupComplete = setupComplete; }
    public String getLastSuccessfulSync() { return lastSuccessfulSync; }
    public void setLastSuccessfulSync(String lastSuccessfulSync) { this.lastSuccessfulSync = lastSuccessfulSync; }
    public int getCharsPerLine() { return paperWidth == 58 ? 32 : 48; }
}
