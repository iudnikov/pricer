package com.theneuron.pricer.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theneuron.pricer.config.AppConfigLocal;
import com.theneuron.pricer.model.*;
import com.theneuron.pricer.model.messages.LossNoticeMessage;
import com.theneuron.pricer.model.messages.WinNoticeMessage;
import com.theneuron.pricer.repo.CacheReader;
import com.theneuron.pricer.repo.CacheWriter;
import com.theneuron.pricer.repo.GuidelineReader;
import com.theneuron.pricer.repo.GuidelineWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
public class PricerServiceMinCostsTest {

    final ObjectMapper objectMapper = AppConfigLocal.objectMapper();
    final Supplier<UUID> uuidSupplier = UUID::randomUUID;
    final Supplier<Instant> nowSupplier = Instant::now;
    final int maxWinsPercentage = 25;
    final int minCostsPercentage = 5;
    final Money priceChangeStep = Money.of(0.1, "USD");
    final MoneyExchanger moneyExchanger = (from, to) -> Money.of(from.getNumber(), to.getCurrencyCode());

    @Test
    public void should_create_new_minimise_costs_guideline() throws Exception {

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

        final String requestId = RandomStringUtils.randomAlphanumeric(10);
        final String lineItemId = "lineItemId" + RandomStringUtils.randomNumeric(10);
        final String screenId = "screenId" + RandomStringUtils.randomNumeric(10);
        final String sspId = "sspId" + RandomStringUtils.randomNumeric(10);

        BidEvidence bidEvidence = BidEvidence.builder()
                .requestId(requestId)
                .lineItemId(lineItemId)
                .screenId(screenId)
                .sspId(sspId)
                .minPrice(BigDecimal.valueOf(1))
                .maxPrice(BigDecimal.valueOf(3))
                .actualPrice(BigDecimal.valueOf(1.7))
                .currencyCode("JOD")
                .timestamp(Instant.now())
                .build();

        CacheData cacheData = CacheData.builder()
                .bidEvidence(bidEvidence)
                .requestId(requestId)
                .build();

        when(cacheReader.read(eq(requestId))).thenReturn(Optional.of(cacheData));
        //when(guidelineReader.read(eq(lineItemId), eq(screenId), eq(sspId))).thenReturn(Optional.of(guideline));

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
        assertEquals(lineItemId, directive.lineItemId);
        assertEquals(screenId, directive.screenId);
        assertEquals(sspId, directive.sspId);
        assertEquals(BigDecimal.valueOf(1.6), directive.newPrice);

    }

    @Test
    public void should_create_new_exploration_and_new_exploitation_directive_for_existing_guideline_on_win_with_lower_price() throws Exception {

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

        final String requestId = RandomStringUtils.randomAlphanumeric(10);
        final String lineItemId = "lineItemId" + RandomStringUtils.randomNumeric(10);
        final String screenId = "screenId" + RandomStringUtils.randomNumeric(10);
        final String sspId = "sspId" + RandomStringUtils.randomNumeric(10);

        UUID directiveId = UUID.randomUUID();

        Guideline guideline = Guideline.builder()
                .guidelineType(GuidelineType.MINIMISE_COSTS)
                .status(GuidelineStatus.ACTIVE)
                .directive(Directive.builder()
                        .screenId(screenId)
                        .sspId(sspId)
                        .lineItemId(lineItemId)
                        .type(DirectiveType.EXPLORATION)
                        .percentage(25)
                        .newPrice(BigDecimal.valueOf(1.6))
                        .priceChange(BigDecimal.valueOf(1.5))
                        .directiveId(directiveId)
                        .requestId(requestId)
                        .timestamp(Instant.now())
                        .build())
                .build();

        BidEvidence bidEvidence = BidEvidence.builder()
                .requestId(requestId)
                .lineItemId(lineItemId)
                .screenId(screenId)
                .sspId(sspId)
                .minPrice(BigDecimal.valueOf(1))
                .maxPrice(BigDecimal.valueOf(3))
                .actualPrice(BigDecimal.valueOf(1.6))
                .directiveId(Optional.of(directiveId))
                .currencyCode("JOD")
                .timestamp(Instant.now())
                .build();

        CacheData cacheData = CacheData.builder()
                .bidEvidence(bidEvidence)
                .requestId(requestId)
                .build();


        when(cacheReader.read(eq(requestId))).thenReturn(Optional.of(cacheData));
        when(guidelineReader.read(eq(lineItemId), eq(screenId), eq(sspId))).thenReturn(Optional.of(guideline));

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

        Guideline written = guidelineArgumentCaptor.getValue();

        assertEquals(3, written.directives.size());

    }

