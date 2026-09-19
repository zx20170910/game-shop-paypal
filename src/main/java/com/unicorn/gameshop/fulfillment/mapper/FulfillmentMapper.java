package com.unicorn.gameshop.fulfillment.mapper;

import com.unicorn.gameshop.fulfillment.model.Fulfillment;
import com.unicorn.gameshop.order.model.FulfillmentStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;

@Mapper
public interface FulfillmentMapper {

    int insert(Fulfillment fulfillment);

    Fulfillment findById(@Param("id") String id);

    Fulfillment findByOrderId(@Param("orderId") String orderId);

    List<Fulfillment> list(@Param("status") FulfillmentStatus status,
                           @Param("limit") int limit);

    int claim(@Param("id") String id,
              @Param("operatorId") String operatorId,
              @Param("claimedAt") Instant claimedAt,
              @Param("updatedAt") Instant updatedAt);

    int complete(@Param("id") String id,
                 @Param("operatorId") String operatorId,
                 @Param("proofObjectKey") String proofObjectKey,
                 @Param("deliveryNote") String deliveryNote,
                 @Param("updatedAt") Instant updatedAt);

    int review(@Param("id") String id,
               @Param("status") FulfillmentStatus status,
               @Param("reviewedAt") Instant reviewedAt,
               @Param("deliveredAt") Instant deliveredAt,
               @Param("updatedAt") Instant updatedAt);

    int reclaimExpired(@Param("cutoff") Instant cutoff,
                       @Param("updatedAt") Instant updatedAt);

    int requeue(@Param("id") String id,
                @Param("reason") String reason,
                @Param("updatedAt") Instant updatedAt);
}
