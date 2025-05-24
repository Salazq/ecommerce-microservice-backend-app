package com.selimhorri.app.e2e.flows;

import com.selimhorri.app.e2e.config.BaseE2ETest;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Test E2E #2: Flujo completo de gestión de productos y categorías
 * 
 * Flujo probado:
 * 1. Usuario autenticado navega por las categorías disponibles
 * 2. Usuario consulta productos por categoría
 * 3. Usuario consulta detalles específicos de un producto
 * 4. Admin crea nuevas categorías y productos
 * 5. Usuario añade productos a favoritos
 */
@TestMethodOrder(OrderAnnotation.class)
@DisplayName("E2E Test 2: Product and Category Management Flow")
public class ProductAndCategoryManagementE2ETest extends BaseE2ETest {
    
    private String categoryId;
    private String productId;
    private String adminToken;
    private String adminUsername;
    
    @BeforeEach
    @Override
    void setUpBase() {
        super.setUpBase();
        createAndAuthenticateTestUser();
        createAdminUser();
    }
    
    private void createAdminUser() {
        String uniqueId = java.util.UUID.randomUUID().toString().substring(0, 8);
        adminUsername = "admin_" + uniqueId;
        
        // Crear usuario admin
        String adminPayload = String.format("""
            {
                "firstName": "Admin",
                "lastName": "User",
                "email": "%s@admin.com",
                "phone": "9876543210",
                "credential": {
                    "username": "%s",
                    "password": "%s",
                    "roleBasedAuthority": "ROLE_ADMIN",
                    "isEnabled": true,
                    "isAccountNonExpired": true,
                    "isAccountNonLocked": true,
                    "isCredentialsNonExpired": true
                }
            }
            """, adminUsername, adminUsername, testPassword);
        
        given()
                .contentType("application/json")
                .body(adminPayload)
        .when()
                .post("/api/users");
        
        // Autenticar admin
        String authPayload = String.format("""
            {
                "username": "%s",
                "password": "%s"
            }
            """, adminUsername, testPassword);
        
        Response authResponse = given()
                .contentType("application/json")
                .body(authPayload)
        .when()
                .post("/api/authenticate");
        
        this.adminToken = authResponse.jsonPath().getString("jwtToken");
    }
    
    @Test
    @Order(1)
    @DisplayName("1. Usuario navega por las categorías disponibles")
    void testBrowseCategories() {
        given()
        .when()
                .get("/api/categories")
        .then()
                .statusCode(200)
                .body("collection", notNullValue());
        
        System.out.println("✅ Categorías consultadas exitosamente");
    }
    
    @Test
    @Order(2)
    @DisplayName("2. Admin crea una nueva categoría")
    void testCreateCategory() {
        String categoryPayload = """
            {
                "categoryTitle": "Electrónicos",
                "imageUrl": "https://example.com/electronics.jpg"
            }
            """;
        
        Response response = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType("application/json")
                .body(categoryPayload)
        .when()
                .post("/api/categories")
        .then()
                .statusCode(200)
                .body("categoryTitle", equalTo("Electrónicos"))
                .body("imageUrl", equalTo("https://example.com/electronics.jpg"))
                .extract()
                .response();
        
        categoryId = response.jsonPath().getString("categoryId");
        System.out.println("✅ Categoría creada exitosamente con ID: " + categoryId);
    }
    
    @Test
    @Order(3)
    @DisplayName("3. Admin crea un nuevo producto")
    void testCreateProduct() {
        String productPayload = String.format("""
            {
                "productTitle": "Smartphone XYZ",
                "imageUrl": "https://example.com/smartphone.jpg",
                "sku": "PHONE-XYZ-001",
                "priceUnit": 599.99,
                "quantity": 50,
                "categoryId": %s
            }
            """, categoryId);
        
        Response response = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType("application/json")
                .body(productPayload)
        .when()
                .post("/api/products")
        .then()
                .statusCode(200)
                .body("productTitle", equalTo("Smartphone XYZ"))
                .body("sku", equalTo("PHONE-XYZ-001"))
                .body("priceUnit", equalTo(599.99f))
                .body("quantity", equalTo(50))
                .extract()
                .response();
        
        productId = response.jsonPath().getString("productId");
        System.out.println("✅ Producto creado exitosamente con ID: " + productId);
    }
    
