package com.theneuron.pricer.service;

import com.theneuron.pricer.model.BidEvidence;
import com.theneuron.pricer.model.BidResultEvidence;
import com.theneuron.pricer.model.BidResultType;
import com.theneuron.pricer.repo.EvidenceRecord;
import com.theneuron.pricer.repo.EvidenceRepo;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

class PricerServiceTest {

    // it should produce exploration price directive when bid wins
    @Test
    public void testOne() throws Exception {

        /*
        on bid result define strategy
        strategy.act()
            TryToWinStrategy
                issue price directives
            MinimizeCostStrategy
                issue price directives
            */

        System.out.println("hello");

        UUID screenOne = UUID.randomUUID();
        UUID lineItemOne = UUID.randomUUID();
        Instant now = Instant.now();
        String requestId = "requestOne";

        BidEvidence bidOne = new BidEvidence(screenOne, lineItemOne, requestId, now, BigDecimal.valueOf(20.00), BigDecimal.valueOf(1000.00), BigDecimal.valueOf(10.00), null);
        BidResultEvidence bidResultEvidence = new BidResultEvidence(screenOne, lineItemOne, requestId, now.plusSeconds(1), BidResultType.LOSE);

        EvidenceRepo evidenceRepo = Mockito.mock(EvidenceRepo.class);
        Mockito.when(evidenceRepo.readByRequestId(requestId)).thenReturn(Lists.list(EvidenceRecord.create(bidOne)));

        PricerService pricerService = new PricerService(evidenceRepo);

        pricerService.onBidResultEvidence(bidResultEvidence);

    }

}