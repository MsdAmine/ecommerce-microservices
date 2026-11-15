
package com.msdamine.orderservice.controller;

import com.msdamine.orderservice.entity.Order;
import com.msdamine.orderservice.enums.OrderStatus;
import com.msdamine.orderservice.repository.OrderRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.msdamine.orderservice.client.ProductClient;
import com.msdamine.orderservice.dto.ProductResponse;

import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrderCreationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private ProductClient productClient;


    @Test
    void shouldCreateOrderAndPersistItemsInDatabase() throws Exception {

        // Arrange: simulate Product Service responses
        when(productClient.getProductById(101L))
                .thenReturn(new ProductResponse(
                        101L,
                        "Product A",
                        new BigDecimal("199.99"),
                        10
                ));

        when(productClient.getProductById(205L))
                .thenReturn(new ProductResponse(
                        205L,
                        "Product B",
                        new BigDecimal("49.99"),
                        5
                ));

        // Client no longer sends unitPrice
        String requestJson = """
            {
                "customerId": 15,
                "items": [
                    {
                        "productId": 101,
                        "quantity": 2
                    },
                    {
                        "productId": 205,
                        "quantity": 1
                    }
                ]
            }
            """;

        // Act: send HTTP POST request
        String responseBody = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.customerId").value(15))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(449.97))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].productId").value(101))
                .andExpect(jsonPath("$.items[0].unitPrice").value(199.99))
                .andExpect(jsonPath("$.items[1].productId").value(205))
                .andExpect(jsonPath("$.items[1].unitPrice").value(49.99))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract generated order ID
        ObjectMapper objectMapper = new ObjectMapper();

        Long orderId = objectMapper
                .readTree(responseBody)
                .get("id")
                .asLong();

        assertTrue(orderId > 0);

        // Synchronize database changes and clear persistence context
        entityManager.flush();
        entityManager.clear();

        // Assert: retrieve the persisted order from PostgreSQL
        Order persistedOrder = orderRepository.findById(orderId)
                .orElseThrow();

        assertEquals(15L, persistedOrder.getCustomerId());
        assertEquals(OrderStatus.PENDING, persistedOrder.getStatus());

        assertEquals(
                0,
                new BigDecimal("449.97")
                        .compareTo(persistedOrder.getTotalAmount())
        );

        assertEquals(2, persistedOrder.getItems().size());

        // Verify stored product IDs
        assertEquals(
                101L,
                persistedOrder.getItems().get(0).getProductId()
        );

        assertEquals(
                205L,
                persistedOrder.getItems().get(1).getProductId()
        );

        // Verify stored prices came from ProductClient
        assertEquals(
                0,
                new BigDecimal("199.99").compareTo(
                        persistedOrder.getItems().get(0).getUnitPrice()
                )
        );

        assertEquals(
                0,
                new BigDecimal("49.99").compareTo(
                        persistedOrder.getItems().get(1).getUnitPrice()
                )
        );
    }

}
