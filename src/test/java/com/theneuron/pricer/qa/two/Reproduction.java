package com.theneuron.pricer.qa.two;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theneuron.pricer.config.AppConfigLocal;
import com.theneuron.pricer.model.messages.BidResponseMessage;
import com.theneuron.pricer.repo.CacheReader;
import com.theneuron.pricer.repo.CacheWriter;
import com.theneuron.pricer.repo.GuidelineReader;
import com.theneuron.pricer.repo.GuidelineWriter;
import com.theneuron.pricer.services.DirectivePublisher;
import com.theneuron.pricer.services.MoneyExchanger;
import com.theneuron.pricer.services.PricerService;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

import static org.mockito.Mockito.mock;

public class Reproduction {

    final ObjectMapper objectMapper = AppConfigLocal.objectMapper();
    final Supplier<UUID> uuidSupplier = UUID::randomUUID;
    final int maxWinsPercentage = 25;
    final int minCostsPercentage = 5;
    final Money priceChangeStep = Money.of(0.1, "USD");
    final MoneyExchanger moneyExchanger = (from, to) -> Money.of(from.getNumber(), to.getCurrencyCode());
    final Supplier<Instant> nowSupplier = Instant::now;

    @Test
    public void test_one() throws Exception {

        String bidResponse = "{\"type\":\"bid_response\",\"time\":1633070557.250000000,\"meta\":{\"ssp\":\"fssp\",\"request_id\":\"test-2320-5-test\",\"screen_id\":\"2399955\",\"campaign_ids\":[\"9d1d2422-d2f8-4cbb-8744-29d37c4b08e6\"],\"line_item_ids\":[\"5453d6f1-7705-47e9-bcdb-3019ad40a38d\"],\"impression_ids\":[\"1\"],\"bid_ids\":[\"1633070557247\"],\"prices\":[5.0],\"deal_ids\":[null],\"currencies\":[\"JOD\"],\"deal_bid_floors\":null,\"deal_bid_floor_curs\":null,\"imp_bid_floors\":[5.0],\"imp_bid_floor_curs\":[\"JOD\"],\"imp_multiplies\":[0.18333334,0.18333334],\"line_item_prices\":[9.803922],\"line_item_currencies\":[\"JOD\"],\"directive_ids\":[null]}}";
        BidResponseMessage message = objectMapper.readValue(bidResponse, BidResponseMessage.class);

        CacheReader cacheReader = mock(CacheReader.class);
        GuidelineWriter guidelineWriter = mock(GuidelineWriter.class);
        DirectivePublisher directivePublisher = mock(DirectivePublisher.class);
        CacheWriter cacheWriter = mock(CacheWriter.class);
        GuidelineReader guidelineReader = mock(GuidelineReader.class);
        PricerService pricerService = PricerService.builder()
                .objectMapper(objectMapper)
                .cacheReader(cacheReader)
                .cacheWriter(cacheWriter)
                .uuidSupplier(uuidSupplier)
                .nowSupplier(nowSupplier)
                .maxWinsPercentage(maxWinsPercentage)
                .minCostsPercentage(minCostsPercentage)
                .priceChangeStep(priceChangeStep)
                .moneyExchanger(moneyExchanger)
                .directivePublisher(directivePublisher)
                .guidelineReader(guidelineReader)
                .guidelineWriter(guidelineWriter)
                .build();

        pricerService.onBidResponseMessage(message);

    }

}
