package com.theneuron.pricer.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theneuron.pricer.config.AppConfig;
import com.theneuron.pricer.model.*;
import com.theneuron.pricer.model.messages.WinNoticeMessage;
import com.theneuron.pricer.repo.CacheReader;
import com.theneuron.pricer.repo.CacheWriter;
import com.theneuron.pricer.repo.GuidelineReader;
import com.theneuron.pricer.repo.GuidelineWriter;
import org.apache.commons.lang3.RandomStringUtils;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PricerServiceMinCostsTest {

    final ObjectMapper objectMapper = AppConfig.objectMapper();
    final Supplier<UUID> uuidSupplier = UUID::randomUUID;
    final int maxWinsPercentage = 25;
    final int minCostsPercentage = 5;
    final Money priceIncreaseStep = Money.of(0.1, "USD");
    final MoneyExchanger moneyExchanger = (from, to) -> Money.of(from.getNumber(), to.getCurrencyCode());

    @Test
    public void creates_new_minimise_costs_guideline() throws Exception {

        CacheReader cacheReader = mock(CacheReader.class);
        GuidelineWriter guidelineWriter = mock(GuidelineWriter.class);
        DirectivePublisher directivePublisher = mock(DirectivePublisher.class);
        CacheWriter cacheWriter = mock(CacheWriter.class);
        GuidelineReader guidelineReader = mock(GuidelineReader.class);
        PricerService pricerService = new PricerService(
                objectMapper, cacheReader, uuidSupplier, maxWinsPercentage, minCostsPercentage, guidelineWriter, priceIncreaseStep, moneyExchanger, directivePublisher, cacheWriter, guidelineReader);

        final String requestId = RandomStringUtils.randomAlphanumeric(10);
        final String lineItem = "lineItem" + RandomStringUtils.randomNumeric(10);
        final String screenIn = "screenIn" + RandomStringUtils.randomNumeric(10);

        BidEvidence bidEvidence = BidEvidence.builder()
                .requestId(requestId)
                .lineItemId(lineItem)
                .screenId(screenIn)
                .minPrice(BigDecimal.valueOf(1))
                .actualPrice(BigDecimal.valueOf(1.7))
                .currencyCode("JOD")
                .build();

        CacheData cacheData = CacheData.builder()
                .bidEvidence(bidEvidence)
                .requestId(requestId)
                .build();

        when(cacheReader.read(eq(requestId))).thenReturn(Optional.of(cacheData));

        WinNoticeMessage winNoticeMessage = WinNoticeMessage.builder()
                .type("win_notice")
                .time(Instant.now())
                .meta(WinNoticeMessage.Meta.builder()
                        .requestId(requestId)
                        .build())
                .build();

        // when
        pricerService.onWinNoticeMessage(winNoticeMessage);

        // then
        ArgumentCaptor<Guideline> guidelineArgumentCaptor = ArgumentCaptor.forClass(Guideline.class);
        verify(guidelineWriter).write(guidelineArgumentCaptor.capture());
        Guideline guideline = guidelineArgumentCaptor.getValue();
        assertEquals(GuidelineType.MINIMISE_COSTS, guideline.guidelineType);
        assertEquals(GuidelineStatus.ACTIVE, guideline.status);

        ArgumentCaptor<Directive> directiveArgumentCaptor = ArgumentCaptor.forClass(Directive.class);
        verify(directivePublisher).publish(directiveArgumentCaptor.capture());
        Directive directive = directiveArgumentCaptor.getValue();
        assertEquals(DirectiveType.EXPLORATION, directive.type);
        assertEquals(minCostsPercentage, directive.percentage);
        assertEquals(lineItem, directive.lineItemId);
        assertEquals(screenIn, directive.screenId);
        assertEquals(BigDecimal.valueOf(1.6), directive.newPrice);

        ArgumentCaptor<CacheData> cacheDataArgumentCaptor = ArgumentCaptor.forClass(CacheData.class);
        verify(cacheWriter).write(cacheDataArgumentCaptor.capture());
        CacheData cacheDataCaptured = cacheDataArgumentCaptor.getValue();
        assertEquals(bidEvidence, cacheDataCaptured.getBidEvidence());
        assertEquals(guideline, cacheDataCaptured.getGuideline());

    }

    @Test
    public void create_new_exploitation_directive_for_existing_guideline_on_win_with_lower_price() throws Exception {

        CacheReader cacheReader = mock(CacheReader.class);
        GuidelineWriter guidelineWriter = mock(GuidelineWriter.class);
        DirectivePublisher directivePublisher = mock(DirectivePublisher.class);
        CacheWriter cacheWriter = mock(CacheWriter.class);
        GuidelineReader guidelineReader = mock(GuidelineReader.class);
        PricerService pricerService = new PricerService(
                objectMapper, cacheReader, uuidSupplier, maxWinsPercentage, minCostsPercentage, guidelineWriter, priceIncreaseStep, moneyExchanger, directivePublisher, cacheWriter, guidelineReader);

        final String requestId = RandomStringUtils.randomAlphanumeric(10);
        final String lineItemId = "lineItemId" + RandomStringUtils.randomNumeric(10);
        final String screenId = "screenId" + RandomStringUtils.randomNumeric(10);

        Guideline guideline = Guideline.builder()
                .guidelineType(GuidelineType.MINIMISE_COSTS)
                .status(GuidelineStatus.ACTIVE)
                .directive(Directive.builder()
                        .screenId(screenId)
                        .lineItemId(lineItemId)
                        .type(DirectiveType.EXPLORATION)
                        .directiveId(uuidSupplier.get())
                        .requestId(requestId)
                        .build())
                .build();

        BidEvidence bidEvidence = BidEvidence.builder()
                .requestId(requestId)
                .lineItemId(lineItemId)
                .screenId(screenId)
                .minPrice(BigDecimal.valueOf(1))
                .actualPrice(BigDecimal.valueOf(1.6))
                .directiveId(Optional.of(UUID.randomUUID()))
                .currencyCode("JOD")
                .build();

        CacheData cacheData = CacheData.builder()
                .bidEvidence(bidEvidence)
                .requestId(requestId)
                .guideline(guideline)
                .build();


        when(cacheReader.read(eq(requestId))).thenReturn(Optional.of(cacheData));

        WinNoticeMessage winNoticeMessage = WinNoticeMessage.builder()
                .type("win_notice")
                .time(Instant.now())
                .meta(WinNoticeMessage.Meta.builder()
                        .requestId(requestId)
                        .build())
                .build();



        // when
        pricerService.onWinNoticeMessage(winNoticeMessage);

        ArgumentCaptor<Directive> directiveArgumentCaptor = ArgumentCaptor.forClass(Directive.class);
        verify(directivePublisher, times(2)).publish(directiveArgumentCaptor.capture());

        Map<DirectiveType, List<Directive>> grouped = directiveArgumentCaptor
                .getAllValues()
                .stream()
                .collect(Collectors.groupingBy((directive) -> directive.type));

        Directive lor = grouped.get(DirectiveType.EXPLORATION).get(0);
        Directive loi = grouped.get(DirectiveType.EXPLOITATION).get(0);

        assertNull(loi.priceChange);
        assertEquals(bidEvidence.actualPrice, loi.newPrice);

        assertNotNull(lor.priceChange);
        assertEquals(bidEvidence.actualPrice.subtract(BigDecimal.valueOf(0.1)), lor.newPrice);

        ArgumentCaptor<Guideline> guidelineArgumentCaptor = ArgumentCaptor.forClass(Guideline.class);

        verify(guidelineWriter).write(guidelineArgumentCaptor.capture());

    }

    @Test
    public void cancel_exploration_when_received_win_but_no_capacity_for_price_reduce_exists() throws Exception {

        CacheReader cacheReader = mock(CacheReader.class);
        GuidelineWriter guidelineWriter = mock(GuidelineWriter.class);
        DirectivePublisher directivePublisher = mock(DirectivePublisher.class);
        CacheWriter cacheWriter = mock(CacheWriter.class);
        GuidelineReader guidelineReader = mock(GuidelineReader.class);
        PricerService pricerService = new PricerService(
                objectMapper, cacheReader, uuidSupplier, maxWinsPercentage, minCostsPercentage, guidelineWriter, priceIncreaseStep, moneyExchanger, directivePublisher, cacheWriter, guidelineReader);

        final String requestId = RandomStringUtils.randomAlphanumeric(10);
        final String lineItemId = "lineItemId" + RandomStringUtils.randomNumeric(10);
        final String screenId = "screenId" + RandomStringUtils.randomNumeric(10);

        Guideline guideline = Guideline.builder()
                .guidelineType(GuidelineType.MINIMISE_COSTS)
                .status(GuidelineStatus.ACTIVE)
                .directive(Directive.builder()
                        .screenId(screenId)
                        .lineItemId(lineItemId)
                        .type(DirectiveType.EXPLORATION)
                        .directiveId(uuidSupplier.get())
                        .requestId(requestId)
                        .build())
                .build();

        BidEvidence bidEvidence = BidEvidence.builder()
                .requestId(requestId)
                .lineItemId(lineItemId)
                .screenId(screenId)
                .minPrice(BigDecimal.valueOf(1))
                .actualPrice(BigDecimal.valueOf(1))
                .directiveId(Optional.of(UUID.randomUUID()))
                .currencyCode("JOD")
                .build();

        CacheData cacheData = CacheData.builder()
                .bidEvidence(bidEvidence)
                .requestId(requestId)
                .guideline(guideline)
                .build();


        when(cacheReader.read(eq(requestId))).thenReturn(Optional.of(cacheData));

        WinNoticeMessage winNoticeMessage = WinNoticeMessage.builder()
                .type("win_notice")
                .time(Instant.now())
                .meta(WinNoticeMessage.Meta.builder()
                        .requestId(requestId)
                        .build())
                .build();

        // when
        pricerService.onWinNoticeMessage(winNoticeMessage);

        // then
        ArgumentCaptor<Guideline> guidelineArgumentCaptor = ArgumentCaptor.forClass(Guideline.class);
        verify(guidelineWriter).write(guidelineArgumentCaptor.capture());
        Guideline guidelineCaptured = guidelineArgumentCaptor.getValue();
        assertEquals(GuidelineType.MINIMISE_COSTS, guidelineCaptured.guidelineType);
        assertEquals(GuidelineStatus.CANCELLED, guidelineCaptured.status);

        ArgumentCaptor<Directive> directiveArgumentCaptor = ArgumentCaptor.forClass(Directive.class);
        verify(directivePublisher).publish(directiveArgumentCaptor.capture());
        Directive directive = directiveArgumentCaptor.getValue();
        assertEquals(DirectiveType.EXPLORATION_CANCEL, directive.type);
        assertEquals(lineItemId, directive.lineItemId);
        assertEquals(screenId, directive.screenId);
        assertNull(directive.newPrice);

    }

    @Test
    public void do_not_start_min_costs_when_max_wins_exists() throws Exception {

        CacheReader cacheReader = mock(CacheReader.class);
        GuidelineWriter guidelineWriter = mock(GuidelineWriter.class);
        DirectivePublisher directivePublisher = mock(DirectivePublisher.class);
        CacheWriter cacheWriter = mock(CacheWriter.class);
        GuidelineReader guidelineReader = mock(GuidelineReader.class);
        PricerService pricerService = new PricerService(
                objectMapper, cacheReader, uuidSupplier, maxWinsPercentage, minCostsPercentage, guidelineWriter, priceIncreaseStep, moneyExchanger, directivePublisher, cacheWriter, guidelineReader);

        final String requestId = RandomStringUtils.randomAlphanumeric(10);
        final String lineItemId = "lineItemId" + RandomStringUtils.randomNumeric(10);
        final String screenId = "screenId" + RandomStringUtils.randomNumeric(10);

        Guideline guideline = Guideline.builder()
                .guidelineType(GuidelineType.MAXIMISE_WINS)
                .status(GuidelineStatus.ACTIVE)
                .directive(Directive.builder()
                        .screenId(screenId)
                        .lineItemId(lineItemId)
                        .type(DirectiveType.EXPLORATION)
                        .directiveId(uuidSupplier.get())
                        .requestId(requestId)
                        .build())
                .build();

        BidEvidence bidEvidence = BidEvidence.builder()
                .requestId(requestId)
                .lineItemId(lineItemId)
                .screenId(screenId)
                .minPrice(BigDecimal.valueOf(1))
                .actualPrice(BigDecimal.valueOf(1.3))
                .currencyCode("JOD")
                .build();

        CacheData cacheData = CacheData.builder()
                .bidEvidence(bidEvidence)
                .requestId(requestId)
                .build();


        when(cacheReader.read(eq(requestId))).thenReturn(Optional.of(cacheData));
        when(guidelineReader.read(eq(lineItemId), eq(screenId))).thenReturn(Optional.of(guideline));

        WinNoticeMessage winNoticeMessage = WinNoticeMessage.builder()
                .type("win_notice")
                .time(Instant.now())
                .meta(WinNoticeMessage.Meta.builder()
                        .requestId(requestId)
                        .build())
                .build();

        // when
        pricerService.onWinNoticeMessage(winNoticeMessage);

        // then
        ArgumentCaptor<Guideline> guidelineArgumentCaptor = ArgumentCaptor.forClass(Guideline.class);
        verify(guidelineWriter).write(guidelineArgumentCaptor.capture());
        Guideline guidelineCaptured = guidelineArgumentCaptor.getValue();
        assertEquals(GuidelineType.MAXIMISE_WINS, guidelineCaptured.guidelineType);
        assertEquals(GuidelineStatus.COMPLETE, guidelineCaptured.status);

    }

}
