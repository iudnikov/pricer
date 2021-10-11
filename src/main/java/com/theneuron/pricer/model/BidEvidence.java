package com.theneuron.pricer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.javamoney.moneta.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@AllArgsConstructor
@Builder(toBuilder = true)
@Value
public class BidEvidence {
    @NonNull
    public String sspId;
    @NonNull
    public String screenId;
    @NonNull
    public String lineItemId;
    public String dealId;
    @NonNull
    public String requestId;
    @NonNull
    public Instant timestamp;
    @NonNull
    public BigDecimal minPrice;
    @NonNull
    public BigDecimal maxPrice;
    @NonNull
    public BigDecimal actualPrice;
    @NonNull
    public String currencyCode;
    @NonNull
    @Builder.Default
    public Optional<UUID> directiveId = Optional.empty();

    public final Money priceIncreaseCapacity() {
        return Money.of(maxPrice.subtract(actualPrice), currencyCode);
    }

    public final Money priceReduceCapacity() {
        return Money.of(actualPrice.subtract(minPrice), currencyCode);
    }

    public final Money actualPriceMoney() {
        return Money.of(actualPrice, currencyCode);
    }

}
