package com.msdamine.productservice.controller;

import com.msdamine.productservice.entity.Product;
import com.msdamine.productservice.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void contextLoads() {
    }

    @Test
    void createProduct_shouldReturnCreatedProduct() throws Exception {

        Product product = new Product(
                "Laptop",
                "Gaming laptop",
                new BigDecimal("1200.00"),
                10
        );

        when(productService.createProduct(any(Product.class)))
                .thenReturn(product);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Laptop",
                                "description": "Gaming laptop",
                                "price": 1200.00,
                                "stockQuantity": 10
                            }
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.price").value(1200.00))
                .andExpect(jsonPath("$.stockQuantity").value(10));

        verify(productService).createProduct(any(Product.class));
    }
    @Test
    void createProduct_withInvalidData_shouldReturnBadRequest() throws Exception {

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "",
                                "description": "Gaming laptop",
                                "price": -10.00,
                                "stockQuantity": -5
                            }
                            """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }
    @Test
    void getAllProducts_shouldReturnProductList() throws Exception {

        List<Product> products = List.of(
                new Product("Laptop", "Gaming laptop",
                        new BigDecimal("1200.00"), 10),
                new Product("Mouse", "Wireless mouse",
                        new BigDecimal("25.00"), 50)
        );

        when(productService.getAllProducts()).thenReturn(products);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Laptop"))
                .andExpect(jsonPath("$[1].name").value("Mouse"));

        verify(productService).getAllProducts();
    }
    @Test
    void getProductById_shouldReturnProduct() throws Exception {

        Product product = new Product(
                "Laptop",
                "Gaming laptop",
                new BigDecimal("1200.00"),
                10
        );

        when(productService.getProductById(1L)).thenReturn(product);

        mockMvc.perform(get("/api/products/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.price").value(1200.00))
                .andExpect(jsonPath("$.stockQuantity").value(10));

        verify(productService).getProductById(1L);
    }
    @Test
    void getProductById_whenProductDoesNotExist_shouldReturnNotFound()
            throws Exception {

        when(productService.getProductById(999L))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found with id: 999"
                ));

        mockMvc.perform(get("/api/products/{id}", 999L))
                .andExpect(status().isNotFound());

        verify(productService).getProductById(999L);
    }
    @Test
    void updateProduct_shouldReturnUpdatedProduct() throws Exception {

        Product updatedProduct = new Product(
                "Updated Laptop",
                "Professional laptop",
                new BigDecimal("1500.00"),
                15
        );

        when(productService.updateProduct(
                org.mockito.ArgumentMatchers.eq(1L),
                any(Product.class)
        )).thenReturn(updatedProduct);

        mockMvc.perform(put("/api/products/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Updated Laptop",
                                "description": "Professional laptop",
                                "price": 1500.00,
                                "stockQuantity": 15
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Laptop"))
                .andExpect(jsonPath("$.price").value(1500.00))
                .andExpect(jsonPath("$.stockQuantity").value(15));

        verify(productService).updateProduct(
                org.mockito.ArgumentMatchers.eq(1L),
                any(Product.class)
        );
    }
    @Test
    void updateProduct_withInvalidData_shouldReturnBadRequest()
            throws Exception {

        mockMvc.perform(put("/api/products/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "",
                                "description": "Invalid product",
                                "price": -10.00,
                                "stockQuantity": -5
                            }
                            """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }
    @Test
    void deleteProduct_shouldReturnNoContent() throws Exception {

        mockMvc.perform(delete("/api/products/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct(1L);
    }
}
