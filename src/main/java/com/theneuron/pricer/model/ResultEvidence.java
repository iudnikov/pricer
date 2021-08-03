package com.theneuron.pricer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
@AllArgsConstructor
public final class ResultEvidence {

    public final UUID screenId;
    public final UUID lineItemId;
    public final String requestId;
    public final Instant timestamp;
    public final BidResultType bidResultType;

    public final boolean isWin() {
        return bidResultType.equals(BidResultType.WIN);
    }

    public final boolean isLose() {
        return bidResultType.equals(BidResultType.LOSE);
    }

}
