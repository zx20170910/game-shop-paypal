package com.unicorn.gameshop.order.controller;

import com.unicorn.gameshop.common.ApiResponse;
import com.unicorn.gameshop.order.dto.OrderStatusResponse;
import com.unicorn.gameshop.order.service.OrderQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderQueryService orderQueryService;

    public OrderController(OrderQueryService orderQueryService) {
        this.orderQueryService = orderQueryService;
    }

    @GetMapping("/{orderNo}/status")
    public ApiResponse<OrderStatusResponse> status(
            @PathVariable String orderNo,
            @RequestHeader(value = "X-Order-Access-Token", required = false) String accessToken) {
        return new ApiResponse<>(orderQueryService.status(orderNo, accessToken));
    }
}
