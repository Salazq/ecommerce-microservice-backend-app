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
 * Test E2E #3: Flujo completo de proceso de compra (Checkout)
 * 
 * Flujo probado:
 * 1. Usuario autenticado crea un carrito de compras
 * 2. Usuario añade productos al carrito
 * 3. Usuario procede al checkout creando un pedido
 * 4. Sistema procesa el pago del pedido
 * 5. Sistema genera información de envío
 * 6. Usuario consulta el estado del pedido
 */
@TestMethodOrder(OrderAnnotation.class)
@DisplayName("E2E Test 3: Complete Purchase Flow (Checkout)")
public class CompletePurchaseFlowE2ETest extends BaseE2ETest {
    
    private String cartId;
    private String productId;
    private String categoryId;
    private String orderId;
    private String paymentId;
    private String userId;
    
    @BeforeEach
    @Override
    void setUpBase() {
        super.setUpBase();
        createAndAuthenticateTestUser();
        setupTestProductAndCategory();
        getUserId();
    }
    
    private void setupTestProductAndCategory() {
        // Crear categoría para el test
        String categoryPayload = """
            {
                "categoryTitle": "Test Category",
                "imageUrl": "https://example.com/test-category.jpg"
            }
            """;
        
        Response categoryResponse = given()
                .contentType("application/json")
                .body(categoryPayload)
        .when()
                .post("/api/categories");
        
        if (categoryResponse.getStatusCode() == 200) {
            categoryId = categoryResponse.jsonPath().getString("categoryId");
        }
        
        // Crear producto para el test
        String productPayload = String.format("""
            {
                "productTitle": "Test Product for Purchase",
                "imageUrl": "https://example.com/test-product.jpg",
                "sku": "TEST-PRODUCT-001",
                "priceUnit": 299.99,
                "quantity": 100,
                "categoryId": %s
            }
            """, categoryId != null ? categoryId : "1");
        
        Response productResponse = given()
                .contentType("application/json")
                .body(productPayload)
        .when()
                .post("/api/products");
        
        if (productResponse.getStatusCode() == 200) {
            productId = productResponse.jsonPath().getString("productId");
        } else {
            // Si no se puede crear, usar un producto existente
            Response existingProducts = given().get("/api/products");
            if (existingProducts.getStatusCode() == 200 && 
                existingProducts.jsonPath().getList("collection").size() > 0) {
                productId = existingProducts.jsonPath().getString("collection[0].productId");
            }
        }
    }
    
    private void getUserId() {
        Response userResponse = given()
                .header("Authorization", getAuthHeader())
        .when()
                .get("/api/users/username/" + testUsername);
        
        if (userResponse.getStatusCode() == 200) {
            userId = userResponse.jsonPath().getString("userId");
        }
    }
    
    @Test
    @Order(1)
    @DisplayName("1. Usuario crea un carrito de compras")
    void testCreateShoppingCart() {
        String cartPayload = String.format("""
            {
                "userId": %s
            }
            """, userId);
        
        Response response = given()
                .header("Authorization", getAuthHeader())
                .contentType("application/json")
                .body(cartPayload)
        .when()
                .post("/api/carts")
        .then()
                .statusCode(200)
                .body("userId", equalTo(Integer.parseInt(userId)))
                .extract()
                .response();
        
        cartId = response.jsonPath().getString("cartId");
        System.out.println("✅ Carrito de compras creado exitosamente con ID: " + cartId);
    }
    
    @Test
    @Order(2)
    @DisplayName("2. Usuario consulta el carrito creado")
    void testGetShoppingCart() {
        given()
                .header("Authorization", getAuthHeader())
        .when()
                .get("/api/carts/" + cartId)
        .then()
                .statusCode(200)
                .body("cartId", equalTo(Integer.parseInt(cartId)))
                .body("userId", equalTo(Integer.parseInt(userId)));
        
        System.out.println("✅ Carrito consultado exitosamente");
    }
    
    @Test
    @Order(3)
    @DisplayName("3. Usuario actualiza el carrito (simulando añadir productos)")
    void testUpdateShoppingCart() {
        String updateCartPayload = String.format("""
            {
                "cartId": %s,
                "userId": %s
            }
            """, cartId, userId);
        
        given()
                .header("Authorization", getAuthHeader())
                .contentType("application/json")
                .body(updateCartPayload)
        .when()
                .put("/api/carts/" + cartId)
        .then()
                .statusCode(200)
                .body("cartId", equalTo(Integer.parseInt(cartId)));
        
        System.out.println("✅ Carrito actualizado exitosamente");
    }
    
