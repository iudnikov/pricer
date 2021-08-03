package com.theneuron.pricer.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theneuron.pricer.config.AppConfig;
import com.theneuron.pricer.model.*;
import com.theneuron.pricer.model.messages.BidResponseMessage;
import com.theneuron.pricer.model.messages.LossNoticeMessage;
import com.theneuron.pricer.model.messages.WinNoticeMessage;
import com.theneuron.pricer.repo.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;

import javax.money.Monetary;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.theneuron.pricer.model.DirectiveType.*;
import static com.theneuron.pricer.services.PricerService.getPriceIncreaseStep;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;


class PricerServiceMaxWinsTest {

    final Money priceIncreaseStep = Money.of(0.1, "USD");
    final Integer maxWinsPercentage = 25;
    final Integer minCostsPercentage = 10;
    final MoneyExchanger moneyExchanger = mock(MoneyExchanger.class);
    final ObjectMapper objectMapper = AppConfig.objectMapper();

    @Test
    public void creates_new_guideline_publishes_exploration_directive() throws Exception {

        GuidelineWriter guidelineWriter = mock(GuidelineWriter.class);
        DirectivePublisher directivePublisher = mock(DirectivePublisher.class);
        CacheWriter cacheWriter = mock(CacheWriter.class);
        GuidelineReader guidelineReader = mock(GuidelineReader.class);
        Money priceIncreaseStep = Money.of(0.1, "USD");


        CacheReader cacheReader = mock(CacheReader.class);
        PricerService pricerService = new PricerService(
                AppConfig.objectMapper(), cacheReader, UUID::randomUUID, maxWinsPercentage, minCostsPercentage, guidelineWriter, priceIncreaseStep, moneyExchanger, directivePublisher, cacheWriter, guidelineReader);

        String requestId = "one";
        String lineItemId = RandomStringUtils.randomAlphanumeric(10);
        String screenId = RandomStringUtils.randomNumeric(10);
        BidEvidence bidEvidence = BidEvidence.builder()
                .requestId(requestId)
                .lineItemId(lineItemId)
                .screenId(screenId)
                .minPrice(BigDecimal.valueOf(3))
                .actualPrice(BigDecimal.valueOf(5))
                .maxPrice(BigDecimal.valueOf(10))
                .currencyCode("USD")
                .build();

        CacheData cacheData = CacheData.builder()
                .bidEvidence(bidEvidence)
                .build();

        LossNoticeMessage lossNoticeMessage = LossNoticeMessage.builder()
                .type("loss_notice")
                .time(Instant.now())
                .meta(LossNoticeMessage.Meta.builder()
                        .requestId(requestId)
                        .build())
                .build();

        System.out.println(objectMapper.writeValueAsString(lossNoticeMessage));

        when(cacheReader.read(requestId)).thenReturn(Optional.of(cacheData));

        // when
        pricerService.onLossNoticeMessage(lossNoticeMessage);

        ArgumentCaptor<Guideline> guidelineArgumentCaptor = ArgumentCaptor.forClass(Guideline.class);

        verify(guidelineWriter).write(guidelineArgumentCaptor.capture());

        List<Directive> directives = guidelineArgumentCaptor.getValue().directives;
        assertEquals(1, directives.size());
        Directive directive = directives.get(0);
        assertEquals(BigDecimal.valueOf(5.1), directive.newPrice);
        assertEquals(25, directive.percentage);
        assertEquals(DirectiveType.EXPLORATION, directive.type);

        ArgumentCaptor<Directive> directiveArgumentCaptor = ArgumentCaptor.forClass(Directive.class);
        verify(directivePublisher).publish(directiveArgumentCaptor.capture());
        Directive publishedDirective = directiveArgumentCaptor.getValue();
        assertEquals(directive, publishedDirective);

    }

