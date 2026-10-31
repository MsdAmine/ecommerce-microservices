package com.msdamine.productservice.service;

import com.msdamine.productservice.entity.Product;
import com.msdamine.productservice.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void createProduct_shouldSaveAndReturnProduct() {
        Product product = new Product(
                "Laptop",
                "Gaming laptop",
                new BigDecimal("1200.00"),
                10
        );

        when(productRepository.save(product)).thenReturn(product);

        Product result = productService.createProduct(product);

        assertEquals("Laptop", result.getName());
        assertEquals(new BigDecimal("1200.00"), result.getPrice());
        verify(productRepository).save(product);
    }

    @Test
    void getAllProducts_shouldReturnAllProducts() {
        List<Product> products = List.of(
                new Product("Laptop", "Gaming laptop", new BigDecimal("1200.00"), 10),
                new Product("Mouse", "Wireless mouse", new BigDecimal("25.00"), 50)
        );

        when(productRepository.findAll()).thenReturn(products);

        List<Product> result = productService.getAllProducts();

        assertEquals(2, result.size());
        assertEquals("Laptop", result.get(0).getName());
        assertEquals("Mouse", result.get(1).getName());
        verify(productRepository).findAll();
    }

    @Test
    void getProductById_whenProductExists_shouldReturnProduct() {
        Product product = new Product(
                "Laptop",
                "Gaming laptop",
                new BigDecimal("1200.00"),
                10
        );

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        Product result = productService.getProductById(1L);

        assertEquals("Laptop", result.getName());
        verify(productRepository).findById(1L);
    }

    @Test
    void getProductById_whenProductDoesNotExist_shouldThrowNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productService.getProductById(99L)
        );

        assertEquals(404, exception.getStatusCode().value());
        verify(productRepository).findById(99L);
    }
    @Test
    void updateProduct_whenProductExists_shouldUpdateAndReturnProduct() {
        Product existingProduct = new Product(
                "Laptop",
                "Old description",
                new BigDecimal("1000.00"),
                5
        );

        Product updatedProduct = new Product(
                "Gaming Laptop",
                "Updated description",
                new BigDecimal("1500.00"),
                10
        );

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(existingProduct));
        when(productRepository.save(existingProduct))
                .thenReturn(existingProduct);

        Product result = productService.updateProduct(1L, updatedProduct);

        assertEquals("Gaming Laptop", result.getName());
        assertEquals("Updated description", result.getDescription());
        assertEquals(new BigDecimal("1500.00"), result.getPrice());
        assertEquals(10, result.getStockQuantity());

        verify(productRepository).findById(1L);
        verify(productRepository).save(existingProduct);
    }

    @Test
    void updateProduct_whenProductDoesNotExist_shouldThrowNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        Product updatedProduct = new Product(
                "Laptop",
                "Gaming laptop",
                new BigDecimal("1200.00"),
                10
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productService.updateProduct(99L, updatedProduct)
        );

        assertEquals(404, exception.getStatusCode().value());
        verify(productRepository, never()).save(updatedProduct);
    }

    @Test
    void deleteProduct_whenProductExists_shouldDeleteProduct() {
        Product product = new Product(
                "Laptop",
                "Gaming laptop",
                new BigDecimal("1200.00"),
                10
        );

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        productService.deleteProduct(1L);

        verify(productRepository).findById(1L);
        verify(productRepository).delete(product);
    }

    @Test
    void deleteProduct_whenProductDoesNotExist_shouldThrowNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productService.deleteProduct(99L)
        );

        assertEquals(404, exception.getStatusCode().value());
        verify(productRepository, never()).delete(org.mockito.ArgumentMatchers.any(Product.class));
    }
}