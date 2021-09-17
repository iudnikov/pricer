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
    public String screenId;
    public String lineItemId;
    public String dealId;
    public String requestId;
    public Instant timestamp;
    public BigDecimal minPrice;
    public BigDecimal maxPrice;
    public BigDecimal actualPrice;
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
