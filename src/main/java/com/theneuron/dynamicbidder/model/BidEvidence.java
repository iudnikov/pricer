package com.theneuron.dynamicbidder.model;

import lombok.AllArgsConstructor;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
public final class BidEvidence {
    final UUID screenId;
    final UUID lineItemId;
    final String requestId;
    final Instant timestamp;
    final BigDecimal bidPrice;
    final BigDecimal remainingBudget;
    final BigDecimal floorPrice;
    @Nullable
    final UUID priceDirective;
}
