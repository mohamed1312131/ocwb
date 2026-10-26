package com.omnicare.service;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QrCodeServiceTest {

    @Test
    void generatedBrandedQrCodeRemainsScannable() throws Exception {
        String expectedUrl = "https://ocwb-production-c5da.up.railway.app/api/public/shares/test-token";
        byte[] qrCode = new QrCodeService().generatePng(expectedUrl);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(qrCode));

        BinaryBitmap bitmap = new BinaryBitmap(
            new HybridBinarizer(new BufferedImageLuminanceSource(image))
        );
        Result decoded = new MultiFormatReader().decode(bitmap);

        assertEquals(expectedUrl, decoded.getText());
    }
}
