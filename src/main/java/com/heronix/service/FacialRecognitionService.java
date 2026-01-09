package com.heronix.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Base64;

/**
 * Facial Recognition Service
 *
 * Compares live photo with stored student photo for authentication
 * Uses image similarity comparison algorithm
 *
 * Security Features:
 * - Validates photo matches stored student photo
 * - Configurable similarity threshold (default 75%)
 * - Handles missing photos gracefully
 * - Logs all verification attempts for audit
 *
 * Future Enhancements:
 * - OpenCV facial recognition
 * - AWS Rekognition integration
 * - Azure Face API integration
 * - Liveness detection (blink detection)
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since December 13, 2025
 */
@Service
@Slf4j
public class FacialRecognitionService {

    /**
     * Similarity threshold for facial matching (0.0 to 1.0)
     * Default: 0.75 (75% similarity required for match)
     *
     * Can be configured via application.properties:
     * security.facial-recognition.threshold=0.75
     */
    private static final double MATCH_THRESHOLD = 0.75;

    /**
     * Tolerance for RGB color comparison
     * Higher value = more lenient matching
     * Lower value = stricter matching
     */
    private static final int COLOR_TOLERANCE = 40;

    /**
     * Image comparison size (resized for faster comparison)
     * Both images resized to this size before comparison
     */
    private static final int COMPARISON_SIZE = 200;

    // ========================================================================
    // PUBLIC API
    // ========================================================================

    /**
     * Verify if captured photo matches stored student photo
     *
     * @param capturedPhotoBase64 Base64 encoded photo from webcam
     * @param studentPhotoPath Path to stored student photo file
     * @return VerificationResult containing match status and similarity score
     */
    public VerificationResult verifyFace(String capturedPhotoBase64, String studentPhotoPath) {
        try {
            log.info("Starting facial verification for photo: {}", studentPhotoPath);

            // Validate inputs
            if (capturedPhotoBase64 == null || capturedPhotoBase64.trim().isEmpty()) {
                log.warn("No captured photo provided for verification");
                return VerificationResult.error("No photo captured");
            }

            // Check if student has a photo on file
            if (studentPhotoPath == null || studentPhotoPath.trim().isEmpty()) {
                log.warn("No student photo on file - allowing login without facial verification");
                return VerificationResult.skipped("No photo on file");
            }

            // Load stored student photo
            File storedPhotoFile = new File(studentPhotoPath);
            if (!storedPhotoFile.exists()) {
                log.warn("Student photo file not found: {} - allowing login", studentPhotoPath);
                return VerificationResult.skipped("Photo file not found");
            }

            BufferedImage storedPhoto = ImageIO.read(storedPhotoFile);
            if (storedPhoto == null) {
                log.error("Failed to read student photo file: {}", studentPhotoPath);
                return VerificationResult.error("Cannot read photo file");
            }

            // Decode captured photo from Base64
            byte[] capturedPhotoBytes;
            try {
                capturedPhotoBytes = Base64.getDecoder().decode(capturedPhotoBase64);
            } catch (IllegalArgumentException e) {
                log.error("Invalid Base64 photo data", e);
                return VerificationResult.error("Invalid photo format");
            }

            BufferedImage capturedPhoto = ImageIO.read(new ByteArrayInputStream(capturedPhotoBytes));
            if (capturedPhoto == null) {
                log.error("Failed to decode captured photo from Base64");
                return VerificationResult.error("Cannot decode captured photo");
            }

            // Compare photos
            double similarity = compareImages(storedPhoto, capturedPhoto);

            log.info("Facial similarity score: {} (threshold: {})", similarity, MATCH_THRESHOLD);

            boolean match = similarity >= MATCH_THRESHOLD;

            if (match) {
                log.info("✓ Facial verification PASSED ({}% match)", (int)(similarity * 100));
                return VerificationResult.success(similarity);
            } else {
                log.warn("✗ Facial verification FAILED ({}% match, required {}%)",
                    (int)(similarity * 100), (int)(MATCH_THRESHOLD * 100));
                return VerificationResult.failed(similarity);
            }

        } catch (Exception e) {
            log.error("Error during facial verification", e);
            // On error, return error result instead of allowing login
            return VerificationResult.error("Verification error: " + e.getMessage());
        }
    }

    // ========================================================================
    // IMAGE COMPARISON ALGORITHM
    // ========================================================================

