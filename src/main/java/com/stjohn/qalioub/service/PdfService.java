package com.stjohn.qalioub.service;

import com.stjohn.qalioub.entity.CarParking;
import com.stjohn.qalioub.repository.CarParkingRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Service
public class PdfService {

    private final CarParkingRepository carParkingRepository;

    public PdfService(CarParkingRepository carParkingRepository) {
        this.carParkingRepository = carParkingRepository;
    }

    @Transactional(readOnly = true)
    public byte[] generateCarParkingsPdf() {
        List<CarParking> carParkings = carParkingRepository.findAllByOrderByReservationIdAscIdAsc();

        try (PDDocument document = new PDDocument()) {
            if (carParkings.isEmpty()) {
                addTextPage(document, "No car parkings found.");
            } else {
                for (CarParking cp : carParkings) {
                    addCarParkingPage(document, cp);
                }
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate car parkings PDF", e);
        }
    }

    private void addCarParkingPage(PDDocument document, CarParking cp) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        float pageWidth  = PDRectangle.A4.getWidth();   // 595.28 pt
        float pageHeight = PDRectangle.A4.getHeight();  // 841.89 pt
        float margin = 50f;

        try (PDPageContentStream cs = new PDPageContentStream(document, page)) {

            // ── Header text (ASCII-safe fields only; Arabic names replaced with '?') ──
            float y = pageHeight - margin;
            writeText(cs, PDType1Font.HELVETICA_BOLD, 13, margin, y,
                    "Car Parking #" + cp.getId());
            y -= 20;
            writeText(cs, PDType1Font.HELVETICA, 10, margin, y,
                    "Reservation: #" + cp.getReservation().getId() +
                    "  |  Phone: " + cp.getReservation().getUser().getPhone());
            y -= 16;
            String statusLine = "Status: " + cp.getStatus().name();
            if (cp.getConfirmedBy() != null) {
                statusLine += "  |  Confirmed by: " + sanitize(cp.getConfirmedBy());
            }
            writeText(cs, PDType1Font.HELVETICA, 10, margin, y, statusLine);
            y -= 16;
            writeText(cs, PDType1Font.HELVETICA, 10, margin, y,
                    "User name: " + (cp.getReservation().getUser().getName() != null ? cp.getReservation().getUser().getName() : ""));
            y -= 20;

            // ── Horizontal rule ──
            cs.setLineWidth(0.5f);
            cs.moveTo(margin, y);
            cs.lineTo(pageWidth - margin, y);
            cs.stroke();
            y -= 15;

            // ── Driving licence image ──
            String imgPath = cp.getDrivingLicensePhotoPath();
            if (imgPath != null && Files.exists(Paths.get(imgPath))) {
                try {
                    PDImageXObject image = PDImageXObject.createFromFile(imgPath, document);
                    float maxW  = pageWidth - 2 * margin;
                    float maxH  = y - margin;
                    float scale = Math.min(maxW / image.getWidth(), maxH / image.getHeight());
                    scale = Math.min(scale, 1.0f); // never upscale
                    float imgW  = image.getWidth()  * scale;
                    float imgH  = image.getHeight() * scale;
                    float imgX  = margin + (maxW - imgW) / 2f; // center horizontally
                    cs.drawImage(image, imgX, y - imgH, imgW, imgH);
                } catch (Exception e) {
                    writeText(cs, PDType1Font.HELVETICA, 10, margin, y,
                            "[Error loading image: " + sanitize(e.getMessage()) + "]");
                }
            } else {
                writeText(cs, PDType1Font.HELVETICA, 10, margin, y,
                        "[Image file not found]");
                y -= 14;
                writeText(cs, PDType1Font.HELVETICA, 8, margin, y,
                        sanitize(imgPath != null ? imgPath : "path is null"));
            }
        }
    }

    private void addTextPage(PDDocument document, String message) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
            writeText(cs, PDType1Font.HELVETICA, 12, 50,
                    PDRectangle.A4.getHeight() / 2f, message);
        }
    }

    private void writeText(PDPageContentStream cs, PDFont font, float size,
                            float x, float y, String text) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(sanitize(text));
        cs.endText();
    }

    /** Replace non-Latin-1 characters (e.g. Arabic) with '?' to keep Type1 font happy. */
    private String sanitize(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            sb.append(c < 256 ? c : '?');
        }
        return sb.toString();
    }
}
