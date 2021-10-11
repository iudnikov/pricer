package com.theneuron.pricer.model.messages;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Builder
@Value
public class BidResponseMessage {

    @NonNull
    public String type;

    @NonNull
    public Instant time;

    public Meta meta;

    @AllArgsConstructor
    @Builder
    @Value
    public static class Meta {

        String ssp;

        @JsonProperty("request_id")
        String requestId;

        @JsonProperty("screen_id")
        String screenId;

        @Singular
        @JsonProperty("campaign_ids")
        List<String> campaignIds;

        @Singular
        @JsonProperty("line_item_ids")
        List<String> lineItemIds;

        @Singular
        @JsonProperty("impression_ids")
        List<String> impressionIds;

        @Singular
        @JsonProperty("bid_ids")
        List<String> bidIds;

        @Singular
        @JsonProperty("prices")
        List<Double> prices;

        @Singular
        @JsonProperty("deal_ids")
        List<String> dealIds;

        @Singular
        @JsonProperty("currencies")
        List<String> currencies;


        @Singular
        @JsonProperty("deal_bid_floors")
        List<Double> dealBidFloors;

        @Singular
        @JsonProperty("deal_bid_floor_curs")
        List<String> dealBidFloorCurs;

        @Singular
        @JsonProperty("imp_bid_floors")
        List<Double> impBidFloors;

        @Singular
        @JsonProperty("imp_bid_floor_curs")
        List<String> impBidFloorCurs;

        @Singular
        @JsonProperty("imp_multiplies")
        List<Double> impMultiplies;

        @Singular
        @JsonProperty("line_item_prices")
        List<Double> lineItemPrices;

        @Singular
        @JsonProperty("line_item_currencies")
        List<String> lineItemCurrencies;

        @Singular
        @JsonProperty("directive_ids")
        List<UUID> directiveIds;

    }

    public static BidResponseMessageBuilder of(Instant at, Meta meta) {
        return builder().type("bid_response").time(at).meta(meta);
    }

}
