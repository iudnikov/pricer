package com.theneuron.pricer.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EvidenceRepo {
    void create(EvidenceRecord evidenceRecord) throws Exception;
    List<EvidenceRecord> read(UUID screenId) throws Exception;
    List<EvidenceRecord> readByRequestId(String requestId) throws Exception;
}
