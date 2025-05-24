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
 * E2E Test #5: Favorites Management Flow
 * 
 * This test verifies the complete favorites management workflow:
 * 1. User creates an account and logs in
 * 2. User browses products and categories
 * 3. User adds multiple products to favorites
 * 4. User views their favorites list
 * 5. User removes items from favorites
 * 6. User manages favorites across different categories
 * 7. User clears all favorites
 * 
 * Microservices involved:
 * - user-service: User authentication and management
 * - product-service: Product catalog and inventory
 * - favourite-service: Favorites management
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FavoritesManagementE2ETest extends BaseE2ETest {

    private static final Logger logger = LoggerFactory.getLogger(FavoritesManagementE2ETest.class);
    
    private static String userToken;
    private static String userId;
    private static String categoryId1;
    private static String categoryId2;
    private static String productId1;
    private static String productId2;
    private static String productId3;
    private static String favoriteId1;
    private static String favoriteId2;

    @Test
    @Order(1)
    @DisplayName("Create user account for favorites testing")
    public void createUserAccount() {
        logger.info("=== Creating user account for favorites testing ===");
        
        Map<String, Object> userRequest = Map.of(
            "firstName", "Favorite",
            "lastName", "Lover",
            "email", "favorite.lover@test.com",
            "phone", "+1-555-0106",
            "username", "favoritelover",
            "password", "Password123!"
        );

        Response response = given()
            .spec(requestSpec)
            .body(userRequest)
            .when()
            .post("/user-service/api/users")
            .then()
            .statusCode(201)
            .body("email", equalTo("favorite.lover@test.com"))
            .body("username", equalTo("favoritelover"))
            .extract().response();

        userId = response.jsonPath().getString("userId");
        userToken = authenticateUser("favoritelover", "Password123!");
        
        logger.info("User account created successfully with ID: {}", userId);
    }

    @Test
    @Order(2)
    @DisplayName("Setup categories and products for favorites testing")
    public void setupCategoriesAndProducts() {
        logger.info("=== Setting up categories and products for favorites testing ===");
        
        // Create first category
        Map<String, Object> category1Request = Map.of(
            "categoryTitle", "Electronics Favorites",
            "imageUrl", "https://example.com/electronics.jpg"
        );

        Response category1Response = given()
            .spec(requestSpec)
            .header("Authorization", "Bearer " + userToken)
            .body(category1Request)
            .when()
            .post("/product-service/api/categories")
            .then()
            .statusCode(201)
            .extract().response();

        categoryId1 = category1Response.jsonPath().getString("categoryId");

        // Create second category
        Map<String, Object> category2Request = Map.of(
            "categoryTitle", "Books Favorites",
            "imageUrl", "https://example.com/books.jpg"
        );

        Response category2Response = given()
            .spec(requestSpec)
            .header("Authorization", "Bearer " + userToken)
            .body(category2Request)
            .when()
            .post("/product-service/api/categories")
            .then()
            .statusCode(201)
            .extract().response();

        categoryId2 = category2Response.jsonPath().getString("categoryId");

        // Create products
        Map<String, Object> product1Request = Map.of(
            "productTitle", "Smartphone Pro",
            "imageUrl", "https://example.com/smartphone.jpg",
            "sku", "PHONE-001",
            "priceUnit", 899.99,
            "quantity", 25,
            "categoryId", categoryId1
        );

        Response product1Response = given()
            .spec(requestSpec)
            .header("Authorization", "Bearer " + userToken)
            .body(product1Request)
            .when()
            .post("/product-service/api/products")
            .then()
            .statusCode(201)
            .extract().response();

        productId1 = product1Response.jsonPath().getString("productId");

        Map<String, Object> product2Request = Map.of(
            "productTitle", "Laptop Gaming",
            "imageUrl", "https://example.com/laptop.jpg",
            "sku", "LAPTOP-001",
            "priceUnit", 1299.99,
            "quantity", 15,
            "categoryId", categoryId1
        );

        Response product2Response = given()
            .spec(requestSpec)
            .header("Authorization", "Bearer " + userToken)
            .body(product2Request)
            .when()
            .post("/product-service/api/products")
            .then()
            .statusCode(201)
            .extract().response();

        productId2 = product2Response.jsonPath().getString("productId");

        Map<String, Object> product3Request = Map.of(
            "productTitle", "Programming Book",
            "imageUrl", "https://example.com/book.jpg",
            "sku", "BOOK-001",
            "priceUnit", 45.99,
            "quantity", 100,
            "categoryId", categoryId2
        );

        Response product3Response = given()
            .spec(requestSpec)
            .header("Authorization", "Bearer " + userToken)
            .body(product3Request)
            .when()
            .post("/product-service/api/products")
            .then()
            .statusCode(201)
            .extract().response();

        productId3 = product3Response.jsonPath().getString("productId");
        
        logger.info("Products setup completed - Product IDs: {}, {}, {}", productId1, productId2, productId3);
    }

    @Test
    @Order(3)
    @DisplayName("Browse products before adding to favorites")
    public void browseProducts() {
        logger.info("=== Browsing products before adding to favorites ===");
        
        // Browse all products
        given()
            .spec(requestSpec)
            .header("Authorization", "Bearer " + userToken)
            .when()
            .get("/product-service/api/products")
            .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(3))
            .body("productId", hasItems(productId1, productId2, productId3));

        // Browse products by category
        given()
            .spec(requestSpec)
            .header("Authorization", "Bearer " + userToken)
            .when()
            .get("/product-service/api/products/category/" + categoryId1)
            .then()
            .statusCode(200)
            .body("size()", equalTo(2))
            .body("productId", hasItems(productId1, productId2));
        
        logger.info("Product browsing completed successfully");
    }

    @Test
    @Order(4)
    @DisplayName("Add first product to favorites")
    public void addFirstProductToFavorites() {
        logger.info("=== Adding first product to favorites ===");
        
        Map<String, Object> favoriteRequest = Map.of(
            "userId", userId,
            "productId", productId1
        );

        Response response = given()
            .spec(requestSpec)
            .header("Authorization", "Bearer " + userToken)
            .body(favoriteRequest)
            .when()
            .post("/favourite-service/api/favourites")
            .then()
            .statusCode(201)
            .body("userId", equalTo(userId))
            .body("productId", equalTo(productId1))
            .extract().response();

        favoriteId1 = response.jsonPath().getString("favouriteId");
        
        logger.info("First product added to favorites successfully with ID: {}", favoriteId1);
    }

    @Test
    @Order(5)
    @DisplayName("Add second product to favorites")
    public void addSecondProductToFavorites() {
        logger.info("=== Adding second product to favorites ===");
        
        Map<String, Object> favoriteRequest = Map.of(
            "userId", userId,
            "productId", productId3
        );

        Response response = given()
            .spec(requestSpec)
            .header("Authorization", "Bearer " + userToken)
            .body(favoriteRequest)
            .when()
            .post("/favourite-service/api/favourites")
            .then()
            .statusCode(201)
            .body("userId", equalTo(userId))
            .body("productId", equalTo(productId3))
            .extract().response();

        favoriteId2 = response.jsonPath().getString("favouriteId");
        
        logger.info("Second product added to favorites successfully with ID: {}", favoriteId2);
    }

    @Test
    @Order(6)
    @DisplayName("View user's favorites list")
    public void viewUserFavoritesList() {
        logger.info("=== Viewing user's favorites list ===");
        
        given()
            .spec(requestSpec)
            .header("Authorization", "Bearer " + userToken)
            .when()
            .get("/favourite-service/api/favourites/user/" + userId)
            .then()
            .statusCode(200)
            .body("size()", equalTo(2))
            .body("favouriteId", hasItems(favoriteId1, favoriteId2))
            .body("productId", hasItems(productId1, productId3))
            .body("userId", everyItem(equalTo(userId)));
        
        logger.info("User favorites list retrieved successfully");
    }

    @Test
    @Order(7)
    @DisplayName("Get all favorites (admin view)")
    public void getAllFavorites() {
        logger.info("=== Getting all favorites (admin view) ===");
        
        given()
            .spec(requestSpec)
            .header("Authorization", "Bearer " + userToken)
            .when()
            .get("/favourite-service/api/favourites")
            .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(2))
            .body("favouriteId", hasItems(favoriteId1, favoriteId2));
        
        logger.info("All favorites retrieved successfully");
    }

    @Test
    @Order(8)
    @DisplayName("Get specific favorite details")
    public void getSpecificFavoriteDetails() {
        logger.info("=== Getting specific favorite details ===");
        
        given()
            .spec(requestSpec)
            .header("Authorization", "Bearer " + userToken)
            .when()
            .get("/favourite-service/api/favourites/" + favoriteId1)
            .then()
            .statusCode(200)
            .body("favouriteId", equalTo(favoriteId1))
            .body("userId", equalTo(userId))
            .body("productId", equalTo(productId1));
        
        logger.info("Specific favorite details retrieved successfully");
    }

    @Test
    @Order(9)
    @DisplayName("Try to add duplicate favorite (should handle gracefully)")
    public void tryAddDuplicateFavorite() {
        logger.info("=== Trying to add duplicate favorite ===");
        
        Map<String, Object> favoriteRequest = Map.of(
            "userId", userId,
            "productId", productId1
        );

        given()
            .spec(requestSpec)
            .header("Authorization", "Bearer " + userToken)
            .body(favoriteRequest)
            .when()
            .post("/favourite-service/api/favourites")
            .then()
            .statusCode(anyOf(is(409), is(400), is(201))); // Handle different possible responses
        
        logger.info("Duplicate favorite handling tested");
    }

    @Test
    @Order(10)
    @DisplayName("Add third product to favorites")
    public void addThirdProductToFavorites() {
        logger.info("=== Adding third product to favorites ===");
        
        Map<String, Object> favoriteRequest = Map.of(
            "userId", userId,
            "productId", productId2
        );

        given()
            .spec(requestSpec)
            .header("Authorization", "Bearer " + userToken)
            .body(favoriteRequest)
            .when()
            .post("/favourite-service/api/favourites")
            .then()
            .statusCode(201)
            .body("userId", equalTo(userId))
            .body("productId", equalTo(productId2));
        
        logger.info("Third product added to favorites successfully");
    }

    @Test
    @Order(11)
    @DisplayName("Verify updated favorites list")
    public void verifyUpdatedFavoritesList() {
        logger.info("=== Verifying updated favorites list ===");
        
        given()
            .spec(requestSpec)
            .header("Authorization", "Bearer " + userToken)
            .when()
            .get("/favourite-service/api/favourites/user/" + userId)
            .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(2))
            .body("productId", hasItems(productId1, productId2, productId3))
            .body("userId", everyItem(equalTo(userId)));
        
        logger.info("Updated favorites list verified successfully");
    }

    @Test
    @Order(12)
    @DisplayName("Remove first favorite")
    public void removeFirstFavorite() {
        logger.info("=== Removing first favorite ===");
        
        given()
            .spec(requestSpec)
            .header("Authorization", "Bearer " + userToken)
            .when()
            .delete("/favourite-service/api/favourites/" + favoriteId1)
            .then()
            .statusCode(anyOf(is(200), is(204)));
        
        logger.info("First favorite removed successfully");
    }

    @Test
    @Order(13)
    @DisplayName("Verify favorite removal")
    public void verifyFavoriteRemoval() {
        logger.info("=== Verifying favorite removal ===");
        
        // Verify the specific favorite is gone
        given()
            .spec(requestSpec)
            .header("Authorization", "Bearer " + userToken)
            .when()
            .get("/favourite-service/api/favourites/" + favoriteId1)
            .then()
            .statusCode(404);

        // Verify it's not in user's list
        given()
            .spec(requestSpec)
            .header("Authorization", "Bearer " + userToken)
            .when()
            .get("/favourite-service/api/favourites/user/" + userId)
            .then()
            .statusCode(200)
            .body("favouriteId", not(hasItem(favoriteId1)))
            .body("productId", not(hasItem(productId1)));
        
        logger.info("Favorite removal verified successfully");
    }

    @Test
    @Order(14)
    @DisplayName("Filter favorites by product")
    public void filterFavoritesByProduct() {
        logger.info("=== Filtering favorites by product ===");
        
        given()
            .spec(requestSpec)
            .header("Authorization", "Bearer " + userToken)
            .queryParam("productId", productId3)
            .when()
            .get("/favourite-service/api/favourites")
            .then()
            .statusCode(200)
            .body("productId", everyItem(equalTo(productId3)));
        
        logger.info("Favorites filtered by product successfully");
    }

    @Test
    @Order(15)
    @DisplayName("Cleanup test data")
    public void cleanupTestData() {
        logger.info("=== Cleaning up test data ===");
        
        try {
            // Remove remaining favorites
            given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + userToken)
                .when()
                .delete("/favourite-service/api/favourites/" + favoriteId2)
                .then()
                .statusCode(anyOf(is(200), is(204), is(404)));

            // Delete products
            given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + userToken)
                .when()
                .delete("/product-service/api/products/" + productId1)
                .then()
                .statusCode(anyOf(is(200), is(204), is(404)));

            given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + userToken)
                .when()
                .delete("/product-service/api/products/" + productId2)
                .then()
                .statusCode(anyOf(is(200), is(204), is(404)));

            given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + userToken)
                .when()
                .delete("/product-service/api/products/" + productId3)
                .then()
                .statusCode(anyOf(is(200), is(204), is(404)));

            // Delete categories
            given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + userToken)
                .when()
                .delete("/product-service/api/categories/" + categoryId1)
                .then()
                .statusCode(anyOf(is(200), is(204), is(404)));

            given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + userToken)
                .when()
                .delete("/product-service/api/categories/" + categoryId2)
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
