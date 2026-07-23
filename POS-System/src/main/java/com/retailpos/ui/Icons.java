package com.retailpos.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

/**
 * Renders all UI icons as Java2D vector drawings — no emoji, no external files.
 * Every icon is a 20×20 ImageIcon by default; call get(name, size) for custom sizes.
 */
public final class Icons {

    private Icons() {}

    public static final int DEFAULT = 20;

    public static ImageIcon get(String name, int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setColor(new Color(0, 0, 0, 0));
        g.fillRect(0, 0, size, size);
        draw(g, name, size);
        g.dispose();
        return new ImageIcon(img);
    }

    public static ImageIcon get(String name) { return get(name, DEFAULT); }

    // colour helpers
    private static Color ink(int alpha) { return new Color(30, 41, 59, alpha); }
    private static Color white()        { return Color.WHITE; }

    private static void draw(Graphics2D g, String name, int s) {
        float lw = s / 10f;
        g.setStroke(new BasicStroke(lw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        float p = s * 0.10f; // padding
        float w = s - 2 * p;
        switch (name) {
            case "cart"     -> drawCart(g, s, p, w, lw);
            case "search"   -> drawSearch(g, s, p, w, lw);
            case "user"     -> drawUser(g, s, p, w, lw);
            case "logout"   -> drawLogout(g, s, p, w, lw);
            case "dashboard"-> drawDashboard(g, s, p, w, lw);
            case "products" -> drawBox(g, s, p, w, lw);
            case "customers"-> drawCustomers(g, s, p, w, lw);
            case "inventory"-> drawInventory(g, s, p, w, lw);
            case "purchases"-> drawPurchases(g, s, p, w, lw);
            case "reports"  -> drawReports(g, s, p, w, lw);
            case "settings" -> drawSettings(g, s, p, w, lw);
            case "add"      -> drawAdd(g, s, p, w, lw);
            case "edit"     -> drawEdit(g, s, p, w, lw);
            case "delete"   -> drawDelete(g, s, p, w, lw);
            case "print"    -> drawPrint(g, s, p, w, lw);
            case "barcode"  -> drawBarcode(g, s, p, w, lw);
            case "refresh"  -> drawRefresh(g, s, p, w, lw);
            case "save"     -> drawSave(g, s, p, w, lw);
            case "backup"   -> drawSave(g, s, p, w, lw);
            case "sync"     -> drawRefresh(g, s, p, w, lw);
            case "pay"      -> drawPay(g, s, p, w, lw);
            case "suspend"  -> drawSuspend(g, s, p, w, lw);
            case "resume"   -> drawResume(g, s, p, w, lw);
            case "clear"    -> drawDelete(g, s, p, w, lw);
            case "check"    -> drawCheck(g, s, p, w, lw);
            case "warning"  -> drawWarning(g, s, p, w, lw);
            case "online"   -> drawDot(g, s, new Color(34, 197, 94));
            case "offline"  -> drawDot(g, s, new Color(239, 68, 68));
            case "syncing"  -> drawRefresh(g, s, p, w, lw);
            default         -> drawBox(g, s, p, w, lw);
        }
    }

    private static void drawCart(Graphics2D g, int s, float p, float w, float lw) {
        g.setColor(ink(220));
        // Basket body
        float bx = p, by = p + w * 0.3f, bw = w, bh = w * 0.55f;
        g.draw(new RoundRectangle2D.Float(bx, by, bw, bh, lw * 2, lw * 2));
        // Handle arc
        float ax = p + w * 0.2f, ay = p, aw = w * 0.6f, ah = w * 0.45f;
        g.draw(new Arc2D.Float(ax, ay, aw, ah, 0, 180, Arc2D.OPEN));
        // Wheels
        float wr = lw * 1.6f;
        g.fill(new Ellipse2D.Float(p + w * 0.2f - wr, by + bh - wr, wr * 2, wr * 2));
        g.fill(new Ellipse2D.Float(p + w * 0.7f - wr, by + bh - wr, wr * 2, wr * 2));
    }

    private static void drawSearch(Graphics2D g, int s, float p, float w, float lw) {
        g.setColor(ink(220));
        float r = w * 0.38f;
        float cx = p + r, cy = p + r;
        g.draw(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
        float hx1 = cx + r * 0.7f, hy1 = cy + r * 0.7f;
        float hx2 = p + w, hy2 = p + w;
        g.draw(new Line2D.Float(hx1, hy1, hx2, hy2));
    }

    private static void drawUser(Graphics2D g, int s, float p, float w, float lw) {
        g.setColor(ink(220));
        float hr = w * 0.28f, hcx = p + w / 2, hcy = p + w * 0.33f;
        g.draw(new Ellipse2D.Float(hcx - hr, hcy - hr, hr * 2, hr * 2));
        float by2 = p + w * 0.7f, bw2 = w * 0.9f, bh2 = w * 0.35f;
        g.draw(new Arc2D.Float(p + w * 0.05f, by2, bw2, bh2, 0, 180, Arc2D.OPEN));
    }

    private static void drawCustomers(Graphics2D g, int s, float p, float w, float lw) {
        g.setColor(ink(220));
        float r = w * 0.22f, cx = p + w * 0.38f, cy = p + w * 0.32f;
        g.draw(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
        g.draw(new Arc2D.Float(p + w * 0.02f, p + w * 0.62f, w * 0.7f, w * 0.35f, 0, 180, Arc2D.OPEN));
        float r2 = w * 0.18f, cx2 = p + w * 0.72f, cy2 = p + w * 0.3f;
        g.setColor(ink(140));
        g.draw(new Ellipse2D.Float(cx2 - r2, cy2 - r2, r2 * 2, r2 * 2));
        g.draw(new Arc2D.Float(p + w * 0.35f, p + w * 0.6f, w * 0.62f, w * 0.3f, 0, 180, Arc2D.OPEN));
    }

    private static void drawLogout(Graphics2D g, int s, float p, float w, float lw) {
        g.setColor(ink(220));
        float mx = p + w * 0.55f;
        g.draw(new RoundRectangle2D.Float(p, p + w * 0.1f, w * 0.6f, w * 0.8f, lw * 2, lw * 2));
        g.draw(new Line2D.Float(mx, p + w / 2, p + w, p + w / 2));
        float[] xa = {p + w - w * 0.22f, p + w, p + w - w * 0.22f};
        float[] ya = {p + w / 2 - w * 0.15f, p + w / 2, p + w / 2 + w * 0.15f};
        g.draw(new GeneralPath(new Polygon(toInt(xa), toInt(ya), 3)));
    }

    private static void drawDashboard(Graphics2D g, int s, float p, float w, float lw) {
        g.setColor(ink(220));
        float hw = w / 2 - lw, gap = lw;
        g.draw(new RoundRectangle2D.Float(p, p, hw, hw, lw, lw));
        g.draw(new RoundRectangle2D.Float(p + hw + gap * 2, p, hw, hw, lw, lw));
        g.draw(new RoundRectangle2D.Float(p, p + hw + gap * 2, hw, hw, lw, lw));
        g.draw(new RoundRectangle2D.Float(p + hw + gap * 2, p + hw + gap * 2, hw, hw, lw, lw));
    }

    private static void drawBox(Graphics2D g, int s, float p, float w, float lw) {
        g.setColor(ink(220));
        g.draw(new RoundRectangle2D.Float(p, p + w * 0.1f, w, w * 0.85f, lw * 2, lw * 2));
        g.draw(new Line2D.Float(p + w * 0.2f, p + w * 0.1f, p + w * 0.2f, p));
        g.draw(new Line2D.Float(p + w * 0.2f, p, p + w * 0.8f, p));
        g.draw(new Line2D.Float(p + w * 0.8f, p, p + w * 0.8f, p + w * 0.1f));
    }

    private static void drawInventory(Graphics2D g, int s, float p, float w, float lw) {
        g.setColor(ink(220));
        for (int i = 0; i < 3; i++) {
            float ry = p + i * (w / 3f + lw * 0.3f);
            g.draw(new RoundRectangle2D.Float(p, ry, w, w / 3f, lw, lw));
        }
    }

    private static void drawPurchases(Graphics2D g, int s, float p, float w, float lw) {
        g.setColor(ink(220));
        g.draw(new RoundRectangle2D.Float(p, p + w * 0.15f, w, w * 0.85f, lw * 2, lw * 2));
        float top = p + w * 0.15f;
        g.draw(new Line2D.Float(p + w * 0.3f, top, p + w * 0.3f, p));
        g.draw(new Line2D.Float(p + w * 0.3f, p, p + w * 0.7f, p));
        g.draw(new Line2D.Float(p + w * 0.7f, p, p + w * 0.7f, top));
        float ly = p + w * 0.45f, gy = w * 0.18f;
        for (int i = 0; i < 3; i++) g.draw(new Line2D.Float(p + w * 0.15f, ly + i * gy, p + w * 0.85f, ly + i * gy));
    }

    private static void drawReports(Graphics2D g, int s, float p, float w, float lw) {
        g.setColor(ink(220));
        g.draw(new RoundRectangle2D.Float(p, p, w, w, lw * 2, lw * 2));
        float[] bh = {0.55f, 0.35f, 0.7f, 0.45f};
        float bw2 = w * 0.14f, gap = w * 0.08f, by = p + w * 0.2f;
        for (int i = 0; i < 4; i++) {
            float bx2 = p + w * 0.12f + i * (bw2 + gap);
            float bbarH = w * bh[i];
            g.fill(new RoundRectangle2D.Float(bx2, by + (w * 0.7f - bbarH), bw2, bbarH, 1, 1));
        }
    }

    private static void drawSettings(Graphics2D g, int s, float p, float w, float lw) {
        g.setColor(ink(220));
        float cr = w * 0.22f, cx = p + w / 2, cy = p + w / 2;
        g.draw(new Ellipse2D.Float(cx - cr, cy - cr, cr * 2, cr * 2));
        float or2 = w * 0.46f;
        int teeth = 8;
        for (int i = 0; i < teeth; i++) {
            double a = 2 * Math.PI * i / teeth;
            float ix = cx + (float)(or2 * 0.72 * Math.cos(a));
            float iy = cy + (float)(or2 * 0.72 * Math.sin(a));
            float ox = cx + (float)(or2 * Math.cos(a));
            float oy = cy + (float)(or2 * Math.sin(a));
            g.setStroke(new BasicStroke(lw * 1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Float(ix, iy, ox, oy));
        }
    }

    private static void drawAdd(Graphics2D g, int s, float p, float w, float lw) {
        g.setColor(RetailThemeManager.PRIMARY);
        g.fill(new Ellipse2D.Float(p, p, w, w));
        g.setColor(white());
        g.setStroke(new BasicStroke(lw * 1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Float(p + w / 2, p + w * 0.25f, p + w / 2, p + w * 0.75f));
        g.draw(new Line2D.Float(p + w * 0.25f, p + w / 2, p + w * 0.75f, p + w / 2));
    }

    private static void drawEdit(Graphics2D g, int s, float p, float w, float lw) {
        g.setColor(ink(200));
        float[] px = {p + w * 0.12f, p + w * 0.12f, p + w * 0.88f};
        float[] py = {p + w * 0.5f, p + w * 0.88f, p + w * 0.12f};
        GeneralPath path = new GeneralPath();
        path.moveTo(px[0], py[0]);
        for (int i = 1; i < px.length; i++) path.lineTo(px[i], py[i]);
        path.closePath();
        g.draw(path);
        g.draw(new Line2D.Float(p + w * 0.12f, p + w * 0.88f, p + w * 0.35f, p + w * 0.88f));
    }

    private static void drawDelete(Graphics2D g, int s, float p, float w, float lw) {
        g.setColor(new Color(220, 38, 38, 200));
        g.draw(new RoundRectangle2D.Float(p + w * 0.15f, p + w * 0.25f, w * 0.7f, w * 0.65f, lw, lw));
        g.draw(new Line2D.Float(p + w * 0.05f, p + w * 0.25f, p + w * 0.95f, p + w * 0.25f));
        g.draw(new Line2D.Float(p + w * 0.35f, p + w * 0.25f, p + w * 0.35f, p + w * 0.1f));
        g.draw(new Line2D.Float(p + w * 0.35f, p + w * 0.1f, p + w * 0.65f, p + w * 0.1f));
        g.draw(new Line2D.Float(p + w * 0.65f, p + w * 0.1f, p + w * 0.65f, p + w * 0.25f));
        g.draw(new Line2D.Float(p + w / 2, p + w * 0.38f, p + w / 2, p + w * 0.82f));
    }

    private static void drawPrint(Graphics2D g, int s, float p, float w, float lw) {
        g.setColor(ink(220));
        g.draw(new RoundRectangle2D.Float(p, p + w * 0.3f, w, w * 0.45f, lw, lw));
        g.draw(new RoundRectangle2D.Float(p + w * 0.15f, p + w * 0.6f, w * 0.7f, w * 0.4f, lw, lw));
        g.draw(new Line2D.Float(p + w * 0.15f, p + w * 0.3f, p + w * 0.15f, p + w * 0.08f));
        g.draw(new Line2D.Float(p + w * 0.15f, p + w * 0.08f, p + w * 0.85f, p + w * 0.08f));
        g.draw(new Line2D.Float(p + w * 0.85f, p + w * 0.08f, p + w * 0.85f, p + w * 0.3f));
        float gy = p + w * 0.7f, gh = w * 0.1f;
        g.draw(new Line2D.Float(p + w * 0.3f, gy, p + w * 0.7f, gy));
        g.draw(new Line2D.Float(p + w * 0.3f, gy + gh, p + w * 0.7f, gy + gh));
    }

    private static void drawBarcode(Graphics2D g, int s, float p, float w, float lw) {
        g.setColor(ink(220));
        float[] widths = {1f, 0.5f, 1.5f, 0.5f, 1f, 0.5f, 1.5f, 0.5f, 1f};
        float x = p + w * 0.05f, bh2 = w * 0.75f, by = p + w * 0.1f;
        float totalW = 0; for (float fw : widths) totalW += fw;
        float scale = (w * 0.9f) / totalW;
        for (float fw : widths) {
            g.fill(new Rectangle2D.Float(x, by, fw * scale - 0.5f, bh2));
            x += fw * scale;
        }
        g.draw(new Line2D.Float(p, p + w * 0.9f, p + w, p + w * 0.9f));
    }

    private static void drawRefresh(Graphics2D g, int s, float p, float w, float lw) {
        g.setColor(ink(220));
        float r = w * 0.38f, cx = p + w / 2, cy = p + w / 2;
        g.draw(new Arc2D.Float(cx - r, cy - r, r * 2, r * 2, 30, 300, Arc2D.OPEN));
        float ax = cx + r, ay = cy;
        float[] arX = {ax - lw * 2.5f, ax + lw * 1.5f, ax + lw * 1.5f};
        float[] arY = {ay - lw * 3f, ay - lw * 3f, ay + lw * 3f};
        g.fill(new Polygon(toInt(arX), toInt(arY), 3));
    }

    private static void drawSave(Graphics2D g, int s, float p, float w, float lw) {
        g.setColor(ink(220));
        g.draw(new RoundRectangle2D.Float(p, p, w, w, lw, lw));
        g.draw(new RoundRectangle2D.Float(p + w * 0.25f, p, w * 0.5f, w * 0.35f, lw * 0.5f, lw * 0.5f));
        g.draw(new RoundRectangle2D.Float(p + w * 0.15f, p + w * 0.5f, w * 0.7f, w * 0.42f, lw, lw));
    }

    private static void drawPay(Graphics2D g, int s, float p, float w, float lw) {
        g.setColor(ink(220));
        g.draw(new RoundRectangle2D.Float(p, p + w * 0.15f, w, w * 0.7f, lw * 2, lw * 2));
        g.draw(new Line2D.Float(p, p + w * 0.35f, p + w, p + w * 0.35f));
        float cr = w * 0.18f, cx = p + w * 0.28f, cy = p + w * 0.62f;
        g.draw(new Ellipse2D.Float(cx - cr, cy - cr, cr * 2, cr * 2));
        g.draw(new Line2D.Float(p + w * 0.55f, cy - cr * 0.5f, p + w * 0.82f, cy - cr * 0.5f));
        g.draw(new Line2D.Float(p + w * 0.55f, cy + cr * 0.5f, p + w * 0.72f, cy + cr * 0.5f));
    }

    private static void drawSuspend(Graphics2D g, int s, float p, float w, float lw) {
        g.setColor(ink(200));
        float bw2 = w * 0.22f, bh2 = w * 0.75f, gap = w * 0.16f;
        float bx = p + (w - 2 * bw2 - gap) / 2;
        g.fill(new RoundRectangle2D.Float(bx, p + (w - bh2) / 2, bw2, bh2, lw, lw));
        g.fill(new RoundRectangle2D.Float(bx + bw2 + gap, p + (w - bh2) / 2, bw2, bh2, lw, lw));
    }

    private static void drawResume(Graphics2D g, int s, float p, float w, float lw) {
        g.setColor(RetailThemeManager.ACCENT);
        float cx = p + w / 2, cy = p + w / 2, r = w * 0.42f;
        float[] ptX = {cx - r * 0.35f, cx + r * 0.6f, cx - r * 0.35f};
        float[] ptY = {cy - r * 0.6f, cy, cy + r * 0.6f};
        GeneralPath tri = new GeneralPath();
        tri.moveTo(ptX[0], ptY[0]); tri.lineTo(ptX[1], ptY[1]); tri.lineTo(ptX[2], ptY[2]);
        tri.closePath(); g.fill(tri);
    }

    private static void drawCheck(Graphics2D g, int s, float p, float w, float lw) {
        g.setColor(RetailThemeManager.ACCENT);
        g.setStroke(new BasicStroke(lw * 1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Float(p + w * 0.12f, p + w * 0.52f, p + w * 0.42f, p + w * 0.78f));
        g.draw(new Line2D.Float(p + w * 0.42f, p + w * 0.78f, p + w * 0.88f, p + w * 0.22f));
    }

    private static void drawWarning(Graphics2D g, int s, float p, float w, float lw) {
        g.setColor(new Color(202, 138, 4, 220));
        float[] tx = {p + w / 2, p + w, p};
        float[] ty = {p, p + w, p + w};
        g.fill(new Polygon(toInt(tx), toInt(ty), 3));
        g.setColor(white());
        g.setStroke(new BasicStroke(lw * 1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Float(p + w / 2, p + w * 0.38f, p + w / 2, p + w * 0.65f));
        g.fill(new Ellipse2D.Float(p + w / 2 - lw * 0.8f, p + w * 0.72f, lw * 1.6f, lw * 1.6f));
    }

    private static void drawDot(Graphics2D g, int s, Color c) {
        g.setColor(c);
        float r = s * 0.35f;
        g.fill(new Ellipse2D.Float(s / 2f - r, s / 2f - r, r * 2, r * 2));
    }

    private static int[] toInt(float[] f) {
        int[] r = new int[f.length];
        for (int i = 0; i < f.length; i++) r[i] = Math.round(f[i]);
        return r;
    }
}
