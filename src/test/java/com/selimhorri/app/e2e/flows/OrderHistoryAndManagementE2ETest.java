package com.selimhorri.app.e2e.flows;

import com.selimhorri.app.e2e.config.BaseE2ETest;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * E2E Test #4: Order History and Management Flow
 * 
 * This test verifies the complete order management workflow:
 * 1. User creates an account and logs in
 * 2. User places multiple orders
 * 3. User views order history
 * 4. User tracks specific order status
 * 5. Admin updates order status
 * 6. User verifies updated order status
 * 7. User cancels an order (if allowed)
 * 
 * Microservices involved:
 * - user-service: User authentication and management
 * - product-service: Product catalog and inventory
 * - order-service: Order creation and management
 * - payment-service: Payment processing
 * - shipping-service: Shipping management
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OrderHistoryAndManagementE2ETest extends BaseE2ETest {

    private static final Logger logger = LoggerFactory.getLogger(OrderHistoryAndManagementE2ETest.class);
    
    private static String userToken;
    private static String userId;
    private static String categoryId;
    private static String productId;
    private static String firstOrderId;
    private static String secondOrderId;
    private static String cartId;

    @Test
    @Order(1)
    @DisplayName("Create user account for order management testing")
    public void createUserAccount() {
        logger.info("=== Creating user account for order management testing ===");
        
        Map<String, Object> userRequest = Map.of(
            "firstName", "Order",
            "lastName", "Manager",
            "email", "order.manager@test.com",
            "phone", "+1-555-0105",
            "username", "ordermanager",
            "password", "Password123!"
        );

        Response response = given()
            .spec(requestSpec)
            .body(userRequest)
            .when()
            .post("/user-service/api/users")
            .then()
            .statusCode(201)
            .body("email", equalTo("order.manager@test.com"))
            .body("username", equalTo("ordermanager"))
            .extract().response();

        userId = response.jsonPath().getString("userId");
        userToken = authenticateUser("ordermanager", "Password123!");
        
        logger.info("User account created successfully with ID: {}", userId);
    }

    @Test
    @Order(2)
    @DisplayName("Setup products for order testing")
    public void setupProducts() {
        logger.info("=== Setting up products for order testing ===");
        
        // Create category
        Map<String, Object> categoryRequest = Map.of(
            "categoryTitle", "Order Test Category",
            "imageUrl", "https://example.com/order-category.jpg"
        );

        Response categoryResponse = given()
            .spec(requestSpec)
            .header("Authorization", "Bearer " + userToken)
            .body(categoryRequest)
            .when()
            .post("/product-service/api/categories")
            .then()
            .statusCode(201)
            .extract().response();

        categoryId = categoryResponse.jsonPath().getString("categoryId");

        // Create product
        Map<String, Object> productRequest = Map.of(
            "productTitle", "Order Test Product",
            "imageUrl", "https://example.com/order-product.jpg",
            "sku", "ORDER-TEST-001",
            "priceUnit", 99.99,
            "quantity", 50,
            "categoryId", categoryId
        );

        Response productResponse = given()
            .spec(requestSpec)
            .header("Authorization", "Bearer " + userToken)
            .body(productRequest)
            .when()
            .post("/product-service/api/products")
            .then()
            .statusCode(201)
            .extract().response();

        productId = productResponse.jsonPath().getString("productId");
        
        logger.info("Product setup completed - Category ID: {}, Product ID: {}", categoryId, productId);
    }

    @Test
    @Order(3)
    @DisplayName("Place first order")
    public void placeFirstOrder() {
        logger.info("=== Placing first order ===");
        
        // Create cart
        Response cartResponse = given()
            .spec(requestSpec)
            .header("Authorization", "Bearer " + userToken)
            .when()
            .post("/order-service/api/carts")
            .then()
            .statusCode(201)
            .extract().response();

        cartId = cartResponse.jsonPath().getString("cartId");

        // Add item to cart
        Map<String, Object> cartItemRequest = Map.of(
            "productId", productId,
            "quantity", 2
        );

        given()
            .spec(requestSpec)
            .header("Authorization", "Bearer " + userToken)
            .body(cartItemRequest)
            .when()
            .post("/order-service/api/carts/" + cartId + "/items")
            .then()
            .statusCode(201);

        // Create order from cart
        Map<String, Object> orderRequest = Map.of(
            "cartId", cartId,
            "orderDesc", "First test order for order management"
        );

        Response orderResponse = given()
            .spec(requestSpec)
            .header("Authorization", "Bearer " + userToken)
            .body(orderRequest)
            .when()
            .post("/order-service/api/orders")
            .then()
            .statusCode(201)
            .body("orderDesc", equalTo("First test order for order management"))
            .body("orderStatus", equalTo("PENDING"))
            .extract().response();

        firstOrderId = orderResponse.jsonPath().getString("orderId");
        
        logger.info("First order placed successfully with ID: {}", firstOrderId);
    }

    @Test
    @Order(4)
    @DisplayName("Place second order")
    public void placeSecondOrder() {
        logger.info("=== Placing second order ===");
        
        // Create new cart
        Response cartResponse = given()
            .spec(requestSpec)
            .header("Authorization", "Bearer " + userToken)
            .when()
            .post("/order-service/api/carts")
            .then()
            .statusCode(201)
            .extract().response();

        String newCartId = cartResponse.jsonPath().getString("cartId");

        // Add item to cart
        Map<String, Object> cartItemRequest = Map.of(
            "productId", productId,
            "quantity", 1
        );

        given()
            .spec(requestSpec)
            .header("Authorization", "Bearer " + userToken)
            .body(cartItemRequest)
            .when()
            .post("/order-service/api/carts/" + newCartId + "/items")
            .then()
            .statusCode(201);

        // Create order from cart
        Map<String, Object> orderRequest = Map.of(
            "cartId", newCartId,
            "orderDesc", "Second test order for order management"
        );

        Response orderResponse = given()
            .spec(requestSpec)
            .header("Authorization", "Bearer " + userToken)
            .body(orderRequest)
            .when()
            .post("/order-service/api/orders")
            .then()
            .statusCode(201)
            .body("orderDesc", equalTo("Second test order for order management"))
            .body("orderStatus", equalTo("PENDING"))
            .extract().response();

        secondOrderId = orderResponse.jsonPath().getString("orderId");
        
        logger.info("Second order placed successfully with ID: {}", secondOrderId);
    }

    @Test
    @Order(5)
    @DisplayName("Retrieve user order history")
    public void retrieveOrderHistory() {
        logger.info("=== Retrieving user order history ===");
        
        given()
            .spec(requestSpec)
            .header("Authorization", "Bearer " + userToken)
            .when()
            .get("/order-service/api/orders/user/" + userId)
            .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(2))
            .body("orderId", hasItems(firstOrderId, secondOrderId))
            .body("orderDesc", hasItems(
                "First test order for order management",
                "Second test order for order management"
            ));
        
        logger.info("Order history retrieved successfully");
    }

    @Test
    @Order(6)
    @DisplayName("Track specific order details")
    public void trackSpecificOrder() {
        logger.info("=== Tracking specific order details ===");
        
        given()
            .spec(requestSpec)
            .header("Authorization", "Bearer " + userToken)
            .when()
            .get("/order-service/api/orders/" + firstOrderId)
            .then()
            .statusCode(200)
            .body("orderId", equalTo(firstOrderId))
            .body("orderDesc", equalTo("First test order for order management"))
            .body("orderStatus", equalTo("PENDING"))
            .body("orderItems", notNullValue())
            .body("orderItems.size()", greaterThan(0));
        
        logger.info("Order details tracked successfully for order: {}", firstOrderId);
    }

    @Test
    @Order(7)
    @DisplayName("Update order status")
    public void updateOrderStatus() {
        logger.info("=== Updating order status ===");
        
        Map<String, Object> statusUpdate = Map.of(
            "orderStatus", "CONFIRMED"
        );

        given()
            .spec(requestSpec)
            .header("Authorization", "Bearer " + userToken)
            .body(statusUpdate)
            .when()
            .put("/order-service/api/orders/" + firstOrderId)
            .then()
            .statusCode(200)
            .body("orderId", equalTo(firstOrderId))
            .body("orderStatus", equalTo("CONFIRMED"));
        
        logger.info("Order status updated successfully to CONFIRMED");
    }

    @Test
    @Order(8)
    @DisplayName("Verify order status update")
    public void verifyOrderStatusUpdate() {
        logger.info("=== Verifying order status update ===");
        
        given()
            .spec(requestSpec)
            .header("Authorization", "Bearer " + userToken)
            .when()
            .get("/order-service/api/orders/" + firstOrderId)
            .then()
            .statusCode(200)
            .body("orderId", equalTo(firstOrderId))
            .body("orderStatus", equalTo("CONFIRMED"));
        
        logger.info("Order status update verified successfully");
    }

    @Test
    @Order(9)
    @DisplayName("Get all orders (admin view)")
    public void getAllOrders() {
        logger.info("=== Getting all orders (admin view) ===");
        
        given()
            .spec(requestSpec)
            .header("Authorization", "Bearer " + userToken)
            .when()
            .get("/order-service/api/orders")
            .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(2))
            .body("orderId", hasItems(firstOrderId, secondOrderId));
        
        logger.info("All orders retrieved successfully");
    }

    @Test
    @Order(10)
    @DisplayName("Filter orders by status")
    public void filterOrdersByStatus() {
        logger.info("=== Filtering orders by status ===");
        
        given()
            .spec(requestSpec)
            .header("Authorization", "Bearer " + userToken)
            .queryParam("status", "CONFIRMED")
            .when()
            .get("/order-service/api/orders")
            .then()
            .statusCode(200)
            .body("orderId", hasItem(firstOrderId))
            .body("orderStatus", everyItem(equalTo("CONFIRMED")));
        
        logger.info("Orders filtered by status successfully");
    }

    @Test
    @Order(11)
    @DisplayName("Cleanup test data")
    public void cleanupTestData() {
        logger.info("=== Cleaning up test data ===");
        
        try {
            // Delete orders (if endpoint exists)
            given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + userToken)
                .when()
                .delete("/order-service/api/orders/" + firstOrderId)
                .then()
                .statusCode(anyOf(is(200), is(204), is(404)));

            given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + userToken)
                .when()
                .delete("/order-service/api/orders/" + secondOrderId)
                .then()
                .statusCode(anyOf(is(200), is(204), is(404)));

            // Delete product
            given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + userToken)
                .when()
                .delete("/product-service/api/products/" + productId)
                .then()
                .statusCode(anyOf(is(200), is(204), is(404)));

            // Delete category
            given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + userToken)
                .when()
                .delete("/product-service/api/categories/" + categoryId)
                .then()
                .statusCode(anyOf(is(200), is(204), is(404)));

            // Delete user
            given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + userToken)
                .when()
                .delete("/user-service/api/users/" + userId)
                .then()
                .statusCode(anyOf(is(200), is(204), is(404)));

        } catch (Exception e) {
            logger.warn("Some cleanup operations failed, which is acceptable: {}", e.getMessage());
        }
        
        logger.info("Test data cleanup completed");
    }
}
