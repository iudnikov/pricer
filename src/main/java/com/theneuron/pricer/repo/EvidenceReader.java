package com.theneuron.pricer.repo;

import com.theneuron.pricer.model.BidEvidence;

import java.util.Optional;
import java.util.UUID;

public interface EvidenceReader {
    Optional<BidEvidence> readById(UUID id);
    Optional<BidEvidence> readByRequestId(String requestId);
}