    @Test
    public void should_overwrite_non_active_guideline() throws Exception {

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

        final String requestId = RandomStringUtils.randomAlphanumeric(10);
        final String lineItemId = "lineItemId" + RandomStringUtils.randomNumeric(10);
        final String screenId = "screenId" + RandomStringUtils.randomNumeric(10);
        final String sspId = "sspId" + RandomStringUtils.randomNumeric(10);

        Guideline guideline = Guideline.builder()
                .guidelineType(GuidelineType.MINIMISE_COSTS)
                .status(GuidelineStatus.CANCELLED)
                .directive(Directive.builder()
                        .screenId(screenId)
                        .sspId(sspId)
                        .lineItemId(lineItemId)
                        .type(DirectiveType.EXPLORATION)
                        .directiveId(uuidSupplier.get())
                        .requestId(requestId)
                        .timestamp(Instant.now())
                        .build())
                .build();

        BidEvidence bidEvidence = BidEvidence.builder()
                .requestId(requestId)
                .lineItemId(lineItemId)
                .screenId(screenId)
                .minPrice(BigDecimal.valueOf(1))
                .maxPrice(BigDecimal.valueOf(3))
                .actualPrice(BigDecimal.valueOf(1.6))
                .currencyCode("JOD")
                .timestamp(Instant.now())
                .sspId(sspId)
                .build();

        CacheData cacheData = CacheData.builder()
                .bidEvidence(bidEvidence)
                .requestId(requestId)
                .build();


        when(cacheReader.read(eq(requestId))).thenReturn(Optional.of(cacheData));
        when(guidelineReader.read(eq(lineItemId), eq(screenId), eq(sspId))).thenReturn(Optional.of(guideline));

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

        verify(directivePublisher).publish(directiveArgumentCaptor.capture());

        Map<DirectiveType, List<Directive>> grouped = directiveArgumentCaptor
                .getAllValues()
                .stream()
                .collect(Collectors.groupingBy((directive) -> directive.type));

        Directive lor = grouped.get(DirectiveType.EXPLORATION).get(0);

        assertNotNull(lor.priceChange);
        assertEquals(bidEvidence.actualPrice.subtract(BigDecimal.valueOf(0.1)), lor.newPrice);

        ArgumentCaptor<Guideline> guidelineArgumentCaptor = ArgumentCaptor.forClass(Guideline.class);

        verify(guidelineWriter).write(guidelineArgumentCaptor.capture());

        Guideline written = guidelineArgumentCaptor.getValue();

        assertEquals(GuidelineStatus.ACTIVE, written.status);
        assertEquals(1, written.directives.size());

    }

    @Test
    public void should_complete_exploration_when_received_win_but_no_capacity_for_price_reduce_exists() throws Exception {

        CacheReader cacheReader = mock(CacheReader.class);
        GuidelineWriter guidelineWriter = mock(GuidelineWriter.class);
        DirectivePublisher directivePublisher = mock(DirectivePublisher.class);
        CacheWriter cacheWriter = mock(CacheWriter.class);
        GuidelineReader guidelineReader = mock(GuidelineReader.class);
        PricerService pricerService = new PricerService(
                objectMapper, cacheReader, uuidSupplier, maxWinsPercentage, minCostsPercentage, guidelineWriter, priceChangeStep, moneyExchanger, directivePublisher, cacheWriter, guidelineReader, Instant::now);

        final String requestId = RandomStringUtils.randomAlphanumeric(10);
        final String lineItemId = "lineItemId" + RandomStringUtils.randomNumeric(10);
        final String screenId = "screenId" + RandomStringUtils.randomNumeric(10);
        final String sspId = "sspId" + RandomStringUtils.randomNumeric(10);

        UUID directiveId = uuidSupplier.get();

        Guideline guideline = Guideline.builder()
                .guidelineType(GuidelineType.MINIMISE_COSTS)
                .status(GuidelineStatus.ACTIVE)
                .directive(Directive.builder()
                        .screenId(screenId)
                        .sspId(sspId)
                        .lineItemId(lineItemId)
                        .type(DirectiveType.EXPLORATION)
                        .directiveId(directiveId)
                        .requestId(requestId)
                        .timestamp(Instant.now())
                        .build())
                .build();

        BidEvidence bidEvidence = BidEvidence.builder()
                .requestId(requestId)
                .lineItemId(lineItemId)
                .screenId(screenId)
                .sspId(sspId)
                .minPrice(BigDecimal.valueOf(1))
                .maxPrice(BigDecimal.valueOf(3))
                .actualPrice(BigDecimal.valueOf(1))
                .directiveId(Optional.of(directiveId))
                .currencyCode("JOD")
                .timestamp(Instant.now())
                .build();

        CacheData cacheData = CacheData.builder()
                .bidEvidence(bidEvidence)
                .requestId(requestId)
                .build();


        when(cacheReader.read(eq(requestId))).thenReturn(Optional.of(cacheData));
        when(guidelineReader.read(eq(lineItemId), eq(screenId), eq(sspId))).thenReturn(Optional.of(guideline));

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
        assertEquals(GuidelineStatus.COMPLETE, guidelineCaptured.status);

        ArgumentCaptor<Directive> directiveArgumentCaptor = ArgumentCaptor.forClass(Directive.class);
        verify(directivePublisher).publish(directiveArgumentCaptor.capture());
        List<Directive> directives = directiveArgumentCaptor.getAllValues();
        assertEquals(1, directives.size());
        Directive directive = directiveArgumentCaptor.getValue();
        assertEquals(DirectiveType.EXPLOITATION, directive.type);
        assertEquals(lineItemId, directive.lineItemId);
        assertEquals(screenId, directive.screenId);
        assertEquals(sspId, directive.sspId);
        //assertEquals(directive.newPrice);

    }

