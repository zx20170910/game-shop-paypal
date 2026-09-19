package com.unicorn.gameshop.order.mapper;

import com.unicorn.gameshop.order.model.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductMapper {

    Product findAvailableById(@Param("id") String id);

    Product findById(@Param("id") String id);

    List<Product> list(@Param("gameId") String gameId, @Param("status") String status);

    int insert(Product product);

    int update(Product product);
}
