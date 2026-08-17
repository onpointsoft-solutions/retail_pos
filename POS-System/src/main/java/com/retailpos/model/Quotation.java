package com.retailpos.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A Quotation is raised against a Job Card.  It lists the parts (products from
 * the store's inventory) required to complete the service, together with their
 * quantities and prices.  Once approved it can be converted to an Invoice /
 * Sale which actually decrements stock.
 */
public class Quotation {

    public enum Status {
        DRAFT, SENT, APPROVED, REJECTED, INVOICED;

        public String display() {
            return switch (this) {
                case DRAFT    -> "Draft";
                case SENT     -> "Sent";
                case APPROVED -> "Approved";
                case REJECTED -> "Rejected";
                case INVOICED -> "Invoiced";
            };
        }
    }

    // ── Identity ──────────────────────────────────────────────────────────────
    private String id;
    private String quotationNumber;    // QT-20260816-0001
    private String jobCardId;          // links back to the job card
    private String jobCardNumber;      // denormalised for display
    private String invoiceSaleId;      // populated once the quotation is delivered/invoiced

    // ── Customer (copied from job card) ───────────────────────────────────────
    private String customerId;
    private String customerName;
    private String customerPhone;

    // ── Financials ────────────────────────────────────────────────────────────
    private double subtotal;           // sum of all item line totals
    private double discountAmount;
    private double taxAmount;
    private double grandTotal;
    private double labourTotal;        // copied from job card at invoice time
    private String notes;

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    private Status status;
    private String createdById;
    private String createdByName;
    private LocalDateTime validUntil;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String syncStatus;

    // ── Line items (parts / products) ─────────────────────────────────────────
    private List<QuotationItem> items = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────────────────

    public Quotation() {}

    // ── Inner class: QuotationItem ────────────────────────────────────────────

    /**
     * One part (product from the store) needed for the service.
     */
    public static class QuotationItem {
        private String id;
        private String quotationId;
        private String productId;      // FK → products.id  (null for free-text parts)
        private String productName;
        private String productSku;
        private int    quantity;
        private double unitPrice;
        private double buyingPrice;
        private double discount;
        private double taxRate;
        private double lineTotal;

        public QuotationItem() {}

        public String getId()                  { return id; }
        public void   setId(String id)         { this.id = id; }
        public String getQuotationId()         { return quotationId; }
        public void   setQuotationId(String v) { this.quotationId = v; }
        public String getProductId()           { return productId; }
        public void   setProductId(String v)   { this.productId = v; }
        public String getProductName()         { return productName; }
        public void   setProductName(String v) { this.productName = v; }
        public String getProductSku()          { return productSku; }
        public void   setProductSku(String v)  { this.productSku = v; }
        public int    getQuantity()            { return quantity; }
        public void   setQuantity(int v)       { this.quantity = v; }
        public double getUnitPrice()           { return unitPrice; }
        public void   setUnitPrice(double v)   { this.unitPrice = v; }
        public double getBuyingPrice()         { return buyingPrice; }
        public void   setBuyingPrice(double v) { this.buyingPrice = v; }
        public double getDiscount()            { return discount; }
        public void   setDiscount(double v)    { this.discount = v; }
        public double getTaxRate()             { return taxRate; }
        public void   setTaxRate(double v)     { this.taxRate = v; }
        public double getLineTotal()           { return lineTotal; }
        public void   setLineTotal(double v)   { this.lineTotal = v; }

        public void recalculate() {
            this.lineTotal = (unitPrice * quantity) - discount;
        }
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getId()                              { return id; }
    public void   setId(String id)                     { this.id = id; }
    public String getQuotationNumber()                 { return quotationNumber; }
    public void   setQuotationNumber(String v)         { this.quotationNumber = v; }
    public String getJobCardId()                       { return jobCardId; }
    public void   setJobCardId(String v)               { this.jobCardId = v; }
    public String getJobCardNumber()                   { return jobCardNumber; }
    public void   setJobCardNumber(String v)           { this.jobCardNumber = v; }
    public String getInvoiceSaleId()                   { return invoiceSaleId; }
    public void   setInvoiceSaleId(String v)           { this.invoiceSaleId = v; }
    public String getCustomerId()                      { return customerId; }
    public void   setCustomerId(String v)              { this.customerId = v; }
    public String getCustomerName()                    { return customerName; }
    public void   setCustomerName(String v)            { this.customerName = v; }
    public String getCustomerPhone()                   { return customerPhone; }
    public void   setCustomerPhone(String v)           { this.customerPhone = v; }
    public double getSubtotal()                        { return subtotal; }
    public void   setSubtotal(double v)                { this.subtotal = v; }
    public double getDiscountAmount()                  { return discountAmount; }
    public void   setDiscountAmount(double v)          { this.discountAmount = v; }
    public double getTaxAmount()                       { return taxAmount; }
    public void   setTaxAmount(double v)               { this.taxAmount = v; }
    public double getGrandTotal()                      { return grandTotal; }
    public void   setGrandTotal(double v)              { this.grandTotal = v; }
    public double getLabourTotal()                     { return labourTotal; }
    public void   setLabourTotal(double v)             { this.labourTotal = v; }
    public String getNotes()                           { return notes; }
    public void   setNotes(String v)                   { this.notes = v; }
    public Status getStatus()                          { return status; }
    public void   setStatus(Status v)                  { this.status = v; }
    public String getCreatedById()                     { return createdById; }
    public void   setCreatedById(String v)             { this.createdById = v; }
    public String getCreatedByName()                   { return createdByName; }
    public void   setCreatedByName(String v)           { this.createdByName = v; }
    public LocalDateTime getValidUntil()               { return validUntil; }
    public void          setValidUntil(LocalDateTime v){ this.validUntil = v; }
    public LocalDateTime getCreatedAt()                { return createdAt; }
    public void          setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public LocalDateTime getUpdatedAt()                { return updatedAt; }
    public void          setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
    public String getSyncStatus()                      { return syncStatus; }
    public void   setSyncStatus(String v)              { this.syncStatus = v; }
    public List<QuotationItem> getItems()              { return items; }
    public void   setItems(List<QuotationItem> v)      { this.items = v; }

    /**
     * Recomputes the quotation total. Labour is a billable service line, so it
     * is included in the taxable amount just like the parts supplied.
     */
    public void recalculateTotals(double taxRate) {
        subtotal = items.stream().mapToDouble(QuotationItem::getLineTotal).sum();
        taxAmount = Math.max(0, subtotal - discountAmount + labourTotal) * taxRate / 100.0;
        grandTotal = subtotal - discountAmount + taxAmount + labourTotal;
    }
}
