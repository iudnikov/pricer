package com.theneuron.pricer.repo;

import com.theneuron.pricer.model.BidEvidence;
import com.theneuron.pricer.model.BidResultEvidence;
import lombok.AllArgsConstructor;
import org.springframework.lang.Nullable;

import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
public final class EvidenceRecord {
    public final UUID screenId;
    public final UUID lineItemId;
    public final Instant createdAt;
    public final EvidenceRecordType evidenceRecordType;
    @Nullable
    public final BidEvidence bidEvidence;
    @Nullable
    public final BidResultEvidence bidResultEvidence;

    public static EvidenceRecord create(BidEvidence bidEvidence) {
        return new EvidenceRecord(bidEvidence.screenId, bidEvidence.lineItemId, bidEvidence.timestamp, EvidenceRecordType.BID_EVIDENCE, bidEvidence, null);
    }
}
