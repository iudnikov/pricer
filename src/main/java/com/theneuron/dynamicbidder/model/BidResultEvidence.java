package com.theneuron.dynamicbidder.model;

import lombok.AllArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
public final class BidResultEvidence {
    final UUID screenId;
    final UUID lineItemId;
    final String requestId;
    final Instant timestamp;
    final BidResultType bidResultType;
}
