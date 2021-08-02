package com.theneuron.pricer.model;

import lombok.AllArgsConstructor;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
public final class BidEvidence {
    public final UUID screenId;
    public final UUID lineItemId;
    public final String requestId;
    public final Instant timestamp;
    public final BigDecimal bidPrice;
    public final BigDecimal remainingBudget;
    public final BigDecimal floorPrice;
    @Nullable
    public final UUID priceDirective;
}