    @Test
    public void cancels_existing_guideline_and_publishes_cancellation_directive_when_no_previous_exploitation_directive_exist_and_price_max_reached() throws Exception {

        GuidelineWriter guidelineWriter = mock(GuidelineWriter.class);
        BidEvidenceWriter bidEvidenceWriter = mock(BidEvidenceWriter.class);
        DirectivePublisher directivePublisher = mock(DirectivePublisher.class);
        CacheReader cacheReader = mock(CacheReader.class);
        CacheWriter cacheWriter = mock(CacheWriter.class);
        GuidelineReader guidelineReader = mock(GuidelineReader.class);


        PricerService pricerService = new PricerService(
                AppConfig.objectMapper(), cacheReader, UUID::randomUUID, maxWinsPercentage, minCostsPercentage, guidelineWriter, priceIncreaseStep, moneyExchanger, directivePublisher, cacheWriter, guidelineReader);

        String requestId = "one";
        String lineItemId = RandomStringUtils.randomAlphanumeric(10);
        String screenId = RandomStringUtils.randomNumeric(10);

        BidEvidence bidEvidenceOne = BidEvidence.builder()
                .requestId(requestId)
                .lineItemId(lineItemId)
                .screenId(screenId)
                .minPrice(BigDecimal.valueOf(3))
                .actualPrice(BigDecimal.valueOf(10))
                .maxPrice(BigDecimal.valueOf(10))
                .currencyCode("USD")
                .build();

        Guideline existingGuideline = Guideline.builder()
                .status(GuidelineStatus.ACTIVE)
                .guidelineType(GuidelineType.MAXIMISE_WINS)
                .directive(Directive.builder()
                        .type(DirectiveType.EXPLORATION)
                        .build())
                .build();

        CacheData cacheData = CacheData.builder()
                .bidEvidence(bidEvidenceOne)
                .guideline(existingGuideline)
                .build();

        LossNoticeMessage lossNoticeMessage = LossNoticeMessage.builder()
                .type("loss_notice")
                .time(Instant.now())
                .meta(LossNoticeMessage.Meta.builder()
                        .requestId(requestId)
                        .build())
                .build();

        when(cacheReader.read(requestId)).thenReturn(Optional.of(cacheData));

        // when
        pricerService.onLossNoticeMessage(lossNoticeMessage);

        ArgumentCaptor<Guideline> guidelineArgumentCaptor = ArgumentCaptor.forClass(Guideline.class);

        verify(guidelineWriter).write(guidelineArgumentCaptor.capture());

        List<Directive> directives = guidelineArgumentCaptor.getValue().directives;
        assertEquals(2, directives.size());
        Directive directive = directives.get(1);
        assertEquals(EXPLORATION_CANCEL, directive.getType());

        ArgumentCaptor<Directive> directiveArgumentCaptor = ArgumentCaptor.forClass(Directive.class);
        verify(directivePublisher).publish(directiveArgumentCaptor.capture());
        Directive publishedDirective = directiveArgumentCaptor.getValue();
        assertEquals(directive, publishedDirective);

    }

