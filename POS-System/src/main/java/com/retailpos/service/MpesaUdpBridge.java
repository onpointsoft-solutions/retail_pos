package com.retailpos.service;

import com.google.gson.Gson;
import com.retailpos.util.DatabaseManager;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** LAN-only receiver for transaction notices sent by the paired BizFlow Bridge phone. */
public final class MpesaUdpBridge {
    public static final int PORT = 45876;
    private static final MpesaUdpBridge INSTANCE = new MpesaUdpBridge();
    private final List<PaymentNotice> recent = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean running;

    private MpesaUdpBridge() { }
    public static MpesaUdpBridge getInstance() { return INSTANCE; }

    public void start() {
        if (running) return;
        running = true;
        Thread listener = new Thread(this::listen, "RetailPOS-Mpesa-UDP");
        listener.setDaemon(true);
        listener.start();
    }

    private void listen() {
        try (DatagramSocket socket = new DatagramSocket(PORT)) {
            byte[] buffer = new byte[4096];
            Gson gson = new Gson();
            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String json = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
                PaymentNotice notice = gson.fromJson(json, PaymentNotice.class);
                if (notice == null || notice.code == null || !notice.code.matches("[A-Za-z0-9]{6,16}")) continue;
                if (!saveNotice(notice)) continue;
                synchronized (recent) {
                    recent.removeIf(existing -> notice.code.equalsIgnoreCase(existing.code));
                    recent.add(0, notice);
                    if (recent.size() > 50) recent.remove(recent.size() - 1);
                }
                // The receiving computer is the bridge's relay point. Upload
                // immediately so every other connected station receives the
                // M-Pesa confirmation through normal incremental sync.
                com.retailpos.sync.SyncService.getInstance().notifyLocalChange();
                byte[] acknowledgement = ("ACK:" + notice.code).getBytes(StandardCharsets.UTF_8);
                try {
                    socket.send(new DatagramPacket(
                        acknowledgement,
                        acknowledgement.length,
                        packet.getAddress(),
                        packet.getPort()
                    ));
                } catch (Exception exception) {
                    System.err.println("[MpesaUdpBridge] Acknowledgement failed: " + exception.getMessage());
                }
            }
        } catch (Exception exception) {
            running = false;
            System.err.println("[MpesaUdpBridge] Listener stopped: " + exception.getMessage());
        }
    }

    public List<PaymentNotice> recentPayments() {
        List<PaymentNotice> persisted = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT code, customer_name, amount, received_at FROM mpesa_transactions " +
                 "ORDER BY received_at DESC LIMIT 50");
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                PaymentNotice notice = new PaymentNotice();
                notice.code = rows.getString("code");
                notice.customerName = rows.getString("customer_name");
                notice.amount = rows.getString("amount");
                notice.receivedAt = rows.getLong("received_at");
                persisted.add(notice);
            }
            return persisted;
        } catch (Exception exception) {
            synchronized (recent) { return new ArrayList<>(recent); }
        }
    }

    private boolean saveNotice(PaymentNotice notice) {
        String sql = "INSERT INTO mpesa_transactions " +
            "(id,code,customer_name,amount,received_at,sync_status,created_at,updated_at) " +
            "VALUES (?,?,?,?,?,'PENDING',datetime('now'),datetime('now')) " +
            "ON CONFLICT(id) DO UPDATE SET customer_name=excluded.customer_name, " +
            "amount=excluded.amount, received_at=excluded.received_at, " +
            "sync_status=CASE WHEN mpesa_transactions.sync_status='SYNCED' THEN 'MODIFIED' " +
            "ELSE mpesa_transactions.sync_status END, updated_at=datetime('now')";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, notice.code);
            statement.setString(2, notice.code);
            statement.setString(3, notice.customerName);
            statement.setString(4, notice.amount);
            statement.setLong(5, notice.receivedAt);
            statement.executeUpdate();
            return true;
        } catch (Exception exception) {
            System.err.println("[MpesaUdpBridge] Could not save transaction: " + exception.getMessage());
            return false;
        }
    }

    public static class PaymentNotice {
        public String code;
        public String customerName;
        public String amount;
        public long receivedAt;

        @Override public String toString() {
            return code + "  |  KES " + amount + "  |  " + customerName;
        }
    }
}
