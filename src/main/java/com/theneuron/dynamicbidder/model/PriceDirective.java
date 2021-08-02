package com.theneuron.dynamicbidder.model;

import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// Outgoing message
@AllArgsConstructor
public final class PriceDirective {
    final UUID priceDirectiveId;
    final UUID screenId;
    final UUID lineItemId;
    final String requestId;
    final Instant timestamp;

}
