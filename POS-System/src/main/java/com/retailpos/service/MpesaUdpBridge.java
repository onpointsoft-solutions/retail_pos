package com.retailpos.service;

import com.google.gson.Gson;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** LAN-only receiver for transaction notices sent by the paired TransRouter phone. */
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
                synchronized (recent) {
                    recent.removeIf(existing -> notice.code.equalsIgnoreCase(existing.code));
                    recent.add(0, notice);
                    if (recent.size() > 50) recent.remove(recent.size() - 1);
                }
            }
        } catch (Exception exception) {
            running = false;
            System.err.println("[MpesaUdpBridge] Listener stopped: " + exception.getMessage());
        }
    }

    public List<PaymentNotice> recentPayments() {
        synchronized (recent) { return new ArrayList<>(recent); }
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
