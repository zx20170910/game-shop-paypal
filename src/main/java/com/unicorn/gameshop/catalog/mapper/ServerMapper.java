package com.unicorn.gameshop.catalog.mapper;

import com.unicorn.gameshop.catalog.model.Server;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ServerMapper {

    int insert(Server server);

    int update(Server server);

    Server findById(@Param("id") String id);

    List<Server> list(@Param("gameId") String gameId, @Param("status") String status);
}
