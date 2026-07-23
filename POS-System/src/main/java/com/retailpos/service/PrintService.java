package com.retailpos.service;

import com.retailpos.model.AppSettings;
import com.retailpos.model.Sale;
import com.retailpos.repository.SaleRepository;
import com.retailpos.util.BarcodeUtil;
import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.time.format.DateTimeFormatter;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

public class PrintService {
    private static PrintService instance;
    private final SaleRepository saleRepo = new SaleRepository();

    private PrintService() {}

    public static synchronized PrintService getInstance() {
        if (instance == null) instance = new PrintService();
        return instance;
    }

    public List<String> getAvailablePrinters() {
        List<String> names = new ArrayList<>();
        javax.print.PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        for (javax.print.PrintService ps : services) names.add(ps.getName());
        return names;
    }

    public void printReceipt(Sale sale, AppSettings settings) {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Receipt-" + sale.getReceiptNumber());
        configurePrinter(job, settings.getPrinterName());
        job.setPrintable(new ReceiptPrintable(sale, settings), createPageFormat(settings.getPaperWidth()));
        try {
            job.print();
        } catch (PrinterException e) {
            System.err.println("[PrintService] Failed to print receipt: " + e.getMessage());
            throw new RuntimeException("Print failed: " + e.getMessage(), e);
        }
    }

    public void reprintReceipt(String saleId, AppSettings settings) throws Exception {
        Sale sale = saleRepo.findById(saleId)
            .orElseThrow(() -> new Exception("Sale not found: " + saleId));
        printReceipt(sale, settings);
    }

    public Path saveReceiptPdf(Sale sale, AppSettings settings) throws Exception {
        String safeReceipt = sale.getReceiptNumber().replaceAll("[^A-Za-z0-9_-]", "_");
        String timestamp = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
        Path file = com.retailpos.util.AppPaths.receiptDirectory().resolve("Receipt_" + safeReceipt + "_" + timestamp + ".pdf");
        com.itextpdf.text.Document document = new com.itextpdf.text.Document(com.itextpdf.text.PageSize.A6);
        com.itextpdf.text.pdf.PdfWriter.getInstance(document, new java.io.FileOutputStream(file.toFile()));
        document.open();
        com.itextpdf.text.Font title = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 14, com.itextpdf.text.Font.BOLD);
        document.add(new com.itextpdf.text.Paragraph(settings.getStoreName(), title));
        document.add(new com.itextpdf.text.Paragraph(settings.getStoreAddress()));
        document.add(new com.itextpdf.text.Paragraph("Receipt: " + sale.getReceiptNumber()));
        document.add(new com.itextpdf.text.Paragraph("Date: " + DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm").format(sale.getCreatedAt())));
        document.add(new com.itextpdf.text.Paragraph("--------------------------------"));
        for (Sale.SaleItem item : sale.getItems()) {
            document.add(new com.itextpdf.text.Paragraph(item.getProductName() + "\n" + item.getQuantity() + " x " + fmt(item.getUnitPrice()) + "     " + fmt(item.getLineTotal())));
        }
        document.add(new com.itextpdf.text.Paragraph("--------------------------------"));
        document.add(new com.itextpdf.text.Paragraph("Subtotal: " + fmt(sale.getSubtotal())));
        document.add(new com.itextpdf.text.Paragraph("Tax: " + fmt(sale.getTaxAmount())));
        document.add(new com.itextpdf.text.Paragraph("TOTAL: " + fmt(sale.getGrandTotal()), title));
        document.add(new com.itextpdf.text.Paragraph(settings.getStoreFooter()));
        document.close();
        return file;
    }

    public void testPrint(AppSettings settings) {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("TestPrint");
        configurePrinter(job, settings.getPrinterName());
        int cols = settings.getCharsPerLine();
        job.setPrintable((g, pf, page) -> {
            if (page > 0) return Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) g;
            g2.translate(pf.getImageableX(), pf.getImageableY());
            Font f = new Font("Monospaced", Font.PLAIN, 9);
            g2.setFont(f); g2.setColor(Color.BLACK);
            int y = 20;
            g2.drawString(center(settings.getStoreName(), cols), 10, y); y += 14;
            g2.drawString(repeat("-", cols), 10, y); y += 14;
            g2.drawString("TEST PRINT", 10, y); y += 14;
            g2.drawString("Printer is working correctly.", 10, y); y += 14;
            g2.drawString(repeat("-", cols), 10, y);
            return Printable.PAGE_EXISTS;
        }, createPageFormat(settings.getPaperWidth()));
        try { job.print(); } catch (PrinterException e) {
            throw new RuntimeException("Test print failed: " + e.getMessage(), e);
        }
    }

    private void configurePrinter(PrinterJob job, String printerName) {
        if (printerName != null && !printerName.isBlank()) {
            for (javax.print.PrintService ps : PrintServiceLookup.lookupPrintServices(null, null)) {
                if (ps.getName().equals(printerName)) {
                    try { job.setPrintService(ps); } catch (Exception ignored) {}
                    break;
                }
            }
        }
    }

    private PageFormat createPageFormat(int paperWidthMm) {
        PrinterJob job = PrinterJob.getPrinterJob();
        PageFormat pf = job.defaultPage();
        Paper paper = new Paper();
        double widthPt = paperWidthMm * 2.835; // mm to points
        double heightPt = 1000; // tall enough for receipt
        paper.setSize(widthPt, heightPt);
        paper.setImageableArea(5, 5, widthPt - 10, heightPt - 10);
        pf.setPaper(paper);
        pf.setOrientation(PageFormat.PORTRAIT);
        return pf;
    }

