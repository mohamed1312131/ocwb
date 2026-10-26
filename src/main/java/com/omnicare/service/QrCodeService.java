package com.omnicare.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;

@Service
public class QrCodeService {

    private static final int QR_SIZE = 700;
    private static final int LOGO_SIZE = 112;
    private static final int BADGE_PADDING = 14;

    private final BufferedImage logo = loadLogo();

    public byte[] generatePng(String value) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 2);

            BitMatrix matrix = new QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints);
            BufferedImage monochromeQr = MatrixToImageWriter.toBufferedImage(matrix);
            BufferedImage qrImage = new BufferedImage(QR_SIZE, QR_SIZE, BufferedImage.TYPE_INT_RGB);
            Graphics2D canvas = qrImage.createGraphics();
            try {
                canvas.setColor(Color.WHITE);
                canvas.fillRect(0, 0, QR_SIZE, QR_SIZE);
                canvas.drawImage(monochromeQr, 0, 0, null);
            } finally {
                canvas.dispose();
            }
            addCenteredLogo(qrImage);

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(qrImage, "PNG", output);
            return output.toByteArray();
        } catch (WriterException | IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not generate QR code", exception);
        }
    }

    private void addCenteredLogo(BufferedImage qrImage) {
        Graphics2D graphics = qrImage.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

            int badgeSize = LOGO_SIZE + (BADGE_PADDING * 2);
            int badgePosition = (QR_SIZE - badgeSize) / 2;
            graphics.setColor(Color.WHITE);
            graphics.fill(new RoundRectangle2D.Double(
                badgePosition,
                badgePosition,
                badgeSize,
                badgeSize,
                28,
                28
            ));

            int logoPosition = (QR_SIZE - LOGO_SIZE) / 2;
            graphics.drawImage(logo, logoPosition, logoPosition, LOGO_SIZE, LOGO_SIZE, null);
        } finally {
            graphics.dispose();
        }
    }

    private BufferedImage loadLogo() {
        try (InputStream input = QrCodeService.class.getResourceAsStream("/omnicare-logo.base64")) {
            if (input == null) {
                throw new IllegalStateException("OmniCare QR logo resource is missing");
            }

            String encodedLogo = new String(input.readAllBytes(), StandardCharsets.US_ASCII);
            byte[] logoBytes = Base64.getMimeDecoder().decode(encodedLogo);
            BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(logoBytes));
            if (image == null) {
                throw new IllegalStateException("OmniCare QR logo resource is invalid");
            }
            return image;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load the OmniCare QR logo", exception);
        }
    }
}
