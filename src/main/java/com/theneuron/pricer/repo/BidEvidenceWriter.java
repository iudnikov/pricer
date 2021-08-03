package com.theneuron.pricer.repo;

import com.theneuron.pricer.model.BidEvidence;

import java.time.Instant;
import java.util.function.Consumer;

public class BidEvidenceWriter {
    public Consumer<StringBuffer> write(BidEvidence bidEvidence, Instant now) {
        return stringBuffer -> stringBuffer.append("insert into bid_evidence");
    }
}
