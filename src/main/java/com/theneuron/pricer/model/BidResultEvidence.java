package com.theneuron.pricer.model;

import lombok.AllArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
public final class BidResultEvidence {
    public final UUID screenId;
    public final UUID lineItemId;
    public final String requestId;
    public final Instant timestamp;
    public final BidResultType bidResultType;
}
