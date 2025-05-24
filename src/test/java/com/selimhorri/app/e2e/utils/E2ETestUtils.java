package com.selimhorri.app.e2e.utils;

import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static io.restassured.RestAssured.given;

/**
 * Utility class for E2E testing operations
 */
public class E2ETestUtils {

    private static final Logger logger = LoggerFactory.getLogger(E2ETestUtils.class);

    /**
     * Generate a unique email for testing
     */
    public static String generateUniqueEmail(String prefix) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return prefix + timestamp + "@test.com";
    }

    /**
     * Generate a unique username for testing
     */
    public static String generateUniqueUsername(String prefix) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return prefix + timestamp;
    }

    /**
     * Generate a unique product SKU
     */
    public static String generateUniqueSku(String prefix) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return prefix + "-" + timestamp;
    }

    /**
     * Generate a random price between min and max
     */
    public static double generateRandomPrice(double min, double max) {
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    /**
     * Generate a random quantity between min and max
     */
    public static int generateRandomQuantity(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    /**
     * Wait for a specified amount of time
     */
    public static void waitSeconds(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
            logger.debug("Waited {} seconds", seconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Wait interrupted", e);
        }
    }

    /**
     * Retry an operation with exponential backoff
     */
    public static <T> T retryOperation(RetryableOperation<T> operation, int maxAttempts, int initialDelaySeconds) {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                logger.debug("Attempting operation, try {}/{}", attempt, maxAttempts);
                return operation.execute();
            } catch (Exception e) {
                lastException = e;
                logger.warn("Operation failed on attempt {}/{}: {}", attempt, maxAttempts, e.getMessage());
                
                if (attempt < maxAttempts) {
                    int delaySeconds = initialDelaySeconds * attempt; // Linear backoff
                    logger.debug("Retrying in {} seconds...", delaySeconds);
                    waitSeconds(delaySeconds);
                }
            }
        }
        
        throw new RuntimeException("Operation failed after " + maxAttempts + " attempts", lastException);
    }

    /**
     * Functional interface for retryable operations
     */
    @FunctionalInterface
    public interface RetryableOperation<T> {
        T execute() throws Exception;
    }

    /**
     * Extract ID from response body
     */
    public static String extractIdFromResponse(Response response, String idField) {
        try {
            String id = response.jsonPath().getString(idField);
            if (id != null && !id.isEmpty()) {
                logger.debug("Extracted {} from response: {}", idField, id);
                return id;
            } else {
                throw new RuntimeException("Failed to extract " + idField + " from response");
            }
        } catch (Exception e) {
            logger.error("Error extracting {} from response: {}", idField, e.getMessage());
            logger.debug("Response body: {}", response.getBody().asString());
            throw e;
        }
    }

    /**
     * Validate response status and extract ID
     */
    public static String validateAndExtractId(Response response, int expectedStatus, String idField) {
        if (response.getStatusCode() != expectedStatus) {
            logger.error("Unexpected status code: {} (expected: {})", response.getStatusCode(), expectedStatus);
            logger.error("Response body: {}", response.getBody().asString());
            throw new RuntimeException("Unexpected status code: " + response.getStatusCode());
        }
        return extractIdFromResponse(response, idField);
    }

    /**
     * Create a test user with unique credentials
     */
    public static Map<String, Object> createTestUserRequest(String prefix) {
        String uniqueId = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return Map.of(
            "firstName", "Test",
            "lastName", "User",
            "email", generateUniqueEmail(prefix),
            "phone", "+1-555-" + String.format("%04d", ThreadLocalRandom.current().nextInt(1000, 9999)),
            "username", generateUniqueUsername(prefix),
            "password", "Password123!"
        );
    }

    /**
     * Create a test category request
     */
    public static Map<String, Object> createTestCategoryRequest(String title) {
        return Map.of(
            "categoryTitle", title,
            "imageUrl", "https://example.com/" + title.toLowerCase().replace(" ", "-") + ".jpg"
        );
    }

    /**
     * Create a test product request
     */
    public static Map<String, Object> createTestProductRequest(String title, String categoryId) {
        return Map.of(
            "productTitle", title,
            "imageUrl", "https://example.com/" + title.toLowerCase().replace(" ", "-") + ".jpg",
            "sku", generateUniqueSku("TEST"),
            "priceUnit", generateRandomPrice(10.0, 1000.0),
            "quantity", generateRandomQuantity(10, 100),
            "categoryId", categoryId
        );
    }

    /**
     * Create a test address request
     */
    public static Map<String, Object> createTestAddressRequest() {
        return Map.of(
            "fullAddress", "123 Test Street",
            "postalCode", "12345",
            "city", "Test City",
            "country", "Test Country"
        );
    }

    /**
     * Log test step with consistent formatting
     */
    public static void logTestStep(String step) {
        logger.info("=== {} ===", step);
    }

    /**
     * Log test result with details
     */
    public static void logTestResult(String operation, String result, String details) {
        logger.info("{} - Result: {} | Details: {}", operation, result, details);
    }

    /**
     * Generate a UUID for test data
     */
    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Safely extract string from response with default value
     */
    public static String safeExtractString(Response response, String path, String defaultValue) {
        try {
            String value = response.jsonPath().getString(path);
            return value != null ? value : defaultValue;
        } catch (Exception e) {
            logger.warn("Failed to extract {} from response, using default: {}", path, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Check if response contains expected fields
     */
    public static boolean validateResponseFields(Response response, List<String> requiredFields) {
        try {
            for (String field : requiredFields) {
                Object value = response.jsonPath().get(field);
                if (value == null) {
                    logger.warn("Required field '{}' is missing from response", field);
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            logger.error("Error validating response fields: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Clean up resource with error handling
     */
    public static void safeCleanup(String resourceType, String resourceId, String endpoint, String token) {
        try {
            given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete(endpoint + "/" + resourceId)
                .then()
                .statusCode(org.hamcrest.Matchers.anyOf(
                    org.hamcrest.Matchers.is(200),
                    org.hamcrest.Matchers.is(204),
                    org.hamcrest.Matchers.is(404)
                ));
            logger.debug("Successfully cleaned up {} with ID: {}", resourceType, resourceId);
        } catch (Exception e) {
            logger.warn("Failed to clean up {} with ID {}: {}", resourceType, resourceId, e.getMessage());
        }
    }

    /**
     * Format money amount for display
     */
    public static String formatMoney(double amount) {
        return String.format("$%.2f", amount);
    }

    /**
     * Generate test description with timestamp
     */
    public static String generateTestDescription(String prefix) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return prefix + " - Generated at " + timestamp;
    }
}