    @Test
    public void cancels_only_latest_exploration_directive_when_price_max_reached() throws Exception {

        GuidelineWriter guidelineWriter = mock(GuidelineWriter.class);
        BidEvidenceWriter bidEvidenceWriter = mock(BidEvidenceWriter.class);
        DirectivePublisher directivePublisher = mock(DirectivePublisher.class);
        CacheReader cacheReader = mock(CacheReader.class);
        CacheWriter cacheWriter = mock(CacheWriter.class);
        GuidelineReader guidelineReader = mock(GuidelineReader.class);


        PricerService pricerService = new PricerService(
                AppConfig.objectMapper(), cacheReader, UUID::randomUUID, maxWinsPercentage, minCostsPercentage, guidelineWriter, priceIncreaseStep, moneyExchanger, directivePublisher, cacheWriter, guidelineReader);

        String requestId = "one";
        String lineItemId = RandomStringUtils.randomAlphanumeric(10);
        String screenId = RandomStringUtils.randomNumeric(10);

        Guideline existingGuideline = Guideline.builder()
                .status(GuidelineStatus.ACTIVE)
                .guidelineType(GuidelineType.MAXIMISE_WINS)
                .directive(Directive.builder()
                        .type(DirectiveType.EXPLORATION)
                        .build())
                .directive(Directive.builder()
                        .type(DirectiveType.EXPLOITATION)
                        .build())
                .directive(Directive.builder()
                        .type(DirectiveType.EXPLORATION)
                        .build())
                .build();

        BidEvidence bidEvidenceOne = BidEvidence.builder()
                .requestId(requestId)
                .lineItemId(lineItemId)
                .screenId(screenId)
                .minPrice(BigDecimal.valueOf(3))
                .actualPrice(BigDecimal.valueOf(10))
                .maxPrice(BigDecimal.valueOf(10))
                .currencyCode("USD")
                .build();

        LossNoticeMessage lossNoticeMessage = LossNoticeMessage.builder()
                .type("loss_notice")
                .time(Instant.now())
                .meta(LossNoticeMessage.Meta.builder()
                        .requestId(requestId)
                        .build())
                .build();

        CacheData cacheData = CacheData.builder()
                .guideline(existingGuideline)
                .bidEvidence(bidEvidenceOne)
                .build();

        when(cacheReader.read(requestId)).thenReturn(Optional.of(cacheData));

        // when
        pricerService.onLossNoticeMessage(lossNoticeMessage);

        ArgumentCaptor<Guideline> guidelineArgumentCaptor = ArgumentCaptor.forClass(Guideline.class);
        verify(guidelineWriter).write(guidelineArgumentCaptor.capture());

        Guideline guideline = guidelineArgumentCaptor.getValue();
        assertEquals(GuidelineStatus.ACTIVE, guideline.status);

        assertEquals(4, guideline.directives.size());
        Directive latestDirective = guideline.directives.get(3);
        assertEquals(EXPLORATION_CANCEL, latestDirective.getType());

        ArgumentCaptor<Directive> directiveArgumentCaptor = ArgumentCaptor.forClass(Directive.class);
        verify(directivePublisher).publish(directiveArgumentCaptor.capture());
        Directive publishedDirective = directiveArgumentCaptor.getValue();
        assertEquals(latestDirective, publishedDirective);

    }

    @Test
    public void issues_exploitation_directive_when_win_received_and_exploration_directive_exists() throws Exception {

        GuidelineWriter guidelineWriter = mock(GuidelineWriter.class);
        BidEvidenceWriter bidEvidenceWriter = mock(BidEvidenceWriter.class);
        DirectivePublisher directivePublisher = mock(DirectivePublisher.class);
        CacheReader cacheReader = mock(CacheReader.class);
        CacheWriter cacheWriter = mock(CacheWriter.class);
        GuidelineReader guidelineReader = mock(GuidelineReader.class);


        PricerService pricerService = new PricerService(
                AppConfig.objectMapper(), cacheReader, UUID::randomUUID, maxWinsPercentage, minCostsPercentage, guidelineWriter, priceIncreaseStep, moneyExchanger, directivePublisher, cacheWriter, guidelineReader);

        String requestId = "one";
        String lineItemId = RandomStringUtils.randomAlphanumeric(10);
        String screenId = RandomStringUtils.randomNumeric(10);
        UUID directiveId = UUID.randomUUID();

        Directive exploration = Directive.builder()
                .directiveId(directiveId)
                .type(EXPLORATION)
                .build();

        Guideline existingGuideline = Guideline.builder()
                .status(GuidelineStatus.ACTIVE)
                .guidelineType(GuidelineType.MAXIMISE_WINS)
                .directive(exploration)
                .build();

        BidEvidence bidEvidence = BidEvidence.builder()
                .screenId(screenId)
                .requestId(requestId)
                .lineItemId(lineItemId)
                .directiveId(Optional.of(directiveId))
                .build();

        WinNoticeMessage winNoticeMessage = WinNoticeMessage.builder()
                .type("win_notice")
                .time(Instant.now())
                .meta(WinNoticeMessage.Meta.builder()
                        .directiveId(Optional.of(directiveId))
                        .requestId(requestId)
                        .build())
                .build();

        CacheData cacheData = CacheData.builder()
                .bidEvidence(bidEvidence)
                .guideline(existingGuideline)
                .build();

        when(cacheReader.read(requestId)).thenReturn(Optional.of(cacheData));

        // when
        pricerService.onWinNoticeMessage(winNoticeMessage);

        ArgumentCaptor<Guideline> guidelineArgumentCaptor = ArgumentCaptor.forClass(Guideline.class);
        verify(guidelineWriter).write(guidelineArgumentCaptor.capture());
        Guideline guideline = guidelineArgumentCaptor.getValue();

        assertEquals(GuidelineStatus.ACTIVE, guideline.status);
        assertEquals(2, guideline.directives.size());
        Directive latestDirective = guideline.directives.get(1);
        assertEquals(EXPLOITATION, latestDirective.getType());

        ArgumentCaptor<Directive> directiveArgumentCaptor = ArgumentCaptor.forClass(Directive.class);
        verify(directivePublisher).publish(directiveArgumentCaptor.capture());
        Directive publishedDirective = directiveArgumentCaptor.getValue();
        assertEquals(latestDirective, publishedDirective);

    }

