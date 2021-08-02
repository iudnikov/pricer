package com.theneuron.dynamicbidder.repo;

import com.theneuron.dynamicbidder.model.BidEvidence;
import com.theneuron.dynamicbidder.model.BidResultEvidence;
import lombok.AllArgsConstructor;
import org.springframework.lang.Nullable;

import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
public final class EvidenceRecord {
    final UUID screenId;
    final UUID campaignId;
    final UUID lineItemId;
    final Instant createdAt;
    @Nullable
    final BidEvidence bidEvidence;
    @Nullable
    final BidResultEvidence bidResultEvidence;
}
