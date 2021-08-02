package com.theneuron.pricer.service;

import com.theneuron.pricer.model.BidResultEvidence;
import com.theneuron.pricer.repo.EvidenceRecord;
import com.theneuron.pricer.repo.EvidenceRepo;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class PricerService {

    private final EvidenceRepo evidenceRepo;

    public PricerService(EvidenceRepo evidenceRepo) {
        this.evidenceRepo = evidenceRepo;
    }

    public void onBidResultEvidence(BidResultEvidence bidResultEvidence) {
        try {
            List<EvidenceRecord> evidenceRecordList = evidenceRepo.readByRequestId(bidResultEvidence.requestId);
            System.out.println(evidenceRecordList.size());
        } catch (Exception e) {
            log.error("can't handle bid result", e);
        }

    }



}
