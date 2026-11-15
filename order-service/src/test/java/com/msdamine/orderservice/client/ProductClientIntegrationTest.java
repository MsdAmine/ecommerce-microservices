package com.msdamine.orderservice.client;

import com.msdamine.orderservice.dto.ProductResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@EnabledIfEnvironmentVariable(
        named = "RUN_FEIGN_IT",
        matches = "true"
)
class ProductClientIntegrationTest {

    @Autowired
    private ProductClient productClient;

    @Test
    void shouldRetrieveProductFromProductService() {

        // Act
        ProductResponse product =
                productClient.getProductById(2L);

        // Assert
        assertNotNull(product);

        assertEquals(2L, product.id());
        assertEquals("Test Mouse", product.name());

        assertEquals(
                0,
                new BigDecimal("29.99").compareTo(product.price())
        );

        assertEquals(10, product.stockQuantity());
    }
}