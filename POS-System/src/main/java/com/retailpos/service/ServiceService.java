package com.retailpos.service;

import com.retailpos.model.*;
import com.retailpos.repository.ServiceRepository;
import com.retailpos.repository.SettingsRepository;
import com.retailpos.util.AuditLogger;
import com.retailpos.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Business logic for the Services module.
 *
 * Responsibilities:
 *  - Create / update Job Cards
 *  - Create / update Quotations linked to Job Cards (parts from store inventory)
 *  - Convert an APPROVED Quotation to an Invoice (= a Sale record + stock decrement)
 *  - Status transitions with validation
 */
public class ServiceService {

    public interface ServiceListener {
        void onJobCardChanged(JobCard jc);
        void onQuotationChanged(Quotation q);
    }

    private static ServiceService instance;
    private final ServiceRepository  repo         = new ServiceRepository();
    private final SettingsRepository settingsRepo = new SettingsRepository();
    private final ProductService     productSvc   = ProductService.getInstance();
    private final SaleService        saleSvc      = SaleService.getInstance();
    private final List<ServiceListener> listeners = new CopyOnWriteArrayList<>();

    private ServiceService() {}

    public static synchronized ServiceService getInstance() {
        if (instance == null) instance = new ServiceService();
        return instance;
    }

    public void addListener(ServiceListener l)    { listeners.add(l); }
    public void removeListener(ServiceListener l) { listeners.remove(l); }

    // ═══════════════════════════════════════════════════════════════════
    //  JOB CARDS
    // ═══════════════════════════════════════════════════════════════════

    /** Create a new job card — generates the job number automatically. */
    public JobCard createJobCard(
            String customerName, String customerPhone, String customerId,
            String assetDescription, String assetSerial,
            String problemDescription, String technicianName, String technicianId,
            double labourCharge, LocalDateTime dueDate,
            List<JobCard.ServiceItem> serviceItems) throws Exception {

        if (customerName == null || customerName.isBlank())
            throw new IllegalArgumentException("Customer name is required.");
        if (assetDescription == null || assetDescription.isBlank())
            throw new IllegalArgumentException("Asset / item description is required.");
        if (problemDescription == null || problemDescription.isBlank())
            throw new IllegalArgumentException("Problem description is required.");

        String now     = LocalDateTime.now().toString();
        String id      = UUID.randomUUID().toString();
        String jobNum  = generateJobNumber();

        // Assign IDs to service items
        List<JobCard.ServiceItem> items = serviceItems == null ? new ArrayList<>() : serviceItems;
        items.forEach(si -> { if (si.getId() == null || si.getId().isBlank()) si.setId(UUID.randomUUID().toString()); });

        JobCard jc = new JobCard();
        jc.setId(id);
        jc.setJobNumber(jobNum);
        jc.setCustomerId(customerId);
        jc.setCustomerName(customerName.trim());
        jc.setCustomerPhone(customerPhone == null ? "" : customerPhone.trim());
        jc.setAssetDescription(assetDescription.trim());
        jc.setAssetSerial(assetSerial == null ? "" : assetSerial.trim());
        jc.setProblemDescription(problemDescription.trim());
        jc.setTechnicianId(technicianId);
        jc.setTechnicianName(technicianName == null ? "" : technicianName.trim());
        jc.setLabourCharge(Math.max(0, labourCharge));
        jc.setDueDate(dueDate);
        jc.setStatus(JobCard.Status.OPEN);
        jc.setSyncStatus("PENDING");
        jc.setServiceItems(items);
        jc.setCreatedAt(LocalDateTime.now());
        jc.setUpdatedAt(LocalDateTime.now());

        repo.insertJobCard(jc);

        AuthService auth = AuthService.getInstance();
        AuditLogger.log(auth.getCurrentUser().getId(), "JOB_CARD_CREATED",
                id, "job=" + jobNum + ",customer=" + customerName);

        notifyJobCardChanged(jc);
        return jc;
    }

