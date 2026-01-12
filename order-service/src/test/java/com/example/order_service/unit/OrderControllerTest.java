package com.example.order_service.unit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.order_service.DTO.PlaceOrderRequest;
import com.example.order_service.DTO.PlaceOrderResponse;
import com.example.order_service.DTO.PlaceOrderRequest.OrderItem;
import com.example.order_service.controller.OrderController;
import com.example.order_service.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@WebMvcTest(OrderController.class)
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @DisplayName("Should return 201 when place order success")
    void shouldReturn201_WhenPlaceOrderSuccess() throws Exception {
        // arrange
        PlaceOrderRequest.OrderItem item = new PlaceOrderRequest.OrderItem(101L, 2);
        PlaceOrderRequest request = new PlaceOrderRequest(1L, List.of(item));

        PlaceOrderResponse response = new PlaceOrderResponse(
                1L,
                10L,
                UUID.randomUUID(),
                List.of(new PlaceOrderResponse.OrderItem(101L, 2)));

        when(orderService.placeOrder(any(PlaceOrderRequest.class))).thenReturn(response);

        // act & assert
        mockMvc.perform(
                post("/v1/order").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(10L))
                .andExpect(jsonPath("$.customerId").value(1L))
                .andExpect(jsonPath("$.items[0].productId").value(101L));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when request is invalid")
    void shouldReturn400_WhenRequestIsInvalid() throws Exception {
        // arrange
        OrderItem item = new OrderItem(10L, 2);
        PlaceOrderRequest request = new PlaceOrderRequest(null, List.of(item));

        // act & assert
        mockMvc.perform(
                post("/v1/order")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isBadRequest());
    }
}