    @Test
    public void do_not_start_min_costs_when_max_wins_exists() throws Exception {

        CacheReader cacheReader = mock(CacheReader.class);
        GuidelineWriter guidelineWriter = mock(GuidelineWriter.class);
        DirectivePublisher directivePublisher = mock(DirectivePublisher.class);
        CacheWriter cacheWriter = mock(CacheWriter.class);
        GuidelineReader guidelineReader = mock(GuidelineReader.class);
        PricerService pricerService = new PricerService(
                objectMapper, cacheReader, uuidSupplier, maxWinsPercentage, minCostsPercentage, guidelineWriter, priceChangeStep, moneyExchanger, directivePublisher, cacheWriter, guidelineReader, Instant::now);

        final String requestId = RandomStringUtils.randomAlphanumeric(10);
        final String lineItemId = "lineItemId" + RandomStringUtils.randomNumeric(10);
        final String screenId = "screenId" + RandomStringUtils.randomNumeric(10);
        final String sspId = "sspId" + RandomStringUtils.randomNumeric(10);
        final UUID directiveId = uuidSupplier.get();

        Guideline guideline = Guideline.builder()
                .guidelineType(GuidelineType.MAXIMISE_WINS)
                .status(GuidelineStatus.ACTIVE)
                .directive(Directive.builder()
                        .screenId(screenId)
                        .sspId(sspId)
                        .lineItemId(lineItemId)
                        .type(DirectiveType.EXPLORATION)
                        .directiveId(directiveId)
                        .requestId(requestId)
                        .timestamp(Instant.now())
                        .build())
                .build();

        BidEvidence bidEvidence = BidEvidence.builder()
                .requestId(requestId)
                .lineItemId(lineItemId)
                .screenId(screenId)
                .minPrice(BigDecimal.valueOf(1))
                .maxPrice(BigDecimal.valueOf(3))
                .actualPrice(BigDecimal.valueOf(1.3))
                .currencyCode("JOD")
                .timestamp(Instant.now())
                .sspId(sspId)
                .build();

        CacheData cacheData = CacheData.builder()
                .bidEvidence(bidEvidence)
                .requestId(requestId)
                .build();

        when(cacheReader.read(eq(requestId))).thenReturn(Optional.of(cacheData));
        when(guidelineReader.read(eq(lineItemId), eq(screenId), eq(sspId))).thenReturn(Optional.of(guideline));

        WinNoticeMessage winNoticeMessage = WinNoticeMessage.builder()
                .type("win_notice")
                .time(Instant.now())
                .meta(WinNoticeMessage.Meta.builder()
                        .requestId(requestId)
                        .build())
                .build();

        // when
        pricerService.onWinNoticeMessage(winNoticeMessage);

        verifyNoInteractions(directivePublisher);
        verifyNoInteractions(guidelineWriter);

    }

