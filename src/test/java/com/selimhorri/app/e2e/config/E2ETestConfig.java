package com.selimhorri.app.e2e.config;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Configuración base para tests E2E contra microservicios desplegados en Kubernetes
 */
@TestConfiguration
public class E2ETestConfig {
    
    // URL del API Gateway en Kubernetes (ajusta según tu configuración)
    private static final String API_GATEWAY_BASE_URL = "http://localhost:30080"; // NodePort del API Gateway
    // Si estás usando minikube, puedes usar: minikube ip + NodePort
    // private static final String API_GATEWAY_BASE_URL = "http://192.168.49.2:30080";
    
    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = API_GATEWAY_BASE_URL;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }
    
    @Bean
    public RequestSpecification requestSpec() {
        return new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .build();
    }
    
    public static String getApiGatewayUrl() {
        return API_GATEWAY_BASE_URL;
    }
}
