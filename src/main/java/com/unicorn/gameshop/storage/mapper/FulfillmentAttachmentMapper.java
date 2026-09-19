package com.unicorn.gameshop.storage.mapper;

import com.unicorn.gameshop.storage.model.FulfillmentAttachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.time.Instant;

@Mapper
public interface FulfillmentAttachmentMapper {

    int insert(FulfillmentAttachment attachment);

    FulfillmentAttachment findById(@Param("id") String id);

    int confirmUploaded(@Param("id") String id,
                        @Param("fulfillmentId") String fulfillmentId,
                        @Param("verifiedBy") String verifiedBy,
                        @Param("verifiedAt") Instant verifiedAt);

    List<FulfillmentAttachment> listByFulfillmentId(@Param("fulfillmentId") String fulfillmentId);

    int invalidateConfirmed(@Param("fulfillmentId") String fulfillmentId);
}
