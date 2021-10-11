package com.theneuron.pricer.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.javamoney.moneta.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
@Builder(toBuilder = true)
@Value
public class Directive {
    @NonNull
    public UUID directiveId;
    @NonNull
    public String requestId;
    @NonNull
    public Instant timestamp;
    public Integer percentage;
    @NonNull
    public DirectiveType type;
    public BigDecimal priceChange;
    public BigDecimal newPrice;
    public String currencyCode;
    @NonNull
    public String screenId;
    @NonNull
    public String lineItemId;
    @NonNull
    public String sspId;

    public boolean isValid() {
        return isCancellation() || !isCancellation() && percentage != null && newPrice != null && currencyCode != null;
    }

    public boolean isExploration() {
        return type.equals(DirectiveType.EXPLORATION);
    }

    public boolean isExploitation() {
        return type.equals(DirectiveType.EXPLOITATION);
    }

    public Money newPriceMoney() {
        return Money.of(newPrice, currencyCode);
    }

    public boolean isCancellation() {
        return type.equals(DirectiveType.EXPLORATION_CANCEL) || type.equals(DirectiveType.EXPLOITATION_CANCEL);
    }
}