    private static class ReceiptPrintable implements Printable {
        private final Sale sale;
        private final AppSettings settings;
        private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        ReceiptPrintable(Sale sale, AppSettings settings) {
            this.sale = sale; this.settings = settings;
        }

        @Override
        public int print(Graphics graphics, PageFormat pf, int page) throws PrinterException {
            if (page > 0) return NO_SUCH_PAGE;
            Graphics2D g = (Graphics2D) graphics;
            g.translate(pf.getImageableX(), pf.getImageableY());
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int cols = settings.getCharsPerLine();
            Font bold = new Font("Monospaced", Font.BOLD, 9);
            Font normal = new Font("Monospaced", Font.PLAIN, 9);
            g.setColor(Color.BLACK);
            int y = 12, lh = 13;

            // Logo
            if (settings.getLogoPath() != null && !settings.getLogoPath().isBlank()) {
                try {
                    Image img = javax.imageio.ImageIO.read(new java.io.File(settings.getLogoPath()));
                    if (img != null) { g.drawImage(img, 10, y, 50, 30, null); y += 38; }
                } catch (Exception ignored) {}
            }

            // Header
            g.setFont(bold); g.drawString(center(settings.getStoreName(), cols), 10, y); y += lh;
            g.setFont(normal);
            if (!settings.getStoreAddress().isBlank()) { g.drawString(center(settings.getStoreAddress(), cols), 10, y); y += lh; }
            if (!settings.getStorePhone().isBlank()) { g.drawString(center("Tel: " + settings.getStorePhone(), cols), 10, y); y += lh; }
            g.drawString(repeat("-", cols), 10, y); y += lh;

            // Receipt info
            g.setFont(bold); g.drawString("RECEIPT", 10, y); y += lh; g.setFont(normal);
            g.drawString("Receipt#: " + sale.getReceiptNumber(), 10, y); y += lh;
            if (sale.getCreatedAt() != null) { g.drawString("Date: " + DT.format(sale.getCreatedAt()), 10, y); y += lh; }
            if (sale.getCashierName() != null) { g.drawString("Cashier: " + sale.getCashierName(), 10, y); y += lh; }
            g.drawString(repeat("-", cols), 10, y); y += lh;

            // Items
            for (Sale.SaleItem item : sale.getItems()) {
                String nameLine = truncate(item.getProductName(), cols - 12);
                g.drawString(nameLine, 10, y); y += lh;
                String priceLine = item.getQuantity() + " x " + fmt(item.getUnitPrice());
                String totalLine = fmt(item.getLineTotal());
                int pad = cols - priceLine.length() - totalLine.length();
                g.drawString(priceLine + repeat(" ", Math.max(1, pad)) + totalLine, 10, y); y += lh;
            }
            g.drawString(repeat("-", cols), 10, y); y += lh;

            // Totals
            g.drawString(padded("Subtotal:", fmt(sale.getSubtotal()), cols), 10, y); y += lh;
            if (sale.getDiscountAmount() > 0) {
                g.drawString(padded("Discount:", "-" + fmt(sale.getDiscountAmount()), cols), 10, y); y += lh;
            }
            if (sale.getTaxAmount() > 0) {
                g.drawString(padded("VAT:", fmt(sale.getTaxAmount()), cols), 10, y); y += lh;
            }
            g.setFont(bold);
            g.drawString(padded("TOTAL:", fmt(sale.getGrandTotal()), cols), 10, y); y += lh;
            g.setFont(normal);
            g.drawString(padded("Payment:", sale.getPaymentMethod(), cols), 10, y); y += lh;
            if ("CASH".equalsIgnoreCase(sale.getPaymentMethod()) && sale.getCashTendered() > 0) {
                g.drawString(padded("Cash:", fmt(sale.getCashTendered()), cols), 10, y); y += lh;
                g.drawString(padded("Change:", fmt(sale.getChange()), cols), 10, y); y += lh;
            }
            if (sale.getPaymentReference() != null && !sale.getPaymentReference().isBlank()) {
                g.drawString("Ref: " + sale.getPaymentReference(), 10, y); y += lh;
            }
            g.drawString(repeat("-", cols), 10, y); y += lh;

            // QR code
            try {
                BufferedImage qr = BarcodeUtil.generateQRCode(sale.getReceiptNumber(), 60);
                int qrX = (int)((pf.getImageableWidth() - 60) / 2);
                g.drawImage(qr, 10 + qrX, y, null); y += 68;
            } catch (Exception ignored) {}

            // Barcode
            try {
                BufferedImage bc = BarcodeUtil.generateBarcode(sale.getReceiptNumber(), (int)pf.getImageableWidth() - 20, 25);
                g.drawImage(bc, 10, y, null); y += 33;
            } catch (Exception ignored) {}

            // Footer
            if (!settings.getStoreFooter().isBlank()) {
                g.drawString(center(settings.getStoreFooter(), cols), 10, y); y += lh;
            }
            g.drawString(repeat(" ", cols), 10, y);
            return PAGE_EXISTS;
        }
    }

    static String center(String text, int width) {
        if (text == null) return "";
        if (text.length() >= width) return text;
        int pad = (width - text.length()) / 2;
        return repeat(" ", pad) + text;
    }

    static String padded(String left, String right, int width) {
        int pad = width - left.length() - right.length();
        return left + repeat(" ", Math.max(1, pad)) + right;
    }

    static String repeat(String s, int n) {
        if (n <= 0) return "";
        return s.repeat(n);
    }

    static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }

    static String fmt(double v) { return String.format("%.2f", v); }
}
