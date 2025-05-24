package com.selimhorri.app.e2e.suite;

import com.selimhorri.app.e2e.flows.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * E2E Test Suite for Ecommerce Microservices
 * 
 * This test suite executes all end-to-end test flows in the correct order
 * to verify complete functionality across all microservices.
 * 
 * Test Execution Order:
 * 1. User Registration and Authentication Flow
 * 2. Product and Category Management Flow
 * 3. Complete Purchase Flow (Checkout)
 * 4. Order History and Management Flow
 * 5. Favorites Management Flow
 * 
 * Prerequisites:
 * - Kubernetes cluster is running
 * - All microservices are deployed and healthy
 * - API Gateway is accessible on port 8080
 * - Spring profile 'e2e' is active
 * 
 * Usage:
 * mvn test -Dtest="E2ETestSuite" -Dspring.profiles.active=e2e
 */
@Suite
@SuiteDisplayName("Ecommerce Microservices E2E Test Suite")
@SelectClasses({
    UserRegistrationAndAuthE2ETest.class,
    ProductAndCategoryManagementE2ETest.class,
    CompletePurchaseFlowE2ETest.class,
    OrderHistoryAndManagementE2ETest.class,
    FavoritesManagementE2ETest.class
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Complete E2E Testing Suite for Ecommerce Microservices")
public class E2ETestSuite {
    
    // This class serves as a test suite runner
    // Individual test methods are defined in the selected test classes
    
    /*
     * Test Coverage Summary:
     * 
     * 1. UserRegistrationAndAuthE2ETest:
     *    - User account creation and validation
     *    - Authentication and JWT token management
     *    - User profile updates and address management
     *    - Password changes and security validation
     * 
     * 2. ProductAndCategoryManagementE2ETest:
     *    - Category creation and management
     *    - Product creation with inventory
     *    - Product browsing and search
     *    - Integration with favorites system
     * 
     * 3. CompletePurchaseFlowE2ETest:
     *    - Shopping cart management
     *    - Order creation and processing
     *    - Payment processing workflow
     *    - Shipping integration
     *    - Complete checkout verification
     * 
     * 4. OrderHistoryAndManagementE2ETest:
     *    - Order history retrieval
     *    - Order status tracking and updates
     *    - Order management operations
     *    - Multi-order scenarios
     * 
     * 5. FavoritesManagementE2ETest:
     *    - Adding products to favorites
     *    - Favorites list management
     *    - Removing favorites
     *    - Cross-category favorites handling
     * 
     * Microservices Tested:
     * - user-service: User management and authentication
     * - product-service: Product and category management
     * - order-service: Order and cart management
     * - payment-service: Payment processing
     * - shipping-service: Shipping management
     * - favourite-service: Favorites management
     * - api-gateway: Request routing and load balancing
     * 
     * Test Data Management:
     * - Each test creates isolated test data
     * - Automatic cleanup after test completion
     * - Unique identifiers to prevent conflicts
     * - Graceful handling of cleanup failures
     */
}
