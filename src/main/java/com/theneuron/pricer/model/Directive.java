package com.theneuron.pricer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
@Builder(toBuilder = true)
@Value
public class Directive {
    public UUID directiveId;
    public String requestId;
    public Instant timestamp;
    public Integer percentage;
    public DirectiveType type;
    public BigDecimal priceChange;
    public BigDecimal newPrice;
    public String currencyCode;
    @NonNull
    public String screenId;
    @NonNull
    public String lineItemId;
}
