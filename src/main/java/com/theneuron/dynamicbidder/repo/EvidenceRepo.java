package com.theneuron.dynamicbidder.repo;

import com.theneuron.dynamicbidder.model.BidEvidence;
import com.theneuron.dynamicbidder.model.BidResultEvidence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EvidenceRepo {
    void Create(EvidenceRecord evidenceRecord) throws Exception;
    List<EvidenceRecord> Read(UUID screenId) throws Exception;
}