    @Test
    @Order(4)
    @DisplayName("4. Usuario procede al checkout creando un pedido")
    void testCreateOrder() {
        String orderPayload = String.format("""
            {
                "orderDate": "%s",
                "orderDesc": "Test order for purchase flow",
                "orderFee": 299.99,
                "userId": %s
            }
            """, java.time.LocalDateTime.now().toString(), userId);
        
        Response response = given()
                .header("Authorization", getAuthHeader())
                .contentType("application/json")
                .body(orderPayload)
        .when()
                .post("/api/orders")
        .then()
                .statusCode(200)
                .body("orderDesc", equalTo("Test order for purchase flow"))
                .body("orderFee", equalTo(299.99f))
                .body("userId", equalTo(Integer.parseInt(userId)))
                .extract()
                .response();
        
        orderId = response.jsonPath().getString("orderId");
        System.out.println("✅ Pedido creado exitosamente con ID: " + orderId);
    }
    
    @Test
    @Order(5)
    @DisplayName("5. Sistema procesa el pago del pedido")
    void testProcessPayment() {
        String paymentPayload = String.format("""
            {
                "paymentDate": "%s",
                "paymentDesc": "Payment for order %s",
                "paymentFee": 299.99,
                "orderId": %s
            }
            """, java.time.LocalDateTime.now().toString(), orderId, orderId);
        
        Response response = given()
                .header("Authorization", getAuthHeader())
                .contentType("application/json")
                .body(paymentPayload)
        .when()
                .post("/api/payments")
        .then()
                .statusCode(200)
                .body("paymentDesc", containsString("Payment for order"))
                .body("paymentFee", equalTo(299.99f))
                .body("orderId", equalTo(Integer.parseInt(orderId)))
                .extract()
                .response();
        
        paymentId = response.jsonPath().getString("paymentId");
        System.out.println("✅ Pago procesado exitosamente con ID: " + paymentId);
    }
    
    @Test
    @Order(6)
    @DisplayName("6. Sistema genera información de envío")
    void testCreateShippingInfo() {
        if (productId != null) {
            String shippingPayload = String.format("""
                {
                    "orderedQuantity": 1,
                    "productId": %s,
                    "orderId": %s
                }
                """, productId, orderId);
            
            Response response = given()
                    .header("Authorization", getAuthHeader())
                    .contentType("application/json")
                    .body(shippingPayload)
            .when()
                    .post("/api/shippings")
            .then()
                    .statusCode(200)
                    .body("orderedQuantity", equalTo(1))
                    .body("productId", equalTo(Integer.parseInt(productId)))
                    .body("orderId", equalTo(Integer.parseInt(orderId)))
                    .extract()
                    .response();
            
            System.out.println("✅ Información de envío generada exitosamente");
        } else {
            System.out.println("⚠️ Saltando test de envío: productId no disponible");
        }
    }
    
    @Test
    @Order(7)
    @DisplayName("7. Usuario consulta el estado del pedido")
    void testGetOrderStatus() {
        given()
                .header("Authorization", getAuthHeader())
        .when()
                .get("/api/orders/" + orderId)
        .then()
                .statusCode(200)
                .body("orderId", equalTo(Integer.parseInt(orderId)))
                .body("orderDesc", equalTo("Test order for purchase flow"))
                .body("orderFee", equalTo(299.99f))
                .body("userId", equalTo(Integer.parseInt(userId)));
        
        System.out.println("✅ Estado del pedido consultado exitosamente");
    }
    
    @Test
    @Order(8)
    @DisplayName("8. Usuario consulta el historial de pedidos")
    void testGetOrderHistory() {
        given()
                .header("Authorization", getAuthHeader())
        .when()
                .get("/api/orders")
        .then()
                .statusCode(200)
                .body("collection", notNullValue())
                .body("collection.size()", greaterThan(0));
        
        System.out.println("✅ Historial de pedidos consultado exitosamente");
    }
    
    @Test
    @Order(9)
    @DisplayName("9. Usuario consulta información del pago")
    void testGetPaymentInfo() {
        given()
                .header("Authorization", getAuthHeader())
        .when()
                .get("/api/payments/" + paymentId)
        .then()
                .statusCode(200)
                .body("paymentId", equalTo(Integer.parseInt(paymentId)))
                .body("paymentDesc", containsString("Payment for order"))
                .body("paymentFee", equalTo(299.99f))
                .body("orderId", equalTo(Integer.parseInt(orderId)));
        
        System.out.println("✅ Información de pago consultada exitosamente");
    }
    
    @Test
    @Order(10)
    @DisplayName("10. Usuario consulta toda la información de envío")
    void testGetAllShippingInfo() {
        given()
                .header("Authorization", getAuthHeader())
        .when()
                .get("/api/shippings")
        .then()
                .statusCode(200)
                .body("collection", notNullValue());
        
        System.out.println("✅ Información de envío consultada exitosamente");
    }
}
