package com.unicorn.gameshop.order.mapper;

import com.unicorn.gameshop.order.model.FulfillmentStatus;
import com.unicorn.gameshop.order.model.Order;
import com.unicorn.gameshop.order.model.OrderStatus;
import com.unicorn.gameshop.order.model.PaymentStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderMapper {

    int insert(Order order);

    Order findById(@Param("id") String id);

    Order findByOrderNo(@Param("orderNo") String orderNo);

    int updatePayment(@Param("orderId") String orderId,
                      @Param("orderStatus") OrderStatus orderStatus,
                      @Param("paymentStatus") PaymentStatus paymentStatus,
                      @Param("fulfillmentStatus") FulfillmentStatus fulfillmentStatus,
                      @Param("updatedAt") java.time.Instant updatedAt);

    int updateFulfillment(@Param("orderId") String orderId,
                          @Param("orderStatus") OrderStatus orderStatus,
                          @Param("fulfillmentStatus") FulfillmentStatus fulfillmentStatus,
                          @Param("updatedAt") java.time.Instant updatedAt);

    int expirePending(@Param("now") java.time.Instant now,
                      @Param("updatedAt") java.time.Instant updatedAt);

    int countRecentByPlayerAndProduct(@Param("playerUid") String playerUid,
                                      @Param("productId") String productId,
                                      @Param("createdAfter") java.time.Instant createdAfter);

    int updateRisk(@Param("orderId") String orderId,
                   @Param("riskStatus") String riskStatus,
                   @Param("riskReason") String riskReason,
                   @Param("fulfillmentStatus") FulfillmentStatus fulfillmentStatus,
                   @Param("updatedAt") java.time.Instant updatedAt);
}
