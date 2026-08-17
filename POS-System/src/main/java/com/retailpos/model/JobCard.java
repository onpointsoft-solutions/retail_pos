package com.retailpos.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A Job Card represents a service job opened for a customer.
 * It tracks the work to be done, the technician, and links to a Quotation
 * that lists parts from store inventory used in the service.
 */
public class JobCard {

    /** Lifecycle states */
    public enum Status {
        OPEN, IN_PROGRESS, AWAITING_PARTS, COMPLETED, CANCELLED, INVOICED;

        public String display() {
            return switch (this) {
                case OPEN           -> "Open";
                case IN_PROGRESS    -> "In Progress";
                case AWAITING_PARTS -> "Awaiting Parts";
                case COMPLETED      -> "Completed";
                case CANCELLED      -> "Cancelled";
                case INVOICED       -> "Invoiced";
            };
        }
    }

    // ── Identity ──────────────────────────────────────────────────────────────
    private String id;
    private String jobNumber;          // JOB-20260816-0001

    // ── Customer / item being serviced ────────────────────────────────────────
    private String customerId;
    private String customerName;
    private String customerPhone;

    /** The physical item brought in (e.g. "Samsung TV Model UA55", "Toyota 110 KCA 123A") */
    private String assetDescription;
    private String assetSerial;        // serial / plate / IMEI

    // ── Service details ───────────────────────────────────────────────────────
    private String problemDescription; // what the customer reported
    private String diagnosis;          // technician diagnosis
    private String resolution;         // what was done
    private String technicianId;
    private String technicianName;
    private double labourCharge;       // charged for labour (no stock items)
    private Status status;

    // ── Linked quotation ──────────────────────────────────────────────────────
    /** ID of the active Quotation for this job card (may be null until one is created) */
    private String activeQuotationId;

    // ── Timestamps ────────────────────────────────────────────────────────────
    private LocalDateTime dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String syncStatus;

    // ── Labour line items (services rendered, not parts) ──────────────────────
    private List<ServiceItem> serviceItems = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────────────────

    public JobCard() {}

    // ── Inner class: ServiceItem ──────────────────────────────────────────────

    /**
     * A single line of work performed (labour), distinct from QuotationItem (parts).
     */
    public static class ServiceItem {
        private String id;
        private String jobCardId;
        private String description;   // e.g. "Panel replacement", "Diagnostic fee"
        private double charge;        // price for this service line
        private int quantity;

        public ServiceItem() {}

        public String getId()                  { return id; }
        public void   setId(String id)         { this.id = id; }
        public String getJobCardId()           { return jobCardId; }
        public void   setJobCardId(String v)   { this.jobCardId = v; }
        public String getDescription()         { return description; }
        public void   setDescription(String v) { this.description = v; }
        public double getCharge()              { return charge; }
        public void   setCharge(double v)      { this.charge = v; }
        public int    getQuantity()            { return quantity; }
        public void   setQuantity(int v)       { this.quantity = v; }
        public double getLineTotal()           { return charge * quantity; }
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getId()                            { return id; }
    public void   setId(String id)                   { this.id = id; }
    public String getJobNumber()                     { return jobNumber; }
    public void   setJobNumber(String v)             { this.jobNumber = v; }
    public String getCustomerId()                    { return customerId; }
    public void   setCustomerId(String v)            { this.customerId = v; }
    public String getCustomerName()                  { return customerName; }
    public void   setCustomerName(String v)          { this.customerName = v; }
    public String getCustomerPhone()                 { return customerPhone; }
    public void   setCustomerPhone(String v)         { this.customerPhone = v; }
    public String getAssetDescription()              { return assetDescription; }
    public void   setAssetDescription(String v)      { this.assetDescription = v; }
    public String getAssetSerial()                   { return assetSerial; }
    public void   setAssetSerial(String v)           { this.assetSerial = v; }
    public String getProblemDescription()            { return problemDescription; }
    public void   setProblemDescription(String v)    { this.problemDescription = v; }
    public String getDiagnosis()                     { return diagnosis; }
    public void   setDiagnosis(String v)             { this.diagnosis = v; }
    public String getResolution()                    { return resolution; }
    public void   setResolution(String v)            { this.resolution = v; }
    public String getTechnicianId()                  { return technicianId; }
    public void   setTechnicianId(String v)          { this.technicianId = v; }
    public String getTechnicianName()                { return technicianName; }
    public void   setTechnicianName(String v)        { this.technicianName = v; }
    public double getLabourCharge()                  { return labourCharge; }
    public void   setLabourCharge(double v)          { this.labourCharge = v; }
    public Status getStatus()                        { return status; }
    public void   setStatus(Status v)                { this.status = v; }
    public String getActiveQuotationId()             { return activeQuotationId; }
    public void   setActiveQuotationId(String v)     { this.activeQuotationId = v; }
    public LocalDateTime getDueDate()                { return dueDate; }
    public void          setDueDate(LocalDateTime v) { this.dueDate = v; }
    public LocalDateTime getCreatedAt()              { return createdAt; }
    public void          setCreatedAt(LocalDateTime v){ this.createdAt = v; }
    public LocalDateTime getUpdatedAt()              { return updatedAt; }
    public void          setUpdatedAt(LocalDateTime v){ this.updatedAt = v; }
    public String getSyncStatus()                    { return syncStatus; }
    public void   setSyncStatus(String v)            { this.syncStatus = v; }
    public List<ServiceItem> getServiceItems()       { return serviceItems; }
    public void setServiceItems(List<ServiceItem> v) { this.serviceItems = v; }

    /** Total labour: sum of all service-item lines */
    public double getTotalLabour() {
        return serviceItems.stream().mapToDouble(ServiceItem::getLineTotal).sum()
             + labourCharge;
    }
}
