package com.unicorn.gameshop.catalog.service;

import com.unicorn.gameshop.catalog.dto.GameRequest;
import com.unicorn.gameshop.catalog.dto.ProductRequest;
import com.unicorn.gameshop.catalog.dto.ServerRequest;
import com.unicorn.gameshop.catalog.mapper.GameMapper;
import com.unicorn.gameshop.catalog.mapper.ServerMapper;
import com.unicorn.gameshop.catalog.model.Game;
import com.unicorn.gameshop.catalog.model.Server;
import com.unicorn.gameshop.common.ApiErrorCode;
import com.unicorn.gameshop.common.BusinessException;
import com.unicorn.gameshop.common.IdGenerator;
import com.unicorn.gameshop.order.mapper.ProductMapper;
import com.unicorn.gameshop.order.model.Product;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class CatalogService {

    private final GameMapper gameMapper;
    private final ServerMapper serverMapper;
    private final ProductMapper productMapper;

    public CatalogService(GameMapper gameMapper, ServerMapper serverMapper, ProductMapper productMapper) {
        this.gameMapper = gameMapper;
        this.serverMapper = serverMapper;
        this.productMapper = productMapper;
    }

    @Transactional
    public Game saveGame(String id, GameRequest request) {
        Instant now = Instant.now();
        Game game = id == null ? new Game() : gameMapper.findById(id);
        if (game == null) {
            throw new BusinessException(ApiErrorCode.PRODUCT_NOT_AVAILABLE, "Game does not exist");
        }
        if (id == null) {
            game.setId(IdGenerator.id());
            game.setCreatedAt(now);
        }
        game.setCode(request.code().trim());
        game.setName(request.name().trim());
        game.setPublisher(request.publisher());
        game.setSupportedPlatforms(request.supportedPlatforms());
        game.setDeliveryType(defaultManual(request.deliveryType()));
        game.setStatus(request.status().trim().toUpperCase());
        game.setUpdatedAt(now);
        if (id == null) {
            gameMapper.insert(game);
        } else if (gameMapper.update(game) != 1) {
            throw new BusinessException(ApiErrorCode.PRODUCT_NOT_AVAILABLE, "Game update failed");
        }
        return game;
    }

    @Transactional
    public Product saveProduct(String id, ProductRequest request) {
        requireGame(request.gameId());
        Instant now = Instant.now();
        Product product = id == null ? new Product() : productMapper.findById(id);
        if (product == null) {
            throw new BusinessException(ApiErrorCode.PRODUCT_NOT_AVAILABLE, "Product does not exist");
        }
        if (id == null) {
            product.setId(IdGenerator.id());
            product.setCreatedAt(now);
        }
        product.setGameId(request.gameId());
        product.setSku(request.sku().trim());
        product.setName(request.name().trim());
        product.setAmountMinor(request.amountMinor());
        product.setCurrency(request.currency().trim().toUpperCase());
        product.setPlatform(request.platform().trim());
        product.setServerRegion(request.serverRegion().trim());
        product.setDeliveryType(defaultManual(request.deliveryType()));
        product.setStatus(request.status().trim().toUpperCase());
        product.setUpdatedAt(now);
        if (id == null) {
            productMapper.insert(product);
        } else if (productMapper.update(product) != 1) {
            throw new BusinessException(ApiErrorCode.PRODUCT_NOT_AVAILABLE, "Product update failed");
        }
        return product;
    }

    @Transactional
    public Server saveServer(String id, ServerRequest request) {
        requireGame(request.gameId());
        Instant now = Instant.now();
        Server server = id == null ? new Server() : serverMapper.findById(id);
        if (server == null) {
            throw new BusinessException(ApiErrorCode.PRODUCT_NOT_AVAILABLE, "Server does not exist");
        }
        if (id == null) {
            server.setId(IdGenerator.id());
            server.setCreatedAt(now);
        }
        server.setGameId(request.gameId());
        server.setCode(request.code().trim());
        server.setName(request.name().trim());
        server.setRegion(request.region().trim());
        server.setStatus(request.status().trim().toUpperCase());
        server.setUpdatedAt(now);
        if (id == null) {
            serverMapper.insert(server);
        } else if (serverMapper.update(server) != 1) {
            throw new BusinessException(ApiErrorCode.PRODUCT_NOT_AVAILABLE, "Server update failed");
        }
        return server;
    }

    public List<Game> games(String status) { return gameMapper.list(status); }

    public List<Product> products(String gameId, String status) { return productMapper.list(gameId, status); }

    public List<Server> servers(String gameId, String status) { return serverMapper.list(gameId, status); }

    private void requireGame(String gameId) {
        if (gameMapper.findById(gameId) == null) {
            throw new BusinessException(ApiErrorCode.PRODUCT_NOT_AVAILABLE, "Game does not exist");
        }
    }

    private String defaultManual(String deliveryType) {
        String value = deliveryType == null || deliveryType.isBlank() ? "MANUAL" : deliveryType.trim().toUpperCase();
        if (!"MANUAL".equals(value)) {
            throw new BusinessException(ApiErrorCode.BAD_REQUEST,
                    "Only MANUAL delivery is enabled in the first phase");
        }
        return value;
    }
}