    /** Update mutable fields on an existing job card. */
    public JobCard updateJobCard(JobCard jc,
            String customerName, String customerPhone, String customerId,
            String assetDescription, String assetSerial,
            String problemDescription, String diagnosis, String resolution,
            String technicianName, String technicianId,
            double labourCharge, LocalDateTime dueDate,
            JobCard.Status newStatus,
            List<JobCard.ServiceItem> serviceItems) throws Exception {

        if (jc.getStatus() == JobCard.Status.INVOICED)
            throw new IllegalStateException("Cannot edit an invoiced job card.");

        jc.setCustomerId(customerId);
        jc.setCustomerName(customerName == null ? jc.getCustomerName() : customerName.trim());
        jc.setCustomerPhone(customerPhone == null ? jc.getCustomerPhone() : customerPhone.trim());
        jc.setAssetDescription(assetDescription == null ? jc.getAssetDescription() : assetDescription.trim());
        jc.setAssetSerial(assetSerial == null ? jc.getAssetSerial() : assetSerial.trim());
        jc.setProblemDescription(problemDescription == null ? jc.getProblemDescription() : problemDescription.trim());
        jc.setDiagnosis(diagnosis == null ? jc.getDiagnosis() : diagnosis.trim());
        jc.setResolution(resolution == null ? jc.getResolution() : resolution.trim());
        jc.setTechnicianId(technicianId);
        jc.setTechnicianName(technicianName == null ? jc.getTechnicianName() : technicianName.trim());
        jc.setLabourCharge(Math.max(0, labourCharge));
        jc.setDueDate(dueDate);
        jc.setStatus(newStatus);
        jc.setSyncStatus("PENDING");
        jc.setUpdatedAt(LocalDateTime.now());

        if (serviceItems != null) {
            serviceItems.forEach(si -> { if (si.getId() == null || si.getId().isBlank()) si.setId(UUID.randomUUID().toString()); });
            jc.setServiceItems(serviceItems);
        }

        repo.updateJobCard(jc);

        AuditLogger.log(AuthService.getInstance().getCurrentUser().getId(),
                "JOB_CARD_UPDATED", jc.getId(),
                "job=" + jc.getJobNumber() + ",status=" + newStatus.name());

        notifyJobCardChanged(jc);
        return jc;
    }

    public void updateJobCardStatus(JobCard jc, JobCard.Status newStatus) throws Exception {
        jc.setStatus(newStatus);
        jc.setSyncStatus("PENDING");
        jc.setUpdatedAt(LocalDateTime.now());
        repo.updateJobCard(jc);
        notifyJobCardChanged(jc);
    }

    public List<JobCard> getAllJobCards() throws Exception          { return repo.findAllJobCards(); }
    public List<JobCard> getJobCardsByStatus(JobCard.Status s) throws Exception { return repo.findJobCardsByStatus(s); }
    public List<JobCard> searchJobCards(String q) throws Exception { return repo.searchJobCards(q); }
    public Optional<JobCard> findJobCardById(String id) throws Exception { return repo.findJobCardById(id); }