    @Test
    public void should_not_create_new_guideline_when_recently_completed_max_wins_exists() throws Exception {

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

        final String requestId = RandomStringUtils.randomAlphanumeric(10);
        final String lineItemId = "lineItemId" + RandomStringUtils.randomNumeric(10);
        final String screenId = "screenId" + RandomStringUtils.randomNumeric(10);
        final String sspId = "sspId" + RandomStringUtils.randomNumeric(10);

        Guideline guideline = Guideline.builder()
                .guidelineType(GuidelineType.MAXIMISE_WINS)
                .status(GuidelineStatus.COMPLETE)
                .directive(Directive.builder()
                        .screenId(screenId)
                        .sspId(sspId)
                        .lineItemId(lineItemId)
                        .type(DirectiveType.EXPLORATION)
                        .directiveId(uuidSupplier.get())
                        .requestId(requestId)
                        .timestamp(Instant.now().minus(30, ChronoUnit.MINUTES))
                        .build())
                .build();

        BidEvidence bidEvidence = BidEvidence.builder()
                .requestId(requestId)
                .lineItemId(lineItemId)
                .screenId(screenId)
                .sspId(sspId)
                .minPrice(BigDecimal.valueOf(1))
                .maxPrice(BigDecimal.valueOf(3))
                .actualPrice(BigDecimal.valueOf(1.6))
                .directiveId(Optional.of(UUID.randomUUID()))
                .currencyCode("JOD")
                .timestamp(Instant.now())
                .build();

        CacheData cacheData = CacheData.builder()
                .bidEvidence(bidEvidence)
                .requestId(requestId)
                .build();

        when(cacheReader.read(eq(requestId))).thenReturn(Optional.of(cacheData));
        when(guidelineReader.read(eq(lineItemId), eq(screenId), eq(sspId))).thenReturn(Optional.of(guideline));

        WinNoticeMessage winNoticeMessage = WinNoticeMessage.builder()
                .type("loss_notice")
                .time(Instant.now())
                .meta(WinNoticeMessage.Meta.builder()
                        .requestId(requestId)
                        .build())
                .build();

        // when
        pricerService.onWinNoticeMessage(winNoticeMessage);

        verify(cacheWriter).delete(eq(cacheData));
        verifyNoInteractions(directivePublisher);
        verifyNoInteractions(guidelineWriter);

    }

    @Test
    public void should_create_minimise_costs_when_completed_max_wins_is_old() throws Exception {

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

        final String requestId = RandomStringUtils.randomAlphanumeric(10);
        final String lineItemId = "lineItemId" + RandomStringUtils.randomNumeric(10);
        final String screenId = "screenId" + RandomStringUtils.randomNumeric(10);
        final String sspId = "sspId" + RandomStringUtils.randomNumeric(10);

        UUID directiveId = UUID.randomUUID();

        Guideline guideline = Guideline.builder()
                .guidelineType(GuidelineType.MAXIMISE_WINS)
                .status(GuidelineStatus.COMPLETE)
                .directive(Directive.builder()
                        .screenId(screenId)
                        .sspId(sspId)
                        .lineItemId(lineItemId)
                        .type(DirectiveType.EXPLORATION)
                        .directiveId(directiveId)
                        .requestId(requestId)
                        .timestamp(Instant.now().minus(61, ChronoUnit.MINUTES))
                        .build())
                .build();

        log.debug("guideline: {}", guideline);

        BidEvidence bidEvidence = BidEvidence.builder()
                .requestId(requestId)
                .lineItemId(lineItemId)
                .screenId(screenId)
                .sspId(sspId)
                .minPrice(BigDecimal.valueOf(1))
                .maxPrice(BigDecimal.valueOf(3))
                .actualPrice(BigDecimal.valueOf(1.6))
                .directiveId(Optional.of(directiveId))
                .currencyCode("JOD")
                .timestamp(Instant.now())
                .build();

        log.debug("bidEidence: {}", bidEvidence);

        CacheData cacheData = CacheData.builder()
                .bidEvidence(bidEvidence)
                .requestId(requestId)
                .build();

        when(cacheReader.read(eq(requestId))).thenReturn(Optional.of(cacheData));
        when(guidelineReader.read(eq(lineItemId), eq(screenId), eq(sspId))).thenReturn(Optional.of(guideline));

        WinNoticeMessage winNoticeMessage = WinNoticeMessage.builder()
                .type("loss_notice")
                .time(Instant.now())
                .meta(WinNoticeMessage.Meta.builder()
                        .requestId(requestId)
                        .build())
                .build();

        // when
        pricerService.onWinNoticeMessage(winNoticeMessage);


        ArgumentCaptor<Directive> directiveArgumentCaptor = ArgumentCaptor.forClass(Directive.class);
        verify(directivePublisher).publish(directiveArgumentCaptor.capture());

        Map<DirectiveType, List<Directive>> grouped = directiveArgumentCaptor
                .getAllValues()
                .stream()
                .collect(Collectors.groupingBy((directive) -> directive.type));

        Directive lor = grouped.get(DirectiveType.EXPLORATION).get(0);

        assertNotNull(lor.priceChange);
        assertEquals(bidEvidence.actualPrice.subtract(BigDecimal.valueOf(0.1)), lor.newPrice);

        ArgumentCaptor<Guideline> guidelineArgumentCaptor = ArgumentCaptor.forClass(Guideline.class);

        verify(guidelineWriter).write(guidelineArgumentCaptor.capture());

        Guideline written = guidelineArgumentCaptor.getValue();

        assertEquals(GuidelineStatus.ACTIVE, written.status);
        assertEquals(1, written.directives.size());

    }

}
