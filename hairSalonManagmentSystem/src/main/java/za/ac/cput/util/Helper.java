package za.ac.cput.util;

import za.ac.cput.domain.enums.DiscountType;
import za.ac.cput.domain.enums.LoyaltyTier;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Shared helper logic used across every factory and service so validation,
 * ID generation and money/loyalty math live in exactly one place instead of
 * being copy-pasted per entity.
 */
public class Helper {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // Accepts local SA numbers (0821234567) or international (+27821234567),
    // with optional spaces/dashes.
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^(\\+27|0)[ -]?[0-9]{2}[ -]?[0-9]{3}[ -]?[0-9]{4}$");

    // 1 loyalty point earned per R10 spent.
    private static final BigDecimal POINTS_PER_RAND_SPENT = new BigDecimal("10");

    // Loyalty tier thresholds, in total points ever earned.
    private static final int SILVER_THRESHOLD = 500;
    private static final int GOLD_THRESHOLD = 2000;
    private static final int PLATINUM_THRESHOLD = 5000;

    // Entity ID generation: 9 uppercase alphanumeric characters, e.g. "K3M8QPSXA".
    // keep accidental collisions negligible at any realistic salon data volume.
    private static final String ID_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int ID_LENGTH = 9;
    private static final SecureRandom ID_RANDOM = new SecureRandom();


    private Helper() {}

    /** True if the string is null, empty, or only whitespace. */
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /** True if amount is non-null and strictly greater than zero. */
    public static boolean isValidAmount(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public static boolean isPositiveDouble(double value) {
        return value > 0;
    }

    public static boolean isPositiveInteger(int value) {
        return value > 0;
    }

    /** True if the string looks like a real email address. */
    public static boolean isValidEmail(String email) {
        return !isNullOrEmpty(email) && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /** True if the string looks like a valid SA phone number (local or +27 format). */
    public static boolean isValidPhoneNumber(String phoneNumber) {
        return !isNullOrEmpty(phoneNumber) &&
                PHONE_PATTERN.matcher(phoneNumber.trim()).matches();
    }

    /** Generates a new random unique ID, used as the primary key for every entity. */
    public static String generateId() {
        StringBuilder id = new StringBuilder(ID_LENGTH);
        for (int i = 0; i < ID_LENGTH; i++) {
            id.append(ID_ALPHABET.charAt(ID_RANDOM.nextInt(ID_ALPHABET.length())));
        }
        return id.toString();
    }

    /** Generates a unique, human-scannable transaction reference for a Payment, e.g. TXN-1893XXXXXX-AB12. */
    public static String generateTransactionReference() {
        String randomSuffix = UUID.randomUUID().toString()
                .substring(0, 4)
                .toUpperCase();
        return "TXN-" + System.currentTimeMillis() + "-" + randomSuffix;
    }

    /**
     * Calculates the discount amount for a given base amount, never exceeding the amount itself.
     * PERCENTAGE treats discountValue as a percent (e.g. 20 = 20%); FIXED_AMOUNT treats it as a flat Rand value.
     */
    public static BigDecimal calculateDiscount(BigDecimal amount, DiscountType type, BigDecimal discountValue) {
        if (!isValidAmount(amount) || type == null || discountValue == null
                || discountValue.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = switch (type) {
            case PERCENTAGE -> amount.multiply(discountValue)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            case FIXED_AMOUNT -> discountValue;
        };

        return discount.min(amount);
    }

    /** Subtracts the discount from the amount, floored at zero. */
    public static BigDecimal calculateFinalAmount(BigDecimal amount, BigDecimal discount) {
        if (!isValidAmount(amount)) return BigDecimal.ZERO;
        BigDecimal safeDiscount = discount == null ? BigDecimal.ZERO : discount;
        BigDecimal finalAmount = amount.subtract(safeDiscount);
        return finalAmount.max(BigDecimal.ZERO);
    }

    /** 1 loyalty point earned per R10 spent, rounded down. */
    public static int calculateLoyaltyPointsEarned(BigDecimal amountSpent) {
        if (!isValidAmount(amountSpent)) return 0;
        return amountSpent.divide(POINTS_PER_RAND_SPENT, 0, RoundingMode.DOWN).intValue();
    }

    /** Maps a customer's total lifetime points to their loyalty tier. */
    public static LoyaltyTier determineLoyaltyTier(int totalPoints) {
        if (totalPoints >= PLATINUM_THRESHOLD) return LoyaltyTier.PLATINUM;
        if (totalPoints >= GOLD_THRESHOLD) return LoyaltyTier.GOLD;
        if (totalPoints >= SILVER_THRESHOLD) return LoyaltyTier.SILVER;
        return LoyaltyTier.BRONZE;
    }
}