    public Map<String, Integer> getJobCardStatusCounts() {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (JobCard.Status s : JobCard.Status.values()) {
            try { map.put(s.display(), repo.countJobCardsByStatus(s)); }
            catch (Exception ignored) { map.put(s.display(), 0); }
        }
        return map;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  QUOTATIONS
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Create a quotation for a job card, listing store parts needed.
     * Each QuotationItem must reference a product already in the system.
     */
    public Quotation createQuotation(JobCard jc,
            List<Quotation.QuotationItem> items,
            double discountAmount, String notes,
            LocalDateTime validUntil) throws Exception {

        if (jc == null) throw new IllegalArgumentException("Job card is required.");
        if (items == null || items.isEmpty())
            throw new IllegalArgumentException("Add at least one part or product to the quotation.");
        if (jc.getStatus() == JobCard.Status.CANCELLED)
            throw new IllegalStateException("Cannot quote on a cancelled job.");
        if (jc.getStatus() == JobCard.Status.INVOICED)
            throw new IllegalStateException("This job has already been invoiced.");

        AppSettings settings = settingsRepo.load();
        double taxRate = settings.getTaxRate();

        validateQuotationItems(items);

        String id     = UUID.randomUUID().toString();
        String qtNum  = generateQuotationNumber();
        AuthService auth = AuthService.getInstance();

        Quotation q = new Quotation();
        q.setId(id);
        q.setQuotationNumber(qtNum);
        q.setJobCardId(jc.getId());
        q.setJobCardNumber(jc.getJobNumber());
        q.setCustomerId(jc.getCustomerId());
        q.setCustomerName(jc.getCustomerName());
        q.setCustomerPhone(jc.getCustomerPhone());
        q.setDiscountAmount(Math.max(0, discountAmount));
        q.setLabourTotal(jc.getTotalLabour());
        q.setNotes(notes == null ? "" : notes.trim());
        q.setStatus(Quotation.Status.DRAFT);
        q.setCreatedById(auth.getCurrentUser().getId());
        q.setCreatedByName(auth.getCurrentUser().getFullName());
        q.setValidUntil(validUntil != null ? validUntil : LocalDateTime.now().plusDays(14));
        q.setSyncStatus("PENDING");
        q.setItems(items);
        q.setCreatedAt(LocalDateTime.now());
        q.setUpdatedAt(LocalDateTime.now());
        validateQuotationDiscount(q);
        q.recalculateTotals(taxRate);

        repo.insertQuotation(q);

        // Link quotation back to job card
        jc.setActiveQuotationId(id);
        // Do not move a completed delivery backwards when its quotation is
        // prepared or corrected after the technician has finished the work.
        if (jc.getStatus() == JobCard.Status.OPEN || jc.getStatus() == JobCard.Status.AWAITING_PARTS) {
            jc.setStatus(JobCard.Status.IN_PROGRESS);
        }
        jc.setSyncStatus("PENDING");
        jc.setUpdatedAt(LocalDateTime.now());
        repo.updateJobCard(jc);

        AuditLogger.log(auth.getCurrentUser().getId(), "QUOTATION_CREATED",
                id, "qt=" + qtNum + ",job=" + jc.getJobNumber()
                   + ",total=" + q.getGrandTotal());

        notifyQuotationChanged(q);
        notifyJobCardChanged(jc);
        return q;
    }

    /** Save updated parts / prices to an existing DRAFT quotation. */
    public Quotation updateQuotation(Quotation q,
            List<Quotation.QuotationItem> items,
            double discountAmount, String notes,
            LocalDateTime validUntil) throws Exception {

        if (q.getStatus() != Quotation.Status.DRAFT && q.getStatus() != Quotation.Status.SENT)
            throw new IllegalStateException("Only draft or sent quotations can be edited.");
        if (items == null || items.isEmpty())
            throw new IllegalArgumentException("Add at least one inventory item to the quotation.");

        AppSettings settings = settingsRepo.load();
        double taxRate = settings.getTaxRate();

        validateQuotationItems(items);

        q.setItems(items);
        // Keep a draft quotation in step with the job card's service labour.
        findJobCardById(q.getJobCardId()).ifPresent(job -> q.setLabourTotal(job.getTotalLabour()));
        q.setDiscountAmount(Math.max(0, discountAmount));
        q.setNotes(notes == null ? "" : notes.trim());
        q.setValidUntil(validUntil);
        q.setSyncStatus("PENDING");
        q.setUpdatedAt(LocalDateTime.now());
        validateQuotationDiscount(q);
        q.recalculateTotals(taxRate);

        repo.updateQuotation(q);
        notifyQuotationChanged(q);
        return q;
    }

    public void approveQuotation(Quotation q) throws Exception {
        if (q.getStatus() != Quotation.Status.DRAFT && q.getStatus() != Quotation.Status.SENT)
            throw new IllegalStateException("Only DRAFT or SENT quotations can be approved.");
        if (q.getValidUntil() != null && q.getValidUntil().isBefore(LocalDateTime.now()))
            throw new IllegalStateException("This quotation has expired. Update its validity date before approval.");
        validateQuotationItems(q.getItems());
        repo.updateQuotationStatus(q.getId(), Quotation.Status.APPROVED);
        q.setStatus(Quotation.Status.APPROVED);
        notifyQuotationChanged(q);
    }

    public void rejectQuotation(Quotation q) throws Exception {
        if (q.getStatus() == Quotation.Status.INVOICED)
            throw new IllegalStateException("Cannot reject an invoiced quotation.");
        repo.updateQuotationStatus(q.getId(), Quotation.Status.REJECTED);
        q.setStatus(Quotation.Status.REJECTED);
        notifyQuotationChanged(q);
    }

    /**
     * Convert an APPROVED quotation to an Invoice.
     *
     * This:
     *  1. Creates a Sale record (payment method passed in)
     *  2. Decrements stock for every product-linked quotation item
     *  3. Marks the quotation INVOICED
     *  4. Marks the job card INVOICED
     *
     * Returns the completed Sale so the caller can print a receipt.
     */
    public Sale convertToInvoice(Quotation q, JobCard jc,
            String paymentMethod, double cashTendered,
            String paymentReference) throws Exception {

        if (q.getStatus() != Quotation.Status.APPROVED)
            throw new IllegalStateException("Only APPROVED quotations can be invoiced.");
        if (jc.getStatus() == JobCard.Status.INVOICED)
            throw new IllegalStateException("This job has already been invoiced.");
        if (jc.getStatus() == JobCard.Status.CANCELLED)
            throw new IllegalStateException("A cancelled job card cannot be invoiced.");
        if (jc.getStatus() != JobCard.Status.COMPLETED)
            throw new IllegalStateException("Complete the job card before invoicing the service delivery.");
        if (q.getValidUntil() != null && q.getValidUntil().isBefore(LocalDateTime.now()))
            throw new IllegalStateException("This quotation has expired and cannot be invoiced.");

        validateQuotationItems(q.getItems());

        // Build sale items from quotation items (parts only — excludes null-product labour)
        List<Sale.SaleItem> saleItems = new ArrayList<>();
        for (Quotation.QuotationItem qi : q.getItems()) {
            if (qi.getProductId() == null || qi.getProductId().isBlank()) continue; // skip free-text parts
            Sale.SaleItem si = new Sale.SaleItem();
            si.setProductId(qi.getProductId());
            si.setProductName(qi.getProductName());
            si.setProductSku(qi.getProductSku() == null ? "" : qi.getProductSku());
            si.setQuantity(qi.getQuantity());
            si.setUnitPrice(qi.getUnitPrice());
            si.setBuyingPrice(qi.getBuyingPrice());
            si.setDiscount(qi.getDiscount());
            si.setTaxRate(qi.getTaxRate());
            si.recalculate();
            saleItems.add(si);
        }

        // Labour is a non-stock invoice line. SaleService recognises it as such
        // and does not decrement inventory for it.
        if (q.getLabourTotal() > 0) {
            Sale.SaleItem labour = new Sale.SaleItem();
            labour.setProductName("Service labour — " + jc.getJobNumber());
            labour.setProductSku("SERVICE");
            labour.setQuantity(1);
            labour.setUnitPrice(q.getLabourTotal());
            labour.setBuyingPrice(0);
            labour.setDiscount(0);
            labour.recalculate();
            saleItems.add(labour);
        }

        if (saleItems.isEmpty())
            throw new IllegalStateException("The quotation has no billable items.");

        // Delegate to SaleService — this handles stock decrement, receipt numbering, audit
        Sale sale = saleSvc.completeSale(
                saleItems, paymentMethod, cashTendered,
                paymentReference, q.getCustomerId(),
                q.getDiscountAmount(), false);

        // Mark quotation invoiced
        repo.markQuotationInvoiced(q.getId(), sale.getId());
        q.setStatus(Quotation.Status.INVOICED);
        q.setInvoiceSaleId(sale.getId());

        // Mark job card invoiced and record sale reference
        jc.setStatus(JobCard.Status.INVOICED);
        jc.setResolution("Invoiced via " + q.getQuotationNumber()
                + " | Receipt " + sale.getReceiptNumber());
        jc.setSyncStatus("PENDING");
        jc.setUpdatedAt(LocalDateTime.now());
        repo.updateJobCard(jc);

        AuditLogger.log(AuthService.getInstance().getCurrentUser().getId(),
                "QUOTATION_INVOICED", q.getId(),
                "qt=" + q.getQuotationNumber() + ",receipt=" + sale.getReceiptNumber()
                        + ",total=" + q.getGrandTotal());

        notifyQuotationChanged(q);
        notifyJobCardChanged(jc);
        return sale;
    }

    public List<Quotation> getQuotationsForJobCard(JobCard jc) throws Exception {
        return repo.findQuotationsByJobCard(jc.getId());
    }

    public Optional<Quotation> findQuotationById(String id) throws Exception {
        return repo.findQuotationById(id);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════════

    private String generateJobNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        try {
            int seq = repo.nextJobCardSequence();
            return String.format("JOB-%s-%04d", date, seq);
        } catch (Exception e) {
            return "JOB-" + date + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        }
    }

    private String generateQuotationNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        try {
            int seq = repo.nextQuotationSequence();
            return String.format("QT-%s-%04d", date, seq);
        } catch (Exception e) {
            return "QT-" + date + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        }
    }

