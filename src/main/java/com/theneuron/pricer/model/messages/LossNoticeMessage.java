package com.theneuron.pricer.model.messages;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;

@Value
@Builder
@AllArgsConstructor
public class LossNoticeMessage {

    @NonNull
    String type;

    @NonNull
    Instant time;

    String content;

    Meta meta;

    @Value
    @Builder
    @AllArgsConstructor
    public static class Meta {

        @JsonProperty("ssp")
        String ssp;

        @JsonProperty("request_id")
        String requestId;

        @JsonProperty("impression_id")
        String impressionId;

        @JsonProperty("bid_id")
        String bidId;

        @JsonProperty("screen_id")
        String screenId;

        @JsonProperty("instance_id")
        String instanceId;

        @JsonProperty("agency_id")
        String agencyId;

        @JsonProperty("advertiser_id")
        String advertiserId;

        @JsonProperty("campaign_id")
        String campaignId;

        @JsonProperty("line_item_id")
        String lineItemId;

        @JsonProperty("creative_id")
        String creativeId;

        @JsonProperty("loss_reason")
        String lossReason;

    }

    public static LossNoticeMessage.LossNoticeMessageBuilder of(Instant at, LossNoticeMessage.Meta meta) {
        return builder().type("loss_notice").time(at).meta(meta);
    }

}