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

    public Money getPriceIncreaseCapacity() {
        return Money.of(maxPrice.subtract(actualPrice), currencyCode);
    }

    public Money getPriceReduceCapacity() {
        return Money.of(actualPrice.subtract(minPrice), currencyCode);
    }

    public final Money getActualPriceMoney() {
        return Money.of(actualPrice, currencyCode);
    }

}
