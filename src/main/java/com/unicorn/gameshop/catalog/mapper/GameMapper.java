package com.unicorn.gameshop.catalog.mapper;

import com.unicorn.gameshop.catalog.model.Game;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;

@Mapper
public interface GameMapper {

    int insert(Game game);

    int update(Game game);

    Game findById(@Param("id") String id);

    List<Game> list(@Param("status") String status);
}
