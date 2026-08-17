package com.retailpos.repository;

import com.retailpos.model.JobCard;
import com.retailpos.model.Quotation;
import com.retailpos.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * All persistence for job cards, job card service items, quotations and
 * quotation items.  Uses the shared SQLite connection pool.
 */
public class ServiceRepository {

    // ═══════════════════════════════════════════════════════════════════
    // JOB CARDS
    // ═══════════════════════════════════════════════════════════════════

    public void insertJobCard(JobCard jc) throws SQLException {
        String sql = "INSERT INTO job_cards(" +
            "id,job_number,customer_id,customer_name,customer_phone," +
            "asset_description,asset_serial,problem_description,diagnosis,resolution," +
            "technician_id,technician_name,labour_charge,status,active_quotation_id," +
            "due_date,sync_status,created_at,updated_at) " +
            "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1,  jc.getId());
            ps.setString(2,  jc.getJobNumber());
            ps.setString(3,  jc.getCustomerId());
            ps.setString(4,  jc.getCustomerName());
            ps.setString(5,  jc.getCustomerPhone());
            ps.setString(6,  jc.getAssetDescription());
            ps.setString(7,  jc.getAssetSerial());
            ps.setString(8,  jc.getProblemDescription());
            ps.setString(9,  jc.getDiagnosis());
            ps.setString(10, jc.getResolution());
            ps.setString(11, jc.getTechnicianId());
            ps.setString(12, jc.getTechnicianName());
            ps.setDouble(13, jc.getLabourCharge());
            ps.setString(14, jc.getStatus().name());
            ps.setString(15, jc.getActiveQuotationId());
            ps.setString(16, jc.getDueDate() != null ? jc.getDueDate().toString() : null);
            ps.setString(17, jc.getSyncStatus());
            ps.setString(18, jc.getCreatedAt().toString());
            ps.setString(19, jc.getUpdatedAt().toString());
            ps.executeUpdate();
        }
        insertServiceItems(jc.getId(), jc.getServiceItems());
    }

    public void updateJobCard(JobCard jc) throws SQLException {
        String sql = "UPDATE job_cards SET " +
            "customer_id=?,customer_name=?,customer_phone=?," +
            "asset_description=?,asset_serial=?,problem_description=?," +
            "diagnosis=?,resolution=?,technician_id=?,technician_name=?," +
            "labour_charge=?,status=?,active_quotation_id=?,due_date=?," +
            "sync_status=?,updated_at=? WHERE id=?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1,  jc.getCustomerId());
            ps.setString(2,  jc.getCustomerName());
            ps.setString(3,  jc.getCustomerPhone());
            ps.setString(4,  jc.getAssetDescription());
            ps.setString(5,  jc.getAssetSerial());
            ps.setString(6,  jc.getProblemDescription());
            ps.setString(7,  jc.getDiagnosis());
            ps.setString(8,  jc.getResolution());
            ps.setString(9,  jc.getTechnicianId());
            ps.setString(10, jc.getTechnicianName());
            ps.setDouble(11, jc.getLabourCharge());
            ps.setString(12, jc.getStatus().name());
            ps.setString(13, jc.getActiveQuotationId());
            ps.setString(14, jc.getDueDate() != null ? jc.getDueDate().toString() : null);
            ps.setString(15, jc.getSyncStatus());
            ps.setString(16, jc.getUpdatedAt().toString());
            ps.setString(17, jc.getId());
            ps.executeUpdate();
        }
        // Replace service items
        deleteServiceItems(jc.getId());
        insertServiceItems(jc.getId(), jc.getServiceItems());
    }

    public Optional<JobCard> findJobCardById(String id) throws SQLException {
        String sql = "SELECT * FROM job_cards WHERE id=?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                JobCard jc = mapJobCard(rs);
                jc.setServiceItems(findServiceItems(id));
                return Optional.of(jc);
            }
        }
        return Optional.empty();
    }

    public List<JobCard> findAllJobCards() throws SQLException {
        return findJobCardsByStatus(null);
    }

    public List<JobCard> findJobCardsByStatus(JobCard.Status status) throws SQLException {
        String sql = status == null
            ? "SELECT * FROM job_cards ORDER BY created_at DESC"
            : "SELECT * FROM job_cards WHERE status=? ORDER BY created_at DESC";
        List<JobCard> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (status != null) ps.setString(1, status.name());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                JobCard jc = mapJobCard(rs);
                jc.setServiceItems(findServiceItems(jc.getId()));
                list.add(jc);
            }
        }
        return list;
    }

    public List<JobCard> searchJobCards(String query) throws SQLException {
        String like = "%" + query + "%";
        String sql = "SELECT * FROM job_cards WHERE " +
            "job_number LIKE ? OR customer_name LIKE ? OR customer_phone LIKE ? " +
            "OR asset_description LIKE ? OR asset_serial LIKE ? " +
            "ORDER BY created_at DESC LIMIT 200";
        List<JobCard> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 1; i <= 5; i++) ps.setString(i, like);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                JobCard jc = mapJobCard(rs);
                jc.setServiceItems(findServiceItems(jc.getId()));
                list.add(jc);
            }
        }
        return list;
    }

    public int countJobCardsByStatus(JobCard.Status status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM job_cards WHERE status=?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** Returns the next sequential number for job card numbers today */
    public int nextJobCardSequence() throws SQLException {
        String today = LocalDateTime.now().toLocalDate().toString().replace("-", "");
        String sql = "SELECT COUNT(*) FROM job_cards WHERE job_number LIKE ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "JOB-" + today + "-%");
            ResultSet rs = ps.executeQuery();
            return (rs.next() ? rs.getInt(1) : 0) + 1;
        }
    }

    // ── Service Items ─────────────────────────────────────────────────────────

    private void insertServiceItems(String jobCardId, List<JobCard.ServiceItem> items) throws SQLException {
        if (items == null || items.isEmpty()) return;
        String sql = "INSERT INTO job_card_service_items(id,job_card_id,description,charge,quantity) VALUES(?,?,?,?,?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (JobCard.ServiceItem si : items) {
                ps.setString(1, si.getId());
                ps.setString(2, jobCardId);
                ps.setString(3, si.getDescription());
                ps.setDouble(4, si.getCharge());
                ps.setInt(5,    si.getQuantity());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void deleteServiceItems(String jobCardId) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "DELETE FROM job_card_service_items WHERE job_card_id=?")) {
            ps.setString(1, jobCardId);
            ps.executeUpdate();
        }
    }

    public List<JobCard.ServiceItem> findServiceItems(String jobCardId) throws SQLException {
        List<JobCard.ServiceItem> list = new ArrayList<>();
        String sql = "SELECT * FROM job_card_service_items WHERE job_card_id=?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, jobCardId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                JobCard.ServiceItem si = new JobCard.ServiceItem();
                si.setId(rs.getString("id"));
                si.setJobCardId(rs.getString("job_card_id"));
                si.setDescription(rs.getString("description"));
                si.setCharge(rs.getDouble("charge"));
                si.setQuantity(rs.getInt("quantity"));
                list.add(si);
            }
        }
        return list;
    }

    // ── Map row → JobCard ─────────────────────────────────────────────────────

    private JobCard mapJobCard(ResultSet rs) throws SQLException {
        JobCard jc = new JobCard();
        jc.setId(rs.getString("id"));
        jc.setJobNumber(rs.getString("job_number"));
        jc.setCustomerId(rs.getString("customer_id"));
        jc.setCustomerName(rs.getString("customer_name"));
        jc.setCustomerPhone(rs.getString("customer_phone"));
        jc.setAssetDescription(rs.getString("asset_description"));
        jc.setAssetSerial(rs.getString("asset_serial"));
        jc.setProblemDescription(rs.getString("problem_description"));
        jc.setDiagnosis(rs.getString("diagnosis"));
        jc.setResolution(rs.getString("resolution"));
        jc.setTechnicianId(rs.getString("technician_id"));
        jc.setTechnicianName(rs.getString("technician_name"));
        jc.setLabourCharge(rs.getDouble("labour_charge"));
        try { jc.setStatus(JobCard.Status.valueOf(rs.getString("status"))); }
        catch (Exception e) { jc.setStatus(JobCard.Status.OPEN); }
        jc.setActiveQuotationId(rs.getString("active_quotation_id"));
        String due = rs.getString("due_date");
        if (due != null && !due.isBlank()) {
            try { jc.setDueDate(LocalDateTime.parse(due)); } catch (Exception ignored) {}
        }
        jc.setSyncStatus(rs.getString("sync_status"));
        String ca = rs.getString("created_at");
        if (ca != null) try { jc.setCreatedAt(LocalDateTime.parse(ca)); } catch (Exception ignored) {}
        String ua = rs.getString("updated_at");
        if (ua != null) try { jc.setUpdatedAt(LocalDateTime.parse(ua)); } catch (Exception ignored) {}
        return jc;
    }

    // ═══════════════════════════════════════════════════════════════════
    // QUOTATIONS
    // ═══════════════════════════════════════════════════════════════════

    public void insertQuotation(Quotation q) throws SQLException {
        String sql = "INSERT INTO quotations(" +
            "id,quotation_number,job_card_id,job_card_number,invoice_sale_id,customer_id,customer_name," +
            "customer_phone,subtotal,discount_amount,tax_amount,labour_total,grand_total," +
            "notes,status,created_by_id,created_by_name,valid_until,sync_status,created_at,updated_at) " +
            "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1,  q.getId());
            ps.setString(2,  q.getQuotationNumber());
            ps.setString(3,  q.getJobCardId());
            ps.setString(4,  q.getJobCardNumber());
            ps.setString(5,  q.getInvoiceSaleId());
            ps.setString(6,  q.getCustomerId());
            ps.setString(7,  q.getCustomerName());
            ps.setString(8,  q.getCustomerPhone());
            ps.setDouble(9,  q.getSubtotal());
            ps.setDouble(10, q.getDiscountAmount());
            ps.setDouble(11, q.getTaxAmount());
            ps.setDouble(12, q.getLabourTotal());
            ps.setDouble(13, q.getGrandTotal());
            ps.setString(14, q.getNotes());
            ps.setString(15, q.getStatus().name());
            ps.setString(16, q.getCreatedById());
            ps.setString(17, q.getCreatedByName());
            ps.setString(18, q.getValidUntil() != null ? q.getValidUntil().toString() : null);
            ps.setString(19, q.getSyncStatus());
            ps.setString(20, q.getCreatedAt().toString());
            ps.setString(21, q.getUpdatedAt().toString());
            ps.executeUpdate();
        }
        insertQuotationItems(q.getId(), q.getItems());
    }

    public void updateQuotation(Quotation q) throws SQLException {
        String sql = "UPDATE quotations SET " +
            "subtotal=?,discount_amount=?,tax_amount=?,labour_total=?,grand_total=?," +
            "notes=?,status=?,valid_until=?,sync_status=?,updated_at=? WHERE id=?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDouble(1,  q.getSubtotal());
            ps.setDouble(2,  q.getDiscountAmount());
            ps.setDouble(3,  q.getTaxAmount());
            ps.setDouble(4,  q.getLabourTotal());
            ps.setDouble(5,  q.getGrandTotal());
            ps.setString(6,  q.getNotes());
            ps.setString(7,  q.getStatus().name());
            ps.setString(8,  q.getValidUntil() != null ? q.getValidUntil().toString() : null);
            ps.setString(9,  q.getSyncStatus());
            ps.setString(10, q.getUpdatedAt().toString());
            ps.setString(11, q.getId());
            ps.executeUpdate();
        }
        deleteQuotationItems(q.getId());
        insertQuotationItems(q.getId(), q.getItems());
    }

    public void updateQuotationStatus(String id, Quotation.Status status) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE quotations SET status=?,sync_status='PENDING',updated_at=? WHERE id=?")) {
            ps.setString(1, status.name());
            ps.setString(2, LocalDateTime.now().toString());
            ps.setString(3, id);
            ps.executeUpdate();
        }
    }

    public void markQuotationInvoiced(String quotationId, String saleId) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE quotations SET status=?,invoice_sale_id=?,sync_status='PENDING',updated_at=? WHERE id=?")) {
            ps.setString(1, Quotation.Status.INVOICED.name());
            ps.setString(2, saleId);
            ps.setString(3, LocalDateTime.now().toString());
            ps.setString(4, quotationId);
            ps.executeUpdate();
        }
    }

    public Optional<Quotation> findQuotationById(String id) throws SQLException {
        String sql = "SELECT * FROM quotations WHERE id=?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Quotation q = mapQuotation(rs);
                q.setItems(findQuotationItems(id));
                return Optional.of(q);
            }
        }
        return Optional.empty();
    }

    public List<Quotation> findQuotationsByJobCard(String jobCardId) throws SQLException {
        String sql = "SELECT * FROM quotations WHERE job_card_id=? ORDER BY created_at DESC";
        List<Quotation> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, jobCardId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Quotation q = mapQuotation(rs);
                q.setItems(findQuotationItems(q.getId()));
                list.add(q);
            }
        }
        return list;
    }

    public List<Quotation> findAllQuotations() throws SQLException {
        String sql = "SELECT * FROM quotations ORDER BY created_at DESC LIMIT 500";
        List<Quotation> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Quotation q = mapQuotation(rs);
                q.setItems(findQuotationItems(q.getId()));
                list.add(q);
            }
        }
        return list;
    }

    public int nextQuotationSequence() throws SQLException {
        String today = LocalDateTime.now().toLocalDate().toString().replace("-", "");
        String sql = "SELECT COUNT(*) FROM quotations WHERE quotation_number LIKE ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "QT-" + today + "-%");
            ResultSet rs = ps.executeQuery();
            return (rs.next() ? rs.getInt(1) : 0) + 1;
        }
    }

    // ── Quotation Items ───────────────────────────────────────────────────────

    private void insertQuotationItems(String quotationId, List<Quotation.QuotationItem> items) throws SQLException {
        if (items == null || items.isEmpty()) return;
        String sql = "INSERT INTO quotation_items(" +
            "id,quotation_id,product_id,product_name,product_sku," +
            "quantity,unit_price,buying_price,discount,tax_rate,line_total) " +
            "VALUES(?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (Quotation.QuotationItem qi : items) {
                ps.setString(1,  qi.getId());
                ps.setString(2,  quotationId);
                ps.setString(3,  qi.getProductId());
                ps.setString(4,  qi.getProductName());
                ps.setString(5,  qi.getProductSku());
                ps.setInt(6,     qi.getQuantity());
                ps.setDouble(7,  qi.getUnitPrice());
                ps.setDouble(8,  qi.getBuyingPrice());
                ps.setDouble(9,  qi.getDiscount());
                ps.setDouble(10, qi.getTaxRate());
                ps.setDouble(11, qi.getLineTotal());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void deleteQuotationItems(String quotationId) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "DELETE FROM quotation_items WHERE quotation_id=?")) {
            ps.setString(1, quotationId);
            ps.executeUpdate();
        }
    }

    public List<Quotation.QuotationItem> findQuotationItems(String quotationId) throws SQLException {
        List<Quotation.QuotationItem> list = new ArrayList<>();
        String sql = "SELECT * FROM quotation_items WHERE quotation_id=?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, quotationId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Quotation.QuotationItem qi = new Quotation.QuotationItem();
                qi.setId(rs.getString("id"));
                qi.setQuotationId(rs.getString("quotation_id"));
                qi.setProductId(rs.getString("product_id"));
                qi.setProductName(rs.getString("product_name"));
                qi.setProductSku(rs.getString("product_sku"));
                qi.setQuantity(rs.getInt("quantity"));
                qi.setUnitPrice(rs.getDouble("unit_price"));
                qi.setBuyingPrice(rs.getDouble("buying_price"));
                qi.setDiscount(rs.getDouble("discount"));
                qi.setTaxRate(rs.getDouble("tax_rate"));
                qi.setLineTotal(rs.getDouble("line_total"));
                list.add(qi);
            }
        }
        return list;
    }

    // ── Map row → Quotation ───────────────────────────────────────────────────

    private Quotation mapQuotation(ResultSet rs) throws SQLException {
        Quotation q = new Quotation();
        q.setId(rs.getString("id"));
        q.setQuotationNumber(rs.getString("quotation_number"));
        q.setJobCardId(rs.getString("job_card_id"));
        q.setJobCardNumber(rs.getString("job_card_number"));
        q.setInvoiceSaleId(rs.getString("invoice_sale_id"));
        q.setCustomerId(rs.getString("customer_id"));
        q.setCustomerName(rs.getString("customer_name"));
        q.setCustomerPhone(rs.getString("customer_phone"));
        q.setSubtotal(rs.getDouble("subtotal"));
        q.setDiscountAmount(rs.getDouble("discount_amount"));
        q.setTaxAmount(rs.getDouble("tax_amount"));
        q.setLabourTotal(rs.getDouble("labour_total"));
        q.setGrandTotal(rs.getDouble("grand_total"));
        q.setNotes(rs.getString("notes"));
        try { q.setStatus(Quotation.Status.valueOf(rs.getString("status"))); }
        catch (Exception e) { q.setStatus(Quotation.Status.DRAFT); }
        q.setCreatedById(rs.getString("created_by_id"));
        q.setCreatedByName(rs.getString("created_by_name"));
        String vu = rs.getString("valid_until");
        if (vu != null && !vu.isBlank()) {
            try { q.setValidUntil(LocalDateTime.parse(vu)); } catch (Exception ignored) {}
        }
        q.setSyncStatus(rs.getString("sync_status"));
        String ca = rs.getString("created_at");
        if (ca != null) try { q.setCreatedAt(LocalDateTime.parse(ca)); } catch (Exception ignored) {}
        String ua = rs.getString("updated_at");
        if (ua != null) try { q.setUpdatedAt(LocalDateTime.parse(ua)); } catch (Exception ignored) {}
        return q;
    }
}