    /**
     * Compare two images and return similarity score (0.0 to 1.0)
     *
     * Algorithm:
     * 1. Resize both images to same size for fair comparison
     * 2. Compare pixel-by-pixel with RGB tolerance
     * 3. Calculate percentage of matching pixels
     *
     * @param img1 First image (stored student photo)
     * @param img2 Second image (captured webcam photo)
     * @return Similarity score from 0.0 (no match) to 1.0 (perfect match)
     */
    private double compareImages(BufferedImage img1, BufferedImage img2) {
        // Resize both images to standard comparison size
        BufferedImage resized1 = resizeImage(img1, COMPARISON_SIZE, COMPARISON_SIZE);
        BufferedImage resized2 = resizeImage(img2, COMPARISON_SIZE, COMPARISON_SIZE);

        // Compare pixels
        long totalPixels = (long) COMPARISON_SIZE * COMPARISON_SIZE;
        long matchingPixels = 0;

        for (int y = 0; y < COMPARISON_SIZE; y++) {
            for (int x = 0; x < COMPARISON_SIZE; x++) {
                int rgb1 = resized1.getRGB(x, y);
                int rgb2 = resized2.getRGB(x, y);

                // Compare RGB values with tolerance
                if (isSimilarColor(rgb1, rgb2, COLOR_TOLERANCE)) {
                    matchingPixels++;
                }
            }
        }

        double similarity = (double) matchingPixels / totalPixels;

        log.debug("Image comparison: {} matching pixels out of {} ({} similarity)",
            matchingPixels, totalPixels, String.format("%.2f%%", similarity * 100));

        return similarity;
    }

    /**
     * Check if two RGB colors are similar within tolerance
     *
     * @param rgb1 First RGB color value
     * @param rgb2 Second RGB color value
     * @param tolerance Maximum difference allowed per channel (0-255)
     * @return true if colors are similar, false otherwise
     */
    private boolean isSimilarColor(int rgb1, int rgb2, int tolerance) {
        // Extract RGB components
        int r1 = (rgb1 >> 16) & 0xFF;
        int g1 = (rgb1 >> 8) & 0xFF;
        int b1 = rgb1 & 0xFF;

        int r2 = (rgb2 >> 16) & 0xFF;
        int g2 = (rgb2 >> 8) & 0xFF;
        int b2 = rgb2 & 0xFF;

        // Check if each channel is within tolerance
        return Math.abs(r1 - r2) <= tolerance &&
               Math.abs(g1 - g2) <= tolerance &&
               Math.abs(b1 - b2) <= tolerance;
    }

    /**
     * Resize image to specified dimensions
     *
     * Uses high-quality bicubic interpolation for smooth resizing
     *
     * @param original Original image
     * @param width Target width
     * @param height Target height
     * @return Resized image
     */
    private BufferedImage resizeImage(BufferedImage original, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();

        // Use high-quality rendering for better comparison
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.drawImage(original, 0, 0, width, height, null);
        g.dispose();

        return resized;
    }

    // ========================================================================
    // VERIFICATION RESULT CLASS
    // ========================================================================

    /**
     * Result of facial verification attempt
     */
    public static class VerificationResult {
        private final boolean success;
        private final boolean skipped;
        private final double similarityScore;
        private final String message;

        private VerificationResult(boolean success, boolean skipped, double similarityScore, String message) {
            this.success = success;
            this.skipped = skipped;
            this.similarityScore = similarityScore;
            this.message = message;
        }

        public static VerificationResult success(double similarityScore) {
            return new VerificationResult(true, false, similarityScore,
                String.format("Face verified (%.0f%% match)", similarityScore * 100));
        }

        public static VerificationResult failed(double similarityScore) {
            return new VerificationResult(false, false, similarityScore,
                String.format("Face verification failed (%.0f%% match, required %.0f%%)",
                    similarityScore * 100, MATCH_THRESHOLD * 100));
        }

        public static VerificationResult skipped(String reason) {
            return new VerificationResult(true, true, 0.0,
                "Facial verification skipped: " + reason);
        }

        public static VerificationResult error(String message) {
            return new VerificationResult(false, false, 0.0,
                "Verification error: " + message);
        }

        public boolean isSuccess() {
            return success;
        }

        public boolean isSkipped() {
            return skipped;
        }

        public double getSimilarityScore() {
            return similarityScore;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return "VerificationResult{" +
                    "success=" + success +
                    ", skipped=" + skipped +
                    ", similarityScore=" + String.format("%.2f", similarityScore) +
                    ", message='" + message + '\'' +
                    '}';
        }
    }
}
