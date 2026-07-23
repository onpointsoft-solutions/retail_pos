package com.retailpos.util;

import com.google.zxing.*;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.qrcode.QRCodeWriter;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class BarcodeUtil {

    private BarcodeUtil() {}

    /**
     * Generate a Code 128 barcode image.
     */
    public static BufferedImage generateBarcode(String text, int width, int height) throws Exception {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("Barcode text cannot be empty");
        Code128Writer writer = new Code128Writer();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.MARGIN, 2);
        BitMatrix matrix = writer.encode(text, BarcodeFormat.CODE_128, width, height, hints);
        return MatrixToImageWriter.toBufferedImage(matrix);
    }

    /**
     * Generate a QR code image.
     */
    public static BufferedImage generateQRCode(String text, int size) throws Exception {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("QR text cannot be empty");
        QRCodeWriter writer = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);
        BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size, hints);
        return MatrixToImageWriter.toBufferedImage(matrix);
    }
}
