package com.selimhorri.app.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.selimhorri.app.domain.Category;
import com.selimhorri.app.domain.Product;

@DataJpaTest
@ActiveProfiles("test")
@TestMethodOrder(OrderAnnotation.class)
@Transactional
public class ProductRepositoryIntegrationTest {
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    private Product testProduct;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        
        setupTestData();
    }

    private void setupTestData() {
        testCategory = Category.builder()
                .categoryTitle("Test Category")
                .imageUrl("http://example.com/category.jpg")
                .build();

        testProduct = Product.builder()
                .productTitle("Test Product")
                .imageUrl("https://example.com/test-image.jpg")
                .sku("TEST-SKU-12345")
                .priceUnit(99.99)
                .quantity(50)
                .category(testCategory)
                .build();
    }
    
    @Test
    @Order(1)
    @DisplayName("Repository Test - Should return empty list when no products exist")
    public void testFindAll_EmptyList() {
        // When
        List<Product> products = productRepository.findAll();
        
        // Then
        assertThat(products).isNotNull();
        assertThat(products).isEmpty();
    }    
    @Test
    @Order(2)
    @DisplayName("Repository Test - Should find all products when they exist")
    public void testFindAll_WithData() {
        // Given
        Category savedCategory = categoryRepository.save(testCategory);
        testProduct.setCategory(savedCategory);
        productRepository.save(testProduct);
        
        // When
        List<Product> products = productRepository.findAll();
        
        // Then
        assertThat(products).isNotNull();
        assertThat(products).hasSize(1);
        assertThat(products.get(0).getProductTitle()).isEqualTo("Test Product");
    }
    
    @Test
    @Order(3)
    @DisplayName("Repository Test - Should find product by ID when it exists")
    public void testFindById_Success() {
        // Given
        Category savedCategory = categoryRepository.save(testCategory);
        testProduct.setCategory(savedCategory);
        Product savedProduct = productRepository.save(testProduct);
        
        // When
        Optional<Product> foundProduct = productRepository.findById(savedProduct.getProductId());
        
        // Then
        assertTrue(foundProduct.isPresent());
        assertEquals(savedProduct.getProductId(), foundProduct.get().getProductId());
        assertEquals("Test Product", foundProduct.get().getProductTitle());
        assertEquals("TEST-SKU-12345", foundProduct.get().getSku());
    }

    @Test
    @Order(4)
    @DisplayName("Repository Test - Should return empty when product ID doesn't exist")
    public void testFindById_NotFound() {
        // When
        Optional<Product> foundProduct = productRepository.findById(999);
        
        // Then
        assertTrue(foundProduct.isEmpty());
    }
    
    @Test
    @Order(5)
    @DisplayName("Repository Test - Should save a new product successfully")
    public void testSaveProduct() {
        // Given
        Category savedCategory = categoryRepository.save(testCategory);
        testProduct.setCategory(savedCategory);
        
        // When
        Product savedProduct = productRepository.save(testProduct);
        
        // Then
        assertNotNull(savedProduct.getProductId());
        assertEquals("Test Product", savedProduct.getProductTitle());
        assertEquals("TEST-SKU-12345", savedProduct.getSku());
        assertEquals(99.99, savedProduct.getPriceUnit());
        assertEquals(50, savedProduct.getQuantity());
        assertNotNull(savedProduct.getCategory());
        assertEquals(savedCategory.getCategoryId(), savedProduct.getCategory().getCategoryId());
    }
    
    @Test
    @Order(6)
    @DisplayName("Repository Test - Should update an existing product")
    public void testUpdateProduct() {
        // Given
        Category savedCategory = categoryRepository.save(testCategory);
        testProduct.setCategory(savedCategory);
        Product savedProduct = productRepository.save(testProduct);
        
        String newTitle = "Updated Product Title";
        Integer newQuantity = 100;
        savedProduct.setProductTitle(newTitle);
        savedProduct.setQuantity(newQuantity);
        
        // When
        Product updatedProduct = productRepository.save(savedProduct);
        
        // Then
        assertEquals(newTitle, updatedProduct.getProductTitle());
        assertEquals(newQuantity, updatedProduct.getQuantity());
        assertEquals(savedProduct.getProductId(), updatedProduct.getProductId());
        assertEquals("TEST-SKU-12345", updatedProduct.getSku()); // Should remain unchanged
    }
    
    @Test
    @Order(7)
    @DisplayName("Repository Test - Should delete a product successfully")
    public void testDeleteProduct() {
        // Given
        Category savedCategory = categoryRepository.save(testCategory);
        testProduct.setCategory(savedCategory);
        Product savedProduct = productRepository.save(testProduct);
        Integer productId = savedProduct.getProductId();
        
        assertTrue(productRepository.findById(productId).isPresent());
        
        // When
        productRepository.deleteById(productId);
        
        // Then
        assertTrue(productRepository.findById(productId).isEmpty());
        assertThat(productRepository.findAll()).isEmpty();
    }

    @Test
    @Order(8)
    @DisplayName("Repository Test - Should check product existence")
    public void testExistsById() {
        // Given
        Category savedCategory = categoryRepository.save(testCategory);
        testProduct.setCategory(savedCategory);
        Product savedProduct = productRepository.save(testProduct);
        
        // When & Then
        assertTrue(productRepository.existsById(savedProduct.getProductId()));
        assertTrue(!productRepository.existsById(999)); // Non-existent ID
    }
}