package com.theneuron.pricer.model.messages;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.lang.Nullable;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class WinNoticeMessage {

    @NonNull String type;

    @NonNull Instant time;

    String content;

    Meta meta;

    @Value
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
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

        @JsonProperty("imp_multiply")
        Double impMultiply;

        @JsonProperty("win_price")
        Double winPrice;

        @Nullable
        @JsonProperty("win_cost") //optional, it exists only if SSP provides such info
        Double winCost;

        @JsonProperty("bid_currency")
        String currency;

        @JsonProperty("instance_markup")
        Double instanceMarkup;

        @JsonProperty("advertiser_markup")
        Double advertiserMarkup;

        @JsonProperty("directive_id")
        Optional<UUID> directiveId;
    }

}