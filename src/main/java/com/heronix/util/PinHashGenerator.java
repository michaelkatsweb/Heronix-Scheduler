package com.heronix.util;

import org.springframework.security.crypto.bcrypt.BCrypt;

/**
 * PIN Hash Generator Utility
 *
 * Generates BCrypt hashes for 4-digit PINs
 * Use this to create PIN hashes for testing or production setup
 *
 * Usage:
 * java -cp target/classes com.heronix.util.PinHashGenerator 1234
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since December 13, 2025
 */
public class PinHashGenerator {

    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════╗");
        System.out.println("║   Heronix Scheduling System - PIN Hash Generator      ║");
        System.out.println("╚═══════════════════════════════════════════════╝");
        System.out.println();

        if (args.length == 0) {
            // No arguments - generate hashes for common PINs
            System.out.println("Generating hashes for common test PINs...");
            System.out.println();

            String[] commonPins = {"1234", "0000", "9999", "1111", "5555"};

            for (String pin : commonPins) {
                generateAndPrint(pin);
            }

            System.out.println();
            System.out.println("Usage: java PinHashGenerator <pin>");
            System.out.println("Example: java PinHashGenerator 1234");

        } else {
            // Generate hash for provided PIN
            String pin = args[0];

            // Validate PIN format
            if (!isValidPin(pin)) {
                System.err.println("❌ Error: PIN must be exactly 4 digits");
                System.err.println("   Example: 1234, 0000, 9999");
                System.exit(1);
            }

            generateAndPrint(pin);

            // Also show SQL update statement
            System.out.println();
            System.out.println("SQL Update Statement:");
            System.out.println("─────────────────────────────────────────────");
            System.out.println("UPDATE students");
            System.out.println("SET pin_hash = '" + BCrypt.hashpw(pin, BCrypt.gensalt(12)) + "',");
            System.out.println("    pin_required = TRUE");
            System.out.println("WHERE student_id = 'S12345';  -- Replace with actual student ID");
        }

        System.out.println();
        System.out.println("═══════════════════════════════════════════════");
    }

    /**
     * Generate BCrypt hash and print to console
     */
    private static void generateAndPrint(String pin) {
        String hash = BCrypt.hashpw(pin, BCrypt.gensalt(12));

        System.out.println("PIN:  " + pin);
        System.out.println("Hash: " + hash);
        System.out.println();

        // Verify hash works
        boolean verified = BCrypt.checkpw(pin, hash);
        if (verified) {
            System.out.println("✓ Hash verified successfully");
        } else {
            System.out.println("✗ Hash verification failed!");
        }

        System.out.println("─────────────────────────────────────────────");
        System.out.println();
    }

    /**
     * Validate PIN format (must be exactly 4 digits)
     */
    private static boolean isValidPin(String pin) {
        if (pin == null || pin.length() != 4) {
            return false;
        }

        // Check if all characters are digits
        for (char c : pin.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Verify a PIN against a hash (for testing)
     */
    public static boolean verifyPin(String pin, String hash) {
        return BCrypt.checkpw(pin, hash);
    }
}
