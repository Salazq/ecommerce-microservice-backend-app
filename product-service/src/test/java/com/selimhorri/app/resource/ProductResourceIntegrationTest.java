package com.selimhorri.app.resource;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.domain.Category;
import com.selimhorri.app.domain.Product;
import com.selimhorri.app.dto.CategoryDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.repository.CategoryRepository;
import com.selimhorri.app.repository.ProductRepository;
import com.selimhorri.app.service.CategoryService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@TestMethodOrder(OrderAnnotation.class)
@Transactional
public class ProductResourceIntegrationTest {
      @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryService categoryService;

    private MockMvc mockMvc;

    private ProductDto testProductDto;
    private Product testProduct;
    private CategoryDto testCategoryDto;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        
        setupTestData();
    }

    private void setupTestData() {
        // Setup test category first
        testCategory = Category.builder()
                .categoryTitle("Test Category")
                .imageUrl("http://example.com/category.jpg")
                .build();

        testCategoryDto = CategoryDto.builder()
                .categoryTitle("Test Category")
                .imageUrl("http://example.com/category.jpg")
                .build();

        // Setup test product
        testProduct = Product.builder()
                .productTitle("Test Product")
                .imageUrl("http://example.com/product.jpg")
                .sku("TEST-SKU-12345")
                .priceUnit(99.99)
                .quantity(50)
                .category(testCategory)
                .build();

        testProductDto = ProductDto.builder()
                .productTitle("Test Product")
                .imageUrl("http://example.com/product.jpg")
                .sku("TEST-SKU-12345")
                .priceUnit(99.99)
                .quantity(50)
                .categoryDto(testCategoryDto)
                .build();
    }    @Test
    @Order(1)
    @DisplayName("Integration Test - GET /api/products - Should return empty list when no products")
    void testFindAllProducts_EmptyList() throws Exception {
        mockMvc.perform(get("/api/products")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(0)));
    }

    @Test
    @Order(2)
    @DisplayName("Integration Test - POST /api/products - Should create new product successfully")
    void testCreateProduct_Success() throws Exception {
        // First save the category
        Category savedCategory = categoryRepository.save(testCategory);
        testCategoryDto.setCategoryId(savedCategory.getCategoryId());
        testProductDto.setCategoryDto(testCategoryDto);

        String productJson = objectMapper.writeValueAsString(testProductDto);

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(productJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productTitle", is("Test Product")))
                .andExpect(jsonPath("$.sku", is("TEST-SKU-12345")))
                .andExpect(jsonPath("$.priceUnit", is(99.99)))
                .andExpect(jsonPath("$.quantity", is(50)))
                .andExpect(jsonPath("$.productId", notNullValue()));
    }    @Test
    @Order(3)
    @DisplayName("Integration Test - GET /api/products/{productId} - Should return product by ID")
    void testFindProductById_Success() throws Exception {
        // First save category and product
        Category savedCategory = categoryRepository.save(testCategory);
        testProduct.setCategory(savedCategory);
        Product savedProduct = productRepository.save(testProduct);

        mockMvc.perform(get("/api/products/{productId}", savedProduct.getProductId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId", is(savedProduct.getProductId())))
                .andExpect(jsonPath("$.productTitle", is("Test Product")))
                .andExpect(jsonPath("$.sku", is("TEST-SKU-12345")))
                .andExpect(jsonPath("$.priceUnit", is(99.99)))
                .andExpect(jsonPath("$.category.categoryId", is(savedCategory.getCategoryId())));
    }

    @Test
    @Order(4)
    @DisplayName("Integration Test - GET /api/products/{productId} - Should result in ProductNotFoundException for non-existent product")
    void testFindProductById_NotFound() throws Exception {
        mockMvc.perform(get("/api/products/{productId}", 999)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg", is("#### Product with id: 999 not found! ####")))
                .andExpect(jsonPath("$.httpStatus", is("BAD_REQUEST")));
    }

    @Test
    @Order(5)
    @DisplayName("Integration Test - PUT /api/products/{productId} - Should update product with path variable")
    void testUpdateProductWithId_Success() throws Exception {
        // First save category and product
        Category savedCategory = categoryRepository.save(testCategory);
        testProduct.setCategory(savedCategory);
        Product savedProduct = productRepository.save(testProduct);

        testProductDto.setProductId(savedProduct.getProductId());
        testProductDto.setProductTitle("Updated Product Title");
        testProductDto.setQuantity(100);
        testCategoryDto.setCategoryId(savedCategory.getCategoryId());
        testProductDto.setCategoryDto(testCategoryDto);

        String productJson = objectMapper.writeValueAsString(testProductDto);        mockMvc.perform(put("/api/products/{productId}", savedProduct.getProductId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(productJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productTitle", is("Updated Product Title")))
                .andExpect(jsonPath("$.quantity", is(100)))
                .andExpect(jsonPath("$.productId", is(savedProduct.getProductId())));
    }

    @Test
    @Order(6)
    @DisplayName("Integration Test - PUT /api/products - Should update product without path variable")
    void testUpdateProduct_Success() throws Exception {
        // First save category and product
        Category savedCategory = categoryRepository.save(testCategory);
        testProduct.setCategory(savedCategory);
        Product savedProduct = productRepository.save(testProduct);

        testProductDto.setProductId(savedProduct.getProductId());
        testProductDto.setProductTitle("Updated via PUT");
        testProductDto.setPriceUnit(199.99);
        testCategoryDto.setCategoryId(savedCategory.getCategoryId());
        testProductDto.setCategoryDto(testCategoryDto);

        String productJson = objectMapper.writeValueAsString(testProductDto);

        mockMvc.perform(put("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(productJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productTitle", is("Updated via PUT")))
                .andExpect(jsonPath("$.priceUnit", is(199.99)))
                .andExpect(jsonPath("$.productId", is(savedProduct.getProductId())));
    }

    @Test
    @Order(7)
    @DisplayName("Integration Test - DELETE /api/products/{productId} - Should delete product successfully")
    void testDeleteProduct_Success() throws Exception {
        // First save category and product
        Category savedCategory = categoryRepository.save(testCategory);
        testProduct.setCategory(savedCategory);
        Product savedProduct = productRepository.save(testProduct);
        Integer productId = savedProduct.getProductId();        mockMvc.perform(delete("/api/products/{productId}", productId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(true)));

        // Verify product was deleted
        assert !productRepository.existsById(productId);
    }

}