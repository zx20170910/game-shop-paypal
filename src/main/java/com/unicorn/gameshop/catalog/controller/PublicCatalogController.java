package com.unicorn.gameshop.catalog.controller;

import com.unicorn.gameshop.catalog.model.Game;
import com.unicorn.gameshop.catalog.model.Server;
import com.unicorn.gameshop.catalog.service.CatalogService;
import com.unicorn.gameshop.common.ApiResponse;
import com.unicorn.gameshop.order.model.Product;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalog")
public class PublicCatalogController {

    private final CatalogService catalogService;

    public PublicCatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/games")
    public ApiResponse<List<Game>> games() { return new ApiResponse<>(catalogService.games("ACTIVE")); }

    @GetMapping("/products")
    public ApiResponse<List<Product>> products(@RequestParam(required = false) String gameId) {
        return new ApiResponse<>(catalogService.products(gameId, "ACTIVE"));
    }

    @GetMapping("/servers")
    public ApiResponse<List<Server>> servers(@RequestParam String gameId) {
        return new ApiResponse<>(catalogService.servers(gameId, "ACTIVE"));
    }
}
