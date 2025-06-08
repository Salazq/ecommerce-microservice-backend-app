package com.selimhorri.app.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.selimhorri.app.domain.Category;
import com.selimhorri.app.domain.Product;

@DataJpaTest
@ActiveProfiles("dev")
public class ProductRepositoryIntegrationTest {
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Test
    @DisplayName("Test should find all products from H2 database")
    public void testFindAll() {
        // When
        List<Product> products = productRepository.findAll();
        
        // Then
        assertThat(products).isNotNull();
        // By default, Flyway should have loaded some sample data
        assertThat(products).isNotEmpty();
    }
    
    @Test
    @DisplayName("Test should find product by ID")
    public void testFindById() {
        // Given - Assuming ID 1 exists in the DB from Flyway migrations
        Integer productId = 1;
        
        // When
        Optional<Product> foundProduct = productRepository.findById(productId);
        
        // Then
        assertTrue(foundProduct.isPresent());
        assertEquals(productId, foundProduct.get().getProductId());
    }
    
    @Test
    @DisplayName("Test should save a new product")
    public void testSaveProduct() {
        // Given
        // First, get a category
        Category category = categoryRepository.findById(1).orElseThrow();
        
        Product newProduct = Product.builder()
                .productTitle("Test Product")
                .imageUrl("https://example.com/test-image.jpg")
                .sku("TEST-SKU-12345")
                .priceUnit(99.99)
                .quantity(50)
                .category(category)
                .build();
        
        // When
        Product savedProduct = productRepository.save(newProduct);
        
        // Then
        assertNotNull(savedProduct.getProductId());
        assertEquals("Test Product", savedProduct.getProductTitle());
        assertEquals("TEST-SKU-12345", savedProduct.getSku());
    }
    
    @Test
    @DisplayName("Test should update an existing product")
    public void testUpdateProduct() {
        // Given
        Product existingProduct = productRepository.findById(1).orElseThrow();
        String newTitle = "Updated Product Title";
        existingProduct.setProductTitle(newTitle);
        
        // When
        Product updatedProduct = productRepository.save(existingProduct);
        
        // Then
        assertEquals(newTitle, updatedProduct.getProductTitle());
        assertEquals(1, updatedProduct.getProductId());
    }
    
    @Test
    @DisplayName("Test should delete a product")
    public void testDeleteProduct() {
        // Given
        Integer productId = 1;
        assertTrue(productRepository.findById(productId).isPresent());
        
        // When
        productRepository.deleteById(productId);
        
        // Then
        assertTrue(productRepository.findById(productId).isEmpty());
    }
}