package com.selimhorri.app.e2e.config;

import com.selimhorri.app.e2e.utils.E2ETestUtils;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * Clase base para todos los tests E2E
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("e2e")
public abstract class BaseE2ETest {
    
    protected String authToken;
    protected String testUsername;
    protected String testPassword = "testPassword123";
    
    @BeforeEach
    void setUpBase() {
        E2ETestConfig.setUp();
        generateUniqueTestCredentials();
    }
    
    private void generateUniqueTestCredentials() {
        // Generar username único para evitar conflictos entre tests
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        testUsername = "testuser_" + uniqueId;
    }
    
    /**
     * Crea un usuario de prueba y obtiene el token de autenticación
     */
    protected void createAndAuthenticateTestUser() {
        // Crear usuario
        String userPayload = String.format("""
            {
                "firstName": "Test",
                "lastName": "User",
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
        
        Response createUserResponse = given()
                .contentType("application/json")
                .body(userPayload)
        .when()
                .post("/api/users")
        .then()
                .statusCode(200)
                .extract()
                .response();
        
        // Autenticar usuario
        String authPayload = String.format("""
            {
                "username": "%s",
                "password": "%s"
            }
            """, testUsername, testPassword);
        
        Response authResponse = given()
                .contentType("application/json")
                .body(authPayload)
        .when()
                .post("/api/authenticate")
        .then()
                .statusCode(200)
                .extract()
                .response();
        
        this.authToken = authResponse.jsonPath().getString("jwtToken");
    }
    
    /**
     * Obtiene el header de autorización para requests autenticados
     */
    protected String getAuthHeader() {
        return "Bearer " + authToken;
    }
    
    /**
     * Limpia los datos de prueba (override en clases hijas si es necesario)
     */
    protected void cleanupTestData() {
        // Implementar limpieza específica en cada test si es necesario
    }
}
