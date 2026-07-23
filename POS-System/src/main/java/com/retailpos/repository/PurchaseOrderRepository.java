package com.retailpos.repository;

import com.retailpos.model.PurchaseOrder;
import com.retailpos.util.DatabaseManager;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class PurchaseOrderRepository {

    private PurchaseOrder mapPO(ResultSet r) throws SQLException {
        PurchaseOrder po = new PurchaseOrder();
        po.setId(r.getString("id")); po.setSupplierId(r.getString("supplier_id"));
        po.setSupplierName(r.getString("supplier_name")); po.setStatus(r.getString("status"));
        po.setNotes(r.getString("notes")); po.setSyncStatus(r.getString("sync_status"));
        String dd = r.getString("expected_delivery_date"); if (dd != null) po.setExpectedDeliveryDate(LocalDate.parse(dd));
        String ca = r.getString("created_at"); if (ca != null) po.setCreatedAt(LocalDateTime.parse(ca));
        String ua = r.getString("updated_at"); if (ua != null) po.setUpdatedAt(LocalDateTime.parse(ua));
        return po;
    }

    private void loadItems(PurchaseOrder po) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM purchase_order_items WHERE po_id=?")) {
            ps.setString(1, po.getId());
            ResultSet r = ps.executeQuery();
            List<PurchaseOrder.PurchaseOrderItem> items = new ArrayList<>();
            while (r.next()) {
                PurchaseOrder.PurchaseOrderItem item = new PurchaseOrder.PurchaseOrderItem();
                item.setProductId(r.getString("product_id")); item.setProductName(r.getString("product_name"));
                item.setOrderedQty(r.getInt("ordered_qty")); item.setReceivedQty(r.getInt("received_qty"));
                item.setBuyingPrice(r.getDouble("buying_price"));
                items.add(item);
            }
            po.setItems(items);
        }
    }

    public void insert(PurchaseOrder po) throws SQLException {
        try (Connection c = DatabaseManager.getConnection()) {
            c.setAutoCommit(false);
            try {
                String sql = "INSERT INTO purchase_orders(id,supplier_id,supplier_name,status,expected_delivery_date," +
                    "notes,sync_status,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?)";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    String now = LocalDateTime.now().toString();
                    ps.setString(1, po.getId()); ps.setString(2, po.getSupplierId());
                    ps.setString(3, po.getSupplierName()); ps.setString(4, po.getStatus());
                    ps.setString(5, po.getExpectedDeliveryDate() != null ? po.getExpectedDeliveryDate().toString() : null);
                    ps.setString(6, po.getNotes()); ps.setString(7, "PENDING");
                    ps.setString(8, now); ps.setString(9, now);
                    ps.executeUpdate();
                }
                for (PurchaseOrder.PurchaseOrderItem item : po.getItems()) {
                    String iSql = "INSERT INTO purchase_order_items(id,po_id,product_id,product_name,ordered_qty,received_qty,buying_price) VALUES(?,?,?,?,?,?,?)";
                    try (PreparedStatement ps = c.prepareStatement(iSql)) {
                        ps.setString(1, java.util.UUID.randomUUID().toString()); ps.setString(2, po.getId());
                        ps.setString(3, item.getProductId()); ps.setString(4, item.getProductName());
                        ps.setInt(5, item.getOrderedQty()); ps.setInt(6, item.getReceivedQty());
                        ps.setDouble(7, item.getBuyingPrice());
                        ps.executeUpdate();
                    }
                }
                c.commit();
            } catch (Exception e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    public Optional<PurchaseOrder> findById(String id) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM purchase_orders WHERE id=?")) {
            ps.setString(1, id);
            ResultSet r = ps.executeQuery();
            if (!r.next()) return Optional.empty();
            PurchaseOrder po = mapPO(r);
            loadItems(po);
            return Optional.of(po);
        }
    }

    public List<PurchaseOrder> findBySupplierId(String supplierId) throws SQLException {
        List<PurchaseOrder> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM purchase_orders WHERE supplier_id=? ORDER BY created_at DESC")) {
            ps.setString(1, supplierId);
            ResultSet r = ps.executeQuery();
            while (r.next()) { PurchaseOrder po = mapPO(r); loadItems(po); list.add(po); }
        }
        return list;
    }

    public List<PurchaseOrder> findAll() throws SQLException {
        List<PurchaseOrder> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM purchase_orders ORDER BY created_at DESC")) {
            ResultSet r = ps.executeQuery();
            while (r.next()) { PurchaseOrder po = mapPO(r); loadItems(po); list.add(po); }
        }
        return list;
    }

    public void updateStatus(String id, String status) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                "UPDATE purchase_orders SET status=?,sync_status='MODIFIED',updated_at=? WHERE id=?")) {
            ps.setString(1, status); ps.setString(2, LocalDateTime.now().toString()); ps.setString(3, id);
            ps.executeUpdate();
        }
    }

    public void updateReceivedQty(String poId, String productId, int receivedQty) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                "UPDATE purchase_order_items SET received_qty=? WHERE po_id=? AND product_id=?")) {
            ps.setInt(1, receivedQty); ps.setString(2, poId); ps.setString(3, productId);
            ps.executeUpdate();
        }
    }
}
