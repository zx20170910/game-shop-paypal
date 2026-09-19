package com.unicorn.gameshop.catalog.controller;

import com.unicorn.gameshop.admin.service.AuditService;
import com.unicorn.gameshop.catalog.dto.GameRequest;
import com.unicorn.gameshop.catalog.dto.ProductRequest;
import com.unicorn.gameshop.catalog.dto.ServerRequest;
import com.unicorn.gameshop.catalog.model.Game;
import com.unicorn.gameshop.catalog.model.Server;
import com.unicorn.gameshop.catalog.service.CatalogService;
import com.unicorn.gameshop.common.ApiResponse;
import com.unicorn.gameshop.order.model.Product;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/catalog")
@PreAuthorize("hasRole('ADMIN')")
public class CatalogAdminController {

    private final CatalogService catalogService;
    private final AuditService auditService;

    public CatalogAdminController(CatalogService catalogService, AuditService auditService) {
        this.catalogService = catalogService;
        this.auditService = auditService;
    }

    @PostMapping("/games")
    public ApiResponse<Game> createGame(@Valid @RequestBody GameRequest request, Authentication authentication) {
        Game game = catalogService.saveGame(null, request);
        auditService.record(authentication, "GAME_CREATE", "GAME", game.getId(), Map.of("code", game.getCode()));
        return new ApiResponse<>(game);
    }

    @PutMapping("/games/{id}")
    public ApiResponse<Game> updateGame(@PathVariable String id,
                                        @Valid @RequestBody GameRequest request,
                                        Authentication authentication) {
        Game game = catalogService.saveGame(id, request);
        auditService.record(authentication, "GAME_UPDATE", "GAME", id, Map.of());
        return new ApiResponse<>(game);
    }

    @GetMapping("/games")
    public ApiResponse<List<Game>> games(@RequestParam(required = false) String status) {
        return new ApiResponse<>(catalogService.games(status));
    }

    @PostMapping("/products")
    public ApiResponse<Product> createProduct(@Valid @RequestBody ProductRequest request,
                                              Authentication authentication) {
        Product product = catalogService.saveProduct(null, request);
        auditService.record(authentication, "PRODUCT_CREATE", "PRODUCT", product.getId(), Map.of("sku", product.getSku()));
        return new ApiResponse<>(product);
    }

    @PutMapping("/products/{id}")
    public ApiResponse<Product> updateProduct(@PathVariable String id,
                                              @Valid @RequestBody ProductRequest request,
                                              Authentication authentication) {
        Product product = catalogService.saveProduct(id, request);
        auditService.record(authentication, "PRODUCT_UPDATE", "PRODUCT", id, Map.of());
        return new ApiResponse<>(product);
    }

    @GetMapping("/products")
    public ApiResponse<List<Product>> products(@RequestParam(required = false) String gameId,
                                               @RequestParam(required = false) String status) {
        return new ApiResponse<>(catalogService.products(gameId, status));
    }

    @PostMapping("/servers")
    public ApiResponse<Server> createServer(@Valid @RequestBody ServerRequest request,
                                            Authentication authentication) {
        Server server = catalogService.saveServer(null, request);
        auditService.record(authentication, "SERVER_CREATE", "SERVER", server.getId(), Map.of("code", server.getCode()));
        return new ApiResponse<>(server);
    }

    @PutMapping("/servers/{id}")
    public ApiResponse<Server> updateServer(@PathVariable String id,
                                            @Valid @RequestBody ServerRequest request,
                                            Authentication authentication) {
        Server server = catalogService.saveServer(id, request);
        auditService.record(authentication, "SERVER_UPDATE", "SERVER", id, Map.of());
        return new ApiResponse<>(server);
    }

    @GetMapping("/servers")
    public ApiResponse<List<Server>> servers(@RequestParam(required = false) String gameId,
                                             @RequestParam(required = false) String status) {
        return new ApiResponse<>(catalogService.servers(gameId, status));
    }
}
