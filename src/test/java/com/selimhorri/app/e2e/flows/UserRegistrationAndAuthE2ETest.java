package com.selimhorri.app.e2e.flows;

import com.selimhorri.app.e2e.config.BaseE2ETest;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Test E2E #1: Flujo completo de registro e inicio de sesión de usuario
 * 
 * Flujo probado:
 * 1. Usuario se registra con credenciales válidas
 * 2. Usuario inicia sesión con las credenciales creadas
 * 3. Usuario actualiza su información de perfil
 * 4. Usuario agrega/actualiza dirección
 */
@TestMethodOrder(OrderAnnotation.class)
@DisplayName("E2E Test 1: User Registration and Authentication Flow")
public class UserRegistrationAndAuthE2ETest extends BaseE2ETest {
    
    private String userId;
    private String credentialId;
    
    @Test
    @Order(1)
    @DisplayName("1. Usuario se registra exitosamente en el sistema")
    void testUserRegistration() {
        String userPayload = String.format("""
            {
                "firstName": "Juan",
                "lastName": "Pérez",
                "email": "%s@test.com",
                "phone": "1234567890",
                "credential": {
                    "username": "%s",
                    "password": "%s",
                    "roleBasedAuthority": "ROLE_USER",
                    "isEnabled": true,
                    "isAccountNonExpired": true,
                    "isAccountNonLocked": true,
                    "isCredentialsNonExpired": true
                }
            }
            """, testUsername, testUsername, testPassword);
        
        Response response = given()
                .contentType("application/json")
                .body(userPayload)
        .when()
                .post("/api/users")
        .then()
                .statusCode(200)
                .body("firstName", equalTo("Juan"))
                .body("lastName", equalTo("Pérez"))
                .body("email", equalTo(testUsername + "@test.com"))
                .body("phone", equalTo("1234567890"))
                .body("credential.username", equalTo(testUsername))
                .body("credential.roleBasedAuthority", equalTo("ROLE_USER"))
                .body("credential.isEnabled", equalTo(true))
                .extract()
                .response();
        
        userId = response.jsonPath().getString("userId");
        credentialId = response.jsonPath().getString("credential.credentialId");
        
        System.out.println("✅ Usuario registrado exitosamente con ID: " + userId);
    }
    
    @Test
    @Order(2)
    @DisplayName("2. Usuario inicia sesión con credenciales válidas")
    void testUserAuthentication() {
        String authPayload = String.format("""
            {
                "username": "%s",
                "password": "%s"
            }
            """, testUsername, testPassword);
        
        Response response = given()
                .contentType("application/json")
                .body(authPayload)
        .when()
                .post("/api/authenticate")
        .then()
                .statusCode(200)
                .body("jwtToken", notNullValue())
                .body("jwtToken", not(emptyString()))
                .extract()
                .response();
        
        authToken = response.jsonPath().getString("jwtToken");
        System.out.println("✅ Usuario autenticado exitosamente. Token obtenido.");
    }
    
    @Test
    @Order(3)
    @DisplayName("3. Usuario consulta su información de perfil")
    void testGetUserProfile() {
        given()
                .header("Authorization", getAuthHeader())
        .when()
                .get("/api/users/" + userId)
        .then()
                .statusCode(200)
                .body("userId", equalTo(Integer.parseInt(userId)))
                .body("firstName", equalTo("Juan"))
                .body("lastName", equalTo("Pérez"))
                .body("email", equalTo(testUsername + "@test.com"))
                .body("credential.username", equalTo(testUsername));
        
        System.out.println("✅ Información de perfil consultada exitosamente");
    }
    
    @Test
    @Order(4)
    @DisplayName("4. Usuario actualiza su información de perfil")
    void testUpdateUserProfile() {
        String updatePayload = String.format("""
            {
                "userId": %s,
                "firstName": "Juan Carlos",
                "lastName": "Pérez González",
                "email": "%s@test.com",
                "phone": "0987654321",
                "credential": {
                    "credentialId": %s,
                    "username": "%s",
                    "password": "%s",
                    "roleBasedAuthority": "ROLE_USER",
                    "isEnabled": true,
                    "isAccountNonExpired": true,
                    "isAccountNonLocked": true,
                    "isCredentialsNonExpired": true
                }
            }
            """, userId, testUsername, credentialId, testUsername, testPassword);
        
        given()
                .header("Authorization", getAuthHeader())
                .contentType("application/json")
                .body(updatePayload)
        .when()
                .put("/api/users/" + userId)
        .then()
                .statusCode(200)
                .body("firstName", equalTo("Juan Carlos"))
                .body("lastName", equalTo("Pérez González"))
                .body("phone", equalTo("0987654321"));
        
        System.out.println("✅ Perfil de usuario actualizado exitosamente");
    }
    
    @Test
    @Order(5)
    @DisplayName("5. Usuario agrega una dirección")
    void testAddUserAddress() {
        String addressPayload = """
            {
                "fullAddress": "Calle 123 #45-67",
                "postalCode": "110111",
                "city": "Bogotá"
            }
            """;
        
        Response response = given()
                .header("Authorization", getAuthHeader())
                .contentType("application/json")
                .body(addressPayload)
        .when()
                .post("/api/address")
        .then()
                .statusCode(200)
                .body("fullAddress", equalTo("Calle 123 #45-67"))
                .body("postalCode", equalTo("110111"))
                .body("city", equalTo("Bogotá"))
                .extract()
                .response();
        
        String addressId = response.jsonPath().getString("addressId");
        System.out.println("✅ Dirección agregada exitosamente con ID: " + addressId);
        
        // Verificar que la dirección se puede consultar
        given()
                .header("Authorization", getAuthHeader())
        .when()
                .get("/api/address/" + addressId)
        .then()
                .statusCode(200)
                .body("fullAddress", equalTo("Calle 123 #45-67"))
                .body("postalCode", equalTo("110111"))
                .body("city", equalTo("Bogotá"));
        
        System.out.println("✅ Dirección consultada exitosamente");
    }
    
    @Test
    @Order(6)
    @DisplayName("6. Validación de token JWT")
    void testJwtTokenValidation() {
        given()
                .pathParam("jwt", authToken)
        .when()
                .get("/api/authenticate/jwt/{jwt}")
        .then()
                .statusCode(200)
                .body(equalTo("true"));
        
        System.out.println("✅ Token JWT validado exitosamente");
    }
}
