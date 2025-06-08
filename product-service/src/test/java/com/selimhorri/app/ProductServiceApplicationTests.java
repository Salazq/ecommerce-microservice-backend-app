package com.selimhorri.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.selimhorri.app.domain.Category;
import com.selimhorri.app.domain.Product;
import com.selimhorri.app.dto.CategoryDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.exception.wrapper.CategoryNotFoundException;
import com.selimhorri.app.exception.wrapper.ProductNotFoundException;
import com.selimhorri.app.repository.CategoryRepository;
import com.selimhorri.app.repository.ProductRepository;
import com.selimhorri.app.service.impl.CategoryServiceImpl;
import com.selimhorri.app.service.impl.ProductServiceImpl;

@ExtendWith(MockitoExtension.class)
class ProductServiceApplicationTests {
	
	@Mock
	private ProductRepository productRepository;
	
	@Mock
	private CategoryRepository categoryRepository;
	
	@InjectMocks
	private ProductServiceImpl productService;
	
	@InjectMocks
	private CategoryServiceImpl categoryService;

	@Test
	@DisplayName("Test 1: Find all products - Success scenario")
	void testFindAllProducts() {
		List<Product> products = new ArrayList<>();
		
		Category category1 = Category.builder()
				.categoryId(1)
				.categoryTitle("Electronics")
				.imageUrl("http://example.com/electronics.jpg")
				.build();
				
		Product product1 = Product.builder()
				.productId(1)
				.productTitle("Laptop")
				.imageUrl("http://example.com/laptop.jpg")
				.sku("LAPTOP-001")
				.priceUnit(999.99)
				.quantity(10)
				.category(category1)
				.build();
		
		Category category2 = Category.builder()
				.categoryId(2)
				.categoryTitle("Books")
				.imageUrl("http://example.com/books.jpg")
				.build();
				
		Product product2 = Product.builder()
				.productId(2)
				.productTitle("Java Programming")
				.imageUrl("http://example.com/java-book.jpg")
				.sku("BOOK-001")
				.priceUnit(49.99)
				.quantity(25)
				.category(category2)
				.build();
		
		products.add(product1);
		products.add(product2);
		
		when(productRepository.findAll()).thenReturn(products);
		
		List<ProductDto> result = productService.findAll();
		
		assertEquals(2, result.size());
		assertEquals("Laptop", result.get(0).getProductTitle());
		assertEquals("Java Programming", result.get(1).getProductTitle());
		assertEquals("LAPTOP-001", result.get(0).getSku());
		assertEquals("BOOK-001", result.get(1).getSku());
		verify(productRepository, times(1)).findAll();
	}

	@Test
	@DisplayName("Test 2: Find product by ID - Success scenario")
	void testFindProductById_Success() {
		Category category = Category.builder()
				.categoryId(1)
				.categoryTitle("Electronics")
				.imageUrl("http://example.com/electronics.jpg")
				.build();
				
		Product product = Product.builder()
				.productId(1)
				.productTitle("Laptop")
				.imageUrl("http://example.com/laptop.jpg")
				.sku("LAPTOP-001")
				.priceUnit(999.99)
				.quantity(10)
				.category(category)
				.build();
		
		when(productRepository.findById(1)).thenReturn(Optional.of(product));
		
		ProductDto result = productService.findById(1);
		
		assertNotNull(result);
		assertEquals(1, result.getProductId());
		assertEquals("Laptop", result.getProductTitle());
		assertEquals("LAPTOP-001", result.getSku());
		assertEquals(999.99, result.getPriceUnit());
		assertEquals(10, result.getQuantity());
		assertNotNull(result.getCategoryDto());
		assertEquals("Electronics", result.getCategoryDto().getCategoryTitle());
		verify(productRepository, times(1)).findById(1);
	}
	
	@Test
	@DisplayName("Test 3: Find product by ID - Product not found")
	void testFindProductById_NotFound() {
		when(productRepository.findById(999)).thenReturn(Optional.empty());

		assertThrows(ProductNotFoundException.class, () -> productService.findById(999));
		verify(productRepository, times(1)).findById(999);
	}