    private void notifyJobCardChanged(JobCard jc) {
        for (ServiceListener l : listeners) {
            try { l.onJobCardChanged(jc); } catch (Exception ignored) {}
        }
    }

    private void notifyQuotationChanged(Quotation q) {
        for (ServiceListener l : listeners) {
            try { l.onQuotationChanged(q); } catch (Exception ignored) {}
        }
    }

    /** Ensures every quotation line is a real, available inventory product. */
    private void validateQuotationItems(List<Quotation.QuotationItem> items) throws Exception {
        if (items == null || items.isEmpty())
            throw new IllegalArgumentException("Add at least one inventory item to the quotation.");
        for (Quotation.QuotationItem qi : items) {
            if (qi == null || qi.getProductId() == null || qi.getProductId().isBlank())
                throw new IllegalArgumentException("Every quotation line must be selected from inventory.");
            if (qi.getQuantity() <= 0)
                throw new IllegalArgumentException("Quotation quantities must be at least 1.");
            if (qi.getUnitPrice() < 0 || qi.getDiscount() < 0)
                throw new IllegalArgumentException("Quotation prices and discounts cannot be negative.");
            if (qi.getDiscount() > qi.getUnitPrice() * qi.getQuantity())
                throw new IllegalArgumentException("A line discount cannot exceed the line value.");
            Product product = productSvc.findById(qi.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Inventory product not found: " + qi.getProductName()));
            if (!product.isActive())
                throw new IllegalArgumentException("Inventory product is inactive: " + product.getName());
            if (qi.getId() == null || qi.getId().isBlank()) qi.setId(UUID.randomUUID().toString());
            qi.setProductName(product.getName());
            qi.setProductSku(product.getSku());
            qi.setBuyingPrice(product.getBuyingPrice());
            qi.recalculate();
        }
    }

    private void validateQuotationDiscount(Quotation q) {
        double billableValue = q.getLabourTotal()
            + q.getItems().stream().mapToDouble(Quotation.QuotationItem::getLineTotal).sum();
        if (q.getDiscountAmount() > billableValue)
            throw new IllegalArgumentException("The quotation discount cannot exceed the billable total.");
    }
}