    @Test
    @Order(4)
    @DisplayName("4. Usuario consulta todos los productos disponibles")
    void testBrowseProducts() {
        given()
        .when()
                .get("/api/products")
        .then()
                .statusCode(200)
                .body("collection", notNullValue())
                .body("collection.size()", greaterThan(0));
        
        System.out.println("✅ Productos consultados exitosamente");
    }
    
    @Test
    @Order(5)
    @DisplayName("5. Usuario consulta detalles específicos del producto")
    void testGetProductDetails() {
        given()
        .when()
                .get("/api/products/" + productId)
        .then()
                .statusCode(200)
                .body("productId", equalTo(Integer.parseInt(productId)))
                .body("productTitle", equalTo("Smartphone XYZ"))
                .body("sku", equalTo("PHONE-XYZ-001"))
                .body("priceUnit", equalTo(599.99f))
                .body("quantity", equalTo(50));
        
        System.out.println("✅ Detalles del producto consultados exitosamente");
    }
    
    @Test
    @Order(6)
    @DisplayName("6. Usuario consulta detalles de la categoría")
    void testGetCategoryDetails() {
        given()
        .when()
                .get("/api/categories/" + categoryId)
        .then()
                .statusCode(200)
                .body("categoryId", equalTo(Integer.parseInt(categoryId)))
                .body("categoryTitle", equalTo("Electrónicos"))
                .body("imageUrl", equalTo("https://example.com/electronics.jpg"));
        
        System.out.println("✅ Detalles de la categoría consultados exitosamente");
    }
    
    @Test
    @Order(7)
    @DisplayName("7. Usuario autenticado añade producto a favoritos")
    void testAddProductToFavourites() {
        String favouritePayload = String.format("""
            {
                "userId": %s,
                "productId": %s,
                "likeDate": "%s"
            }
            """, getUserIdFromToken(), productId, java.time.LocalDateTime.now().toString());
        
        Response response = given()
                .header("Authorization", getAuthHeader())
                .contentType("application/json")
                .body(favouritePayload)
        .when()
                .post("/api/favourites")
        .then()
                .statusCode(200)
                .body("productId", equalTo(Integer.parseInt(productId)))
                .extract()
                .response();
        
        System.out.println("✅ Producto añadido a favoritos exitosamente");
        
        // Verificar que se puede consultar en la lista de favoritos
        given()
                .header("Authorization", getAuthHeader())
        .when()
                .get("/api/favourites")
        .then()
                .statusCode(200)
                .body("collection", notNullValue());
        
        System.out.println("✅ Lista de favoritos consultada exitosamente");
    }
    
    @Test
    @Order(8)
    @DisplayName("8. Admin actualiza información del producto")
    void testUpdateProduct() {
        String updatePayload = String.format("""
            {
                "productId": %s,
                "productTitle": "Smartphone XYZ Pro",
                "imageUrl": "https://example.com/smartphone-pro.jpg",
                "sku": "PHONE-XYZ-PRO-001",
                "priceUnit": 699.99,
                "quantity": 45,
                "categoryId": %s
            }
            """, productId, categoryId);
        
        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType("application/json")
                .body(updatePayload)
        .when()
                .put("/api/products/" + productId)
        .then()
                .statusCode(200)
                .body("productTitle", equalTo("Smartphone XYZ Pro"))
                .body("sku", equalTo("PHONE-XYZ-PRO-001"))
                .body("priceUnit", equalTo(699.99f))
                .body("quantity", equalTo(45));
        
        System.out.println("✅ Producto actualizado exitosamente");
    }
    
    private String getUserIdFromToken() {
        // Implementar lógica para extraer userId del token o 
        // obtenerlo consultando el usuario por username
        Response userResponse = given()
                .header("Authorization", getAuthHeader())
        .when()
                .get("/api/users/username/" + testUsername);
        
        return userResponse.jsonPath().getString("userId");
    }
}