	@Test
	@DisplayName("Test 4: Save product - Success scenario")
	void testSaveProduct_Success() {
		CategoryDto categoryDto = CategoryDto.builder()
				.categoryId(1)
				.categoryTitle("Electronics")
				.imageUrl("http://example.com/electronics.jpg")
				.build();

		ProductDto productDto = ProductDto.builder()
				.productTitle("New Laptop")
				.imageUrl("http://example.com/new-laptop.jpg")
				.sku("LAPTOP-002")
				.priceUnit(1299.99)
				.quantity(5)
				.categoryDto(categoryDto)
				.build();

		Category category = Category.builder()
				.categoryId(1)
				.categoryTitle("Electronics")
				.imageUrl("http://example.com/electronics.jpg")
				.build();

		Product productToSave = Product.builder()
				.productTitle("New Laptop")
				.imageUrl("http://example.com/new-laptop.jpg")
				.sku("LAPTOP-002")
				.priceUnit(1299.99)
				.quantity(5)
				.category(category)
				.build();

		Product savedProduct = Product.builder()
				.productId(3)
				.productTitle("New Laptop")
				.imageUrl("http://example.com/new-laptop.jpg")
				.sku("LAPTOP-002")
				.priceUnit(1299.99)
				.quantity(5)
				.category(category)
				.build();

		when(productRepository.save(productToSave)).thenReturn(savedProduct);

		ProductDto result = productService.save(productDto);

		assertNotNull(result);
		assertEquals(3, result.getProductId());
		assertEquals("New Laptop", result.getProductTitle());
		assertEquals("LAPTOP-002", result.getSku());
		verify(productRepository, times(1)).save(productToSave);
	}

	@Test
	@DisplayName("Test 5: Find all categories - Success scenario")
	void testFindAllCategories() {
		List<Category> categories = new ArrayList<>();
		
		Category category1 = Category.builder()
				.categoryId(1)
				.categoryTitle("Electronics")
				.imageUrl("http://example.com/electronics.jpg")
				.build();
				
		Category category2 = Category.builder()
				.categoryId(2)
				.categoryTitle("Books")
				.imageUrl("http://example.com/books.jpg")
				.build();
		
		categories.add(category1);
		categories.add(category2);
		
		when(categoryRepository.findAll()).thenReturn(categories);
		
		List<CategoryDto> result = categoryService.findAll();
		
		assertEquals(2, result.size());
		assertEquals("Electronics", result.get(0).getCategoryTitle());
		assertEquals("Books", result.get(1).getCategoryTitle());
		verify(categoryRepository, times(1)).findAll();
	}

	@Test
	@DisplayName("Test 6: Find category by ID - Success scenario")
	void testFindCategoryById_Success() {
		Category category = Category.builder()
				.categoryId(1)
				.categoryTitle("Electronics")
				.imageUrl("http://example.com/electronics.jpg")
				.build();
		
		when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
		
		CategoryDto result = categoryService.findById(1);
		
		assertNotNull(result);
		assertEquals(1, result.getCategoryId());
		assertEquals("Electronics", result.getCategoryTitle());
		assertEquals("http://example.com/electronics.jpg", result.getImageUrl());
		verify(categoryRepository, times(1)).findById(1);
	}
	
	@Test
	@DisplayName("Test 7: Find category by ID - Category not found")
	void testFindCategoryById_NotFound() {
		when(categoryRepository.findById(999)).thenReturn(Optional.empty());

		assertThrows(CategoryNotFoundException.class, () -> categoryService.findById(999));
		verify(categoryRepository, times(1)).findById(999);
	}

	@Test
	@DisplayName("Test 8: Update product - Success scenario")
	void testUpdateProduct_Success() {
		CategoryDto categoryDto = CategoryDto.builder()
				.categoryId(1)
				.categoryTitle("Electronics")
				.imageUrl("http://example.com/electronics.jpg")
				.build();

		ProductDto productDto = ProductDto.builder()
				.productId(1)
				.productTitle("Updated Laptop")
				.imageUrl("http://example.com/updated-laptop.jpg")
				.sku("LAPTOP-001")
				.priceUnit(899.99)
				.quantity(15)
				.categoryDto(categoryDto)
				.build();

		Category category = Category.builder()
				.categoryId(1)
				.categoryTitle("Electronics")
				.imageUrl("http://example.com/electronics.jpg")
				.build();

		Product productToUpdate = Product.builder()
				.productId(1)
				.productTitle("Updated Laptop")
				.imageUrl("http://example.com/updated-laptop.jpg")
				.sku("LAPTOP-001")
				.priceUnit(899.99)
				.quantity(15)
				.category(category)
				.build();

		when(productRepository.save(productToUpdate)).thenReturn(productToUpdate);

		ProductDto result = productService.update(productDto);

		assertNotNull(result);
		assertEquals(1, result.getProductId());
		assertEquals("Updated Laptop", result.getProductTitle());
		assertEquals(899.99, result.getPriceUnit());
		assertEquals(15, result.getQuantity());
		verify(productRepository, times(1)).save(productToUpdate);
	}

}






