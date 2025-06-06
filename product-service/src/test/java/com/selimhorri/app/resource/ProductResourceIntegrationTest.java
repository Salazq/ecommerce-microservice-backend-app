package com.selimhorri.app.resource;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.dto.CategoryDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.service.CategoryService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class ProductResourceIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private CategoryService categoryService;
    
    
    @Test
    @DisplayName("Test should get product by ID - REST API")
    public void testFindById() throws Exception {
        // Given
        int productId = 1;
        
        mockMvc.perform(get("/api/products/{productId}", productId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId", is(productId)));
    }
    
    @Test
    @DisplayName("Test should create new product - REST API")
    public void testSave() throws Exception {
        // Given
        CategoryDto categoryDto = categoryService.findById(1);
        
        ProductDto newProductDto = ProductDto.builder()
                .productTitle("REST API Test Product")
                .imageUrl("https://example.com/rest-test.jpg")
                .sku("REST-TEST-SKU-456")
                .priceUnit(149.99)
                .quantity(35)
                .categoryDto(categoryDto)
                .build();
        
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newProductDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").exists())
                .andExpect(jsonPath("$.productTitle", is("REST API Test Product")));
    }
    
    @Test
    @DisplayName("Test should update product - REST API")
    public void testUpdate() throws Exception {
        // Given
        // First, let's create a product to update
        CategoryDto categoryDto = categoryService.findById(1);
        
        ProductDto productToUpdate = ProductDto.builder()
                .productTitle("Initial REST Product")
                .imageUrl("https://example.com/initial-rest.jpg")
                .sku("INITIAL-REST-SKU-789")
                .priceUnit(199.99)
                .quantity(10)
                .categoryDto(categoryDto)
                .build();
        
        String responseJson = mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productToUpdate)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        
        ProductDto createdProduct = objectMapper.readValue(responseJson, ProductDto.class);
        
        // Now update the product
        createdProduct.setProductTitle("Updated REST Product");
        
        mockMvc.perform(put("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createdProduct)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId", is(createdProduct.getProductId())))
                .andExpect(jsonPath("$.productTitle", is("Updated REST Product")));
    }
    
}