    @Test
    public void cancel_all_directives_when_received_bid_evidence_with_decreased_max_price_lower_than_latest_actual_price() throws Exception {

        GuidelineWriter guidelineWriter = mock(GuidelineWriter.class);
        BidEvidenceWriter bidEvidenceWriter = mock(BidEvidenceWriter.class);
        DirectivePublisher directivePublisher = mock(DirectivePublisher.class);
        CacheReader cacheReader = mock(CacheReader.class);
        CacheWriter cacheWriter = mock(CacheWriter.class);
        GuidelineReader guidelineReader = mock(GuidelineReader.class);


        PricerService pricerService = new PricerService(
                AppConfig.objectMapper(), cacheReader, UUID::randomUUID, maxWinsPercentage, minCostsPercentage, guidelineWriter, priceIncreaseStep, moneyExchanger, directivePublisher, cacheWriter, guidelineReader);

        String requestId = "one";
        String lineItemId = RandomStringUtils.randomAlphanumeric(10);
        String screenId = RandomStringUtils.randomNumeric(10);

        Guideline existingGuideline = Guideline.builder()
                .status(GuidelineStatus.ACTIVE)
                .guidelineType(GuidelineType.MAXIMISE_WINS)
                .directive(Directive.builder()
                        .directiveId(UUID.randomUUID())
                        .type(EXPLORATION)
                        .build())
                .directive(Directive.builder()
                        .directiveId(UUID.randomUUID())
                        .type(EXPLOITATION)
                        .build())
                .build();

        BidResponseMessage bidResponseMessage = BidResponseMessage.builder()
                .type("bid_response")
                .time(Instant.now())
                .meta(BidResponseMessage.Meta.builder()
                        .bidId("some bid id")
                        .requestId(requestId)
                        .screenId(screenId)
                        .lineItemId(lineItemId.toString())
                        .dealBidFloor(1.0)
                        .impBidFloor(1.0)
                        .price(4.0) // price is greater than lineItemPrice
                        .lineItemPrice(3.0)
                        .lineItemCurrency("USD")
                        .build())
                .build();

        when(guidelineReader.read(lineItemId, screenId)).thenReturn(Optional.of(existingGuideline));

        // when
        pricerService.onBidResponseMessage(bidResponseMessage);

        ArgumentCaptor<Guideline> guidelineArgumentCaptor = ArgumentCaptor.forClass(Guideline.class);
        verify(guidelineWriter).write(guidelineArgumentCaptor.capture());
        Guideline guideline = guidelineArgumentCaptor.getValue();

        assertEquals(GuidelineStatus.CANCELLED, guideline.status);
        assertEquals(4, guideline.directives.size());
        Directive exploitationDirectiveCancel = guideline.directives.get(2);
        assertEquals(EXPLOITATION_CANCEL, exploitationDirectiveCancel.getType());
        Directive explorationDirectiveCancel = guideline.directives.get(3);
        assertEquals(EXPLORATION_CANCEL, explorationDirectiveCancel.getType());

        ArgumentCaptor<Directive> directiveArgumentCaptor = ArgumentCaptor.forClass(Directive.class);
        verify(directivePublisher, times(2)).publish(directiveArgumentCaptor.capture());
        List<Directive> publishedDirectives = directiveArgumentCaptor.getAllValues();
        assertTrue(publishedDirectives.stream().anyMatch(directive -> directive.equals(exploitationDirectiveCancel)));
        assertTrue(publishedDirectives.stream().anyMatch(directive -> directive.equals(explorationDirectiveCancel)));

    }

