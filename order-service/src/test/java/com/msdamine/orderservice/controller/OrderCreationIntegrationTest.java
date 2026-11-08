
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

    @Test
    void shouldCreateOrderAndPersistItemsInDatabase() throws Exception {

        // Arrange
        String requestJson = """
                {
                    "customerId": 15,
                    "items": [
                        {
                            "productId": 101,
                            "quantity": 2,
                            "unitPrice": 199.99
                        },
                        {
                            "productId": 205,
                            "quantity": 1,
                            "unitPrice": 49.99
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
                .andExpect(jsonPath("$.items[1].productId").value(205))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Read generated order ID from response
        ObjectMapper objectMapper = new ObjectMapper();

        Long orderId = objectMapper
                .readTree(responseBody)
                .get("id")
                .asLong();

        assertTrue(orderId > 0);

        // Force SQL synchronization and clear persistence context
        entityManager.flush();
        entityManager.clear();

        // Assert: reload order from PostgreSQL
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

        assertEquals(
                101L,
                persistedOrder.getItems().get(0).getProductId()
        );

        assertEquals(
                205L,
                persistedOrder.getItems().get(1).getProductId()
        );
    }
}