    @Test
    public void link_bid_evidence_to_existing_guideline_by_screenId_and_lineItemId() throws Exception {

        GuidelineWriter guidelineWriter = mock(GuidelineWriter.class);
        BidEvidenceWriter bidEvidenceWriter = mock(BidEvidenceWriter.class);
        DirectivePublisher directivePublisher = mock(DirectivePublisher.class);
        CacheReader cacheReader = mock(CacheReader.class);
        CacheWriter cacheWriter = mock(CacheWriter.class);
        GuidelineReader guidelineReader = mock(GuidelineReader.class);


        PricerService pricerService = new PricerService(
                AppConfig.objectMapper(), cacheReader, UUID::randomUUID, maxWinsPercentage, minCostsPercentage, guidelineWriter, priceIncreaseStep, moneyExchanger, directivePublisher, cacheWriter, guidelineReader);

        String requestTwo = "two";
        String lineItemId = RandomStringUtils.randomAlphanumeric(10);
        String screenId = RandomStringUtils.randomNumeric(10);

        BidResponseMessage bidResponseMessage = BidResponseMessage.builder()
                .type("bid_response")
                .time(Instant.now())
                .meta(BidResponseMessage.Meta.builder()
                        .bidId("some bid id")
                        .requestId(requestTwo)
                        .screenId(screenId)
                        .lineItemId(lineItemId.toString())
                        .dealBidFloor(1.0)
                        .impBidFloor(1.0)
                        .price(1.0)
                        .lineItemPrice(3.0)
                        .lineItemCurrency("USD")
                        .build())
                .build();

        BidEvidence bidEvidence = PricerService.bidEvidence(bidResponseMessage, 0);

        // when
        pricerService.onBidResponseMessage(bidResponseMessage);

        ArgumentCaptor<CacheData> cacheDataArgumentCaptor = ArgumentCaptor.forClass(CacheData.class);

        verify(cacheWriter).write(cacheDataArgumentCaptor.capture());

        CacheData cacheData = cacheDataArgumentCaptor.getValue();

        assertEquals(bidEvidence, cacheData.getBidEvidence());
        assertEquals(requestTwo, cacheData.getBidEvidence().requestId);

    }

    @Test
    public void test_price_increase_step() throws Exception {

        Money oneJOD = Money.of(1, "JOD");
        Money step = Money.of(0.1, "USD");
        MoneyExchanger exchanger = mock(MoneyExchanger.class);

        when(exchanger.exchange(eq(step), eq(Monetary.getCurrency("JOD"))))
                .thenReturn(Money.of(0.07, "JOD"));

        Money result = getPriceIncreaseStep(oneJOD, step, exchanger);

        assertEquals(Money.of(0.07, "JOD"), result);

    }

}