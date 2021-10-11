package com.theneuron.pricer.services;

import com.theneuron.pricer.mock.UUIDSupplierQueued;
import com.theneuron.pricer.model.Directive;
import com.theneuron.pricer.model.DirectiveType;
import com.theneuron.pricer.model.Guideline;
import com.theneuron.pricer.model.messages.BidResponseMessage;
import com.theneuron.pricer.model.messages.CurrencyRatesMessage;
import com.theneuron.pricer.model.messages.LossNoticeMessage;
import com.theneuron.pricer.model.messages.WinNoticeMessage;
import com.theneuron.pricer.repo.CurrencyRateWriter;
import com.theneuron.pricer.repo.GuidelineReader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.joda.time.DateTime;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@RunWith(SpringRunner.class)
@SpringBootTest()
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles("local")
@Slf4j
public class PricerServiceIntegrationTest {

    @Autowired
    PricerService pricerService;

    @Autowired
    CurrencyRateWriter currencyRateWriter;

    @Autowired
    UUIDSupplierQueued uuidSupplierQueued;

    @Autowired
    GuidelineReader guidelineReader;

    @BeforeEach
    public void beforeEach() {
        currencyRateWriter.write(DateTime.now(), CurrencyRatesMessage.CurrencyRate.builder()
                .currencyPair("USD/JOD")
                .rate(BigDecimal.valueOf(0.7))
                .build());
        uuidSupplierQueued.clear();
    }

    final String sspId = "ssp";

    @Test
    public void should_maximise_wins_cancelled_after_max_price_reached() throws Exception {

        final String screenOne = "screen-" + RandomStringUtils.randomNumeric(10);
        final String lineItemOne = "line-item-" + RandomStringUtils.randomNumeric(10);
        final String requestIdOne = "request-" + RandomStringUtils.randomNumeric(10);

        pricerService.onBidResponseMessage(
                BidResponseMessage.of(
                        Instant.now(),
                        BidResponseMessage.Meta.builder()
                                .ssp(sspId)
                                .bidId("bid-one")
                                .requestId(requestIdOne)
                                .currency("JOD")
                                .price(3.00)
                                .impBidFloor(3.00)
                                .impBidFloorCur("JOD")
                                .lineItemPrice(3.10)
                                .lineItemCurrency("JOD")
                                .lineItemId(lineItemOne)
                                .screenId(screenOne)
                                .build())
                        .build());

        final UUID directiveIdOne = uuidSupplierQueued.putRandom();

        // should try to increase price
        pricerService.onLossNoticeMessage(LossNoticeMessage.of(
                Instant.now(),
                LossNoticeMessage.Meta.builder()
                        .ssp(sspId)
                        .screenId(screenOne)
                        .lineItemId(lineItemOne)
                        .requestId(requestIdOne)
                        .lossReason("just because you are looser")
                        .build())
                .build());

        final String requestIdTwo = "request-" + RandomStringUtils.randomNumeric(10);

        pricerService.onBidResponseMessage(
                BidResponseMessage.of(
                        Instant.now(),
                        BidResponseMessage.Meta.builder()
                                .bidId("bid-one")
                                .ssp(sspId)
                                .requestId(requestIdTwo)
                                .currency("JOD")
                                .price(3.07)
                                .impBidFloor(3.00)
                                .impBidFloorCur("JOD")
                                .lineItemPrice(3.10)
                                .lineItemCurrency("JOD")
                                .lineItemId(lineItemOne)
                                .screenId(screenOne)
                                .directiveId(directiveIdOne)
                                .build())
                        .build());

        final UUID directiveIdTwo = uuidSupplierQueued.putRandom();

        // should try to increase price
        pricerService.onLossNoticeMessage(LossNoticeMessage.of(
                Instant.now(),
                LossNoticeMessage.Meta.builder()
                        .screenId(screenOne)
                        .ssp(sspId)
                        .lineItemId(lineItemOne)
                        .requestId(requestIdTwo)
                        .lossReason("just because you are looser")
                        .build())
                .build());

        final String requestIdThree = "request-" + RandomStringUtils.randomNumeric(10);

        final UUID directiveIdThree = uuidSupplierQueued.putRandom();

        pricerService.onBidResponseMessage(
                BidResponseMessage.of(
                        Instant.now(),
                        BidResponseMessage.Meta.builder()
                                .bidId("bid-one")
                                .ssp(sspId)
                                .requestId(requestIdThree)
                                .currency("JOD")
                                .price(3.10)
                                .impBidFloor(3.00)
                                .impBidFloorCur("JOD")
                                .lineItemPrice(3.10)
                                .lineItemCurrency("JOD")
                                .lineItemId(lineItemOne)
                                .screenId(screenOne)
                                .directiveId(directiveIdOne)
                                .build())
                        .build());

        // should cancel exploration and cancel guideline
        pricerService.onLossNoticeMessage(LossNoticeMessage.of(
                Instant.now(),
                LossNoticeMessage.Meta.builder()
                        .screenId(screenOne)
                        .ssp(sspId)
                        .lineItemId(lineItemOne)
                        .requestId(requestIdThree)
                        .lossReason("just because you are looser")
                        .build())
                .build());

        Guideline guideline = guidelineReader.read(lineItemOne, screenOne, sspId).get();

        Directive directiveOne = guideline.getDirectiveById(directiveIdOne).get();
        Directive directiveTwo = guideline.getDirectiveById(directiveIdTwo).get();
        Directive directiveThree = guideline.getDirectiveById(directiveIdThree).get();

        assertEquals(DirectiveType.EXPLORATION, directiveOne.type);
        assertEquals(BigDecimal.valueOf(3.07), directiveOne.newPrice);
        assertEquals(DirectiveType.EXPLORATION, directiveTwo.type);
        assertEquals(BigDecimal.valueOf(3.10), directiveTwo.newPrice);
        assertEquals(DirectiveType.EXPLORATION_CANCEL, directiveThree.type);

        // then it should not try to do something with previous requestId
        pricerService.onLossNoticeMessage(LossNoticeMessage.of(
                Instant.now(),
                LossNoticeMessage.Meta.builder()
                        .ssp(sspId)
                        .screenId(screenOne)
                        .lineItemId(lineItemOne)
                        .requestId(requestIdTwo)
                        .lossReason("just because you are looser")
                        .build())
                .build());

        // then it should not try to do something with previous requestId
        pricerService.onWinNoticeMessage(WinNoticeMessage.of(
                Instant.now(),
                WinNoticeMessage.Meta.builder()
                        .ssp(sspId)
                        .screenId(screenOne)
                        .lineItemId(lineItemOne)
                        .requestId(requestIdTwo)
                        .winPrice(3.07)
                        .build())
                .build());

    }

    @Test
    public void should_maximise_wins_complete_after_win_received() throws Exception {

        final String screenOne = "screen-" + RandomStringUtils.randomNumeric(10);
        final String lineItemOne = "line-item-" + RandomStringUtils.randomNumeric(10);
        final String requestIdOne = "request-" + RandomStringUtils.randomNumeric(10);

        pricerService.onBidResponseMessage(
                BidResponseMessage.of(
                        Instant.now(),
                        BidResponseMessage.Meta.builder()
                                .bidId("bid-one")
                                .ssp(sspId)
                                .requestId(requestIdOne)
                                .currency("JOD")
                                .price(3.00)
                                .impBidFloor(3.00)
                                .impBidFloorCur("JOD")
                                .lineItemPrice(3.10)
                                .lineItemCurrency("JOD")
                                .lineItemId(lineItemOne)
                                .screenId(screenOne)
                                .ssp(sspId)
                                .build())
                        .build());

        final UUID directiveIdOne = uuidSupplierQueued.putRandom();

        // should try to increase price
        pricerService.onLossNoticeMessage(LossNoticeMessage.of(
                Instant.now(),
                LossNoticeMessage.Meta.builder()
                        .ssp(sspId)
                        .screenId(screenOne)
                        .lineItemId(lineItemOne)
                        .requestId(requestIdOne)
                        .lossReason("just because you are looser")
                        .build())
                .build());

        final String requestIdTwo = "request-" + RandomStringUtils.randomNumeric(10);

        pricerService.onBidResponseMessage(
                BidResponseMessage.of(
                        Instant.now(),
                        BidResponseMessage.Meta.builder()
                                .ssp(sspId)
                                .bidId("bid-one")
                                .requestId(requestIdTwo)
                                .currency("JOD")
                                .price(3.07)
                                .impBidFloor(3.00)
                                .impBidFloorCur("JOD")
                                .lineItemPrice(3.10)
                                .lineItemCurrency("JOD")
                                .lineItemId(lineItemOne)
                                .screenId(screenOne)
                                .directiveId(directiveIdOne)
                                .build())
                        .build());

        final UUID directiveIdTwo = uuidSupplierQueued.putRandom();

        // should try to increase price
        pricerService.onLossNoticeMessage(LossNoticeMessage.of(
                Instant.now(),
                LossNoticeMessage.Meta.builder()
                        .ssp(sspId)
                        .screenId(screenOne)
                        .lineItemId(lineItemOne)
                        .requestId(requestIdTwo)
                        .lossReason("just because you are looser")
                        .build())
                .build());

        final String requestIdThree = "request-" + RandomStringUtils.randomNumeric(10);

        final UUID directiveIdThree = uuidSupplierQueued.putRandom();

        pricerService.onBidResponseMessage(
                BidResponseMessage.of(
                        Instant.now(),
                        BidResponseMessage.Meta.builder()
                                .ssp(sspId)
                                .bidId("bid-one")
                                .requestId(requestIdThree)
                                .currency("JOD")
                                .price(3.10)
                                .impBidFloor(3.00)
                                .impBidFloorCur("JOD")
                                .lineItemPrice(3.10)
                                .lineItemCurrency("JOD")
                                .lineItemId(lineItemOne)
                                .screenId(screenOne)
                                .directiveId(directiveIdOne)
                                .build())
                        .build());

        pricerService.onWinNoticeMessage(WinNoticeMessage.of(
                Instant.now(),
                WinNoticeMessage.Meta.builder()
                        .ssp(sspId)
                        .screenId(screenOne)
                        .lineItemId(lineItemOne)
                        .requestId(requestIdThree)
                        .winPrice(3.10)
                        .build())
                .build());

        final UUID directiveIdFour = uuidSupplierQueued.putRandom();

        // then it should not try to do something with previous requestId
        pricerService.onWinNoticeMessage(WinNoticeMessage.of(
                Instant.now(),
                WinNoticeMessage.Meta.builder()
                        .ssp(sspId)
                        .screenId(screenOne)
                        .lineItemId(lineItemOne)
                        .requestId(requestIdThree)
                        .winPrice(3.10)
                        .build())
                .build());

    }

    @Test
    public void should_minimise_costs_when_win_received() throws Exception {

        final String screenOne = "screen-" + RandomStringUtils.randomNumeric(10);
        final String lineItemOne = "line-item-" + RandomStringUtils.randomNumeric(10);
        final String requestIdOne = "request-" + RandomStringUtils.randomNumeric(10);

        pricerService.onBidResponseMessage(
                BidResponseMessage.of(
                        Instant.now(),
                        BidResponseMessage.Meta.builder()
                                .ssp(sspId)
                                .bidId("bid-one")
                                .requestId(requestIdOne)
                                .currency("JOD")
                                .price(3.10)
                                .impBidFloor(3.00)
                                .impBidFloorCur("JOD")
                                .lineItemPrice(3.10)
                                .lineItemCurrency("JOD")
                                .lineItemId(lineItemOne)
                                .screenId(screenOne)
                                .build())
                        .build());

        final UUID directiveIdOne = uuidSupplierQueued.putRandom();

        // should try to reduce price
        pricerService.onWinNoticeMessage(WinNoticeMessage.of(
                Instant.now(),
                WinNoticeMessage.Meta.builder()
                        .screenId(screenOne)
                        .ssp(sspId)
                        .lineItemId(lineItemOne)
                        .requestId(requestIdOne)
                        .winPrice(3.10)
                        .build())
                .build());

        // if still wins, should issue exploitation and issue new exploration
        pricerService.onBidResponseMessage(
                BidResponseMessage.of(
                        Instant.now(),
                        BidResponseMessage.Meta.builder()
                                .ssp(sspId)
                                .bidId("bid-one")
                                .requestId(requestIdOne)
                                .currency("JOD")
                                .price(3.03)
                                .impBidFloor(3.00)
                                .impBidFloorCur("JOD")
                                .lineItemPrice(3.10)
                                .lineItemCurrency("JOD")
                                .lineItemId(lineItemOne)
                                .screenId(screenOne)
                                .directiveId(directiveIdOne)
                                .build())
                        .build());

        final UUID directiveIdTwo = uuidSupplierQueued.putRandom();
        final UUID directiveIdThree = uuidSupplierQueued.putRandom();

        pricerService.onWinNoticeMessage(WinNoticeMessage.of(
                Instant.now(),
                WinNoticeMessage.Meta.builder()
                        .screenId(screenOne)
                        .ssp(sspId)
                        .lineItemId(lineItemOne)
                        .requestId(requestIdOne)
                        .winPrice(3.03)
                        .build())
                .build());

        // if still wins should issue exploration and complete guideline

        pricerService.onBidResponseMessage(
                BidResponseMessage.of(
                        Instant.now(),
                        BidResponseMessage.Meta.builder()
                                .bidId("bid-one")
                                .ssp(sspId)
                                .requestId(requestIdOne)
                                .currency("JOD")
                                .price(3.00)
                                .impBidFloor(3.00)
                                .impBidFloorCur("JOD")
                                .lineItemPrice(3.10)
                                .lineItemCurrency("JOD")
                                .lineItemId(lineItemOne)
                                .screenId(screenOne)
                                .directiveId(directiveIdThree)
                                .build())
                        .build());

        final UUID directiveIdFour = uuidSupplierQueued.putRandom();

        pricerService.onWinNoticeMessage(WinNoticeMessage.of(
                Instant.now(),
                WinNoticeMessage.Meta.builder()
                        .screenId(screenOne)
                        .ssp(sspId)
                        .lineItemId(lineItemOne)
                        .requestId(requestIdOne)
                        .winPrice(3.00)
                        .build())
                .build());
    }

    @Test
    public void should_cancel_minimise_costs_when_loss_received_and_no_exploitation_directives_exist() throws Exception {
        final String screenOne = "screen-" + RandomStringUtils.randomNumeric(10);
        final String lineItemOne = "line-item-" + RandomStringUtils.randomNumeric(10);
        final String requestIdOne = "request-" + RandomStringUtils.randomNumeric(10);

        pricerService.onBidResponseMessage(
                BidResponseMessage.of(
                        Instant.now(),
                        BidResponseMessage.Meta.builder().ssp(sspId)
                                .bidId("bid-one")
                                .requestId(requestIdOne)
                                .currency("JOD")
                                .price(3.10)
                                .impBidFloor(3.00)
                                .impBidFloorCur("JOD")
                                .lineItemPrice(3.10)
                                .lineItemCurrency("JOD")
                                .lineItemId(lineItemOne)
                                .screenId(screenOne)
                                .build())
                        .build());

        final UUID directiveIdOne = uuidSupplierQueued.putRandom();

        // should try to reduce price
        pricerService.onWinNoticeMessage(WinNoticeMessage.of(
                Instant.now(),
                WinNoticeMessage.Meta.builder().ssp(sspId)
                        .screenId(screenOne)
                        .lineItemId(lineItemOne)
                        .requestId(requestIdOne)
                        .winPrice(3.10)
                        .build())
                .build());

        pricerService.onBidResponseMessage(
                BidResponseMessage.of(
                        Instant.now(),
                        BidResponseMessage.Meta.builder().ssp(sspId)
                                .bidId("bid-one")
                                .requestId(requestIdOne)
                                .currency("JOD")
                                .price(3.03)
                                .impBidFloor(3.00)
                                .impBidFloorCur("JOD")
                                .lineItemPrice(3.10)
                                .lineItemCurrency("JOD")
                                .lineItemId(lineItemOne)
                                .screenId(screenOne)
                                .directiveId(directiveIdOne)
                                .build())
                        .build());

        final UUID directiveIdTwo = uuidSupplierQueued.putRandom();

        // should try to reduce price
        pricerService.onLossNoticeMessage(LossNoticeMessage.of(
                Instant.now(),
                LossNoticeMessage.Meta.builder().ssp(sspId)
                        .screenId(screenOne)
                        .lineItemId(lineItemOne)
                        .requestId(requestIdOne)
                        .lossReason("some reason")
                        .build())
                .build());
    }

    @Test
    public void should_not_start_minimise_costs_on_first_loss() throws Exception {
        final String screenOne = "screen-" + RandomStringUtils.randomNumeric(10);
        final String lineItemOne = "line-item-" + RandomStringUtils.randomNumeric(10);
        final String requestIdOne = "request-" + RandomStringUtils.randomNumeric(10);

        pricerService.onBidResponseMessage(
                BidResponseMessage.of(
                        Instant.now(),
                        BidResponseMessage.Meta.builder().ssp(sspId)
                                .bidId("bid-one")
                                .requestId(requestIdOne)
                                .currency("JOD")
                                .price(3.10)
                                .impBidFloor(3.00)
                                .impBidFloorCur("JOD")
                                .lineItemPrice(3.10)
                                .lineItemCurrency("JOD")
                                .lineItemId(lineItemOne)
                                .screenId(screenOne)
                                .build())
                        .build());

        final UUID directiveIdOne = uuidSupplierQueued.putRandom();

        // should do nothing
        pricerService.onLossNoticeMessage(LossNoticeMessage.of(
                Instant.now(),
                LossNoticeMessage.Meta.builder().ssp(sspId)
                        .screenId(screenOne)
                        .lineItemId(lineItemOne)
                        .requestId(requestIdOne)
                        .lossReason("some reason")
                        .build())
                .build());
    }

    @Test
    public void should_handle_complete_and_then_activate() throws Exception {

        final String screenOne = "screen-" + RandomStringUtils.randomNumeric(10);
        final String lineItemOne = "line-item-" + RandomStringUtils.randomNumeric(10);
        final String requestIdOne = "request-" + RandomStringUtils.randomNumeric(10);

        pricerService.onBidResponseMessage(
                BidResponseMessage.of(
                        Instant.now(),
                        BidResponseMessage.Meta.builder().ssp(sspId)
                                .bidId("bid-one")
                                .requestId(requestIdOne)
                                .currency("JOD")
                                .price(3.00)
                                .impBidFloor(3.00)
                                .impBidFloorCur("JOD")
                                .lineItemPrice(3.50)
                                .lineItemCurrency("JOD")
                                .lineItemId(lineItemOne)
                                .screenId(screenOne)
                                .build())
                        .build());

        final UUID directiveIdOne = uuidSupplierQueued.putRandom();

        // should try to increase price
        pricerService.onLossNoticeMessage(LossNoticeMessage.of(
                Instant.now(),
                LossNoticeMessage.Meta.builder().ssp(sspId)
                        .screenId(screenOne)
                        .lineItemId(lineItemOne)
                        .requestId(requestIdOne)
                        .lossReason("just because you are looser")
                        .build())
                .build());

        final String requestIdTwo = "request-" + RandomStringUtils.randomNumeric(10);

        pricerService.onBidResponseMessage(
                BidResponseMessage.of(
                        Instant.now(),
                        BidResponseMessage.Meta.builder().ssp(sspId)
                                .bidId("bid-one")
                                .requestId(requestIdTwo)
                                .currency("JOD")
                                .price(3.07)
                                .impBidFloor(3.00)
                                .impBidFloorCur("JOD")
                                .lineItemPrice(3.50)
                                .lineItemCurrency("JOD")
                                .lineItemId(lineItemOne)
                                .screenId(screenOne)
                                .directiveId(directiveIdOne)
                                .build())
                        .build());

        final UUID directiveIdTwo = uuidSupplierQueued.putRandom();

        // should try to increase price
        pricerService.onLossNoticeMessage(LossNoticeMessage.of(
                Instant.now(),
                LossNoticeMessage.Meta.builder().ssp(sspId)
                        .screenId(screenOne)
                        .lineItemId(lineItemOne)
                        .requestId(requestIdTwo)
                        .lossReason("just because you are looser")
                        .build())
                .build());

        final String requestIdThree = "request-" + RandomStringUtils.randomNumeric(10);
        final UUID directiveIdThree = uuidSupplierQueued.putRandom();

        pricerService.onBidResponseMessage(
                BidResponseMessage.of(
                        Instant.now(),
                        BidResponseMessage.Meta.builder().ssp(sspId)
                                .bidId("bid-one")
                                .requestId(requestIdThree)
                                .currency("JOD")
                                .price(3.10)
                                .impBidFloor(3.00)
                                .impBidFloorCur("JOD")
                                .lineItemPrice(3.50)
                                .lineItemCurrency("JOD")
                                .lineItemId(lineItemOne)
                                .screenId(screenOne)
                                .directiveId(directiveIdOne)
                                .build())
                        .build());

        pricerService.onWinNoticeMessage(WinNoticeMessage.of(
                Instant.now(),
                WinNoticeMessage.Meta.builder().ssp(sspId)
                        .screenId(screenOne)
                        .lineItemId(lineItemOne)
                        .requestId(requestIdThree)
                        .winPrice(3.10)
                        .build())
                .build());

        final String requestIdFour = "request-4-" + RandomStringUtils.randomNumeric(10);
        final UUID directiveIdFour = uuidSupplierQueued.putRandom();

        pricerService.onBidResponseMessage(
                BidResponseMessage.of(
                        Instant.now(),
                        BidResponseMessage.Meta.builder().ssp(sspId)
                                .bidId("bid-one")
                                .requestId(requestIdFour)
                                .currency("JOD")
                                .price(3.10)
                                .impBidFloor(3.00)
                                .impBidFloorCur("JOD")
                                .lineItemPrice(3.50)
                                .lineItemCurrency("JOD")
                                .lineItemId(lineItemOne)
                                .screenId(screenOne)
                                .directiveId(directiveIdOne)
                                .build())
                        .build());

        pricerService.onLossNoticeMessage(LossNoticeMessage.of(
                Instant.now(),
                LossNoticeMessage.Meta.builder().ssp(sspId)
                        .screenId(screenOne)
                        .lineItemId(lineItemOne)
                        .requestId(requestIdFour)
                        .lossReason("we are loosing again")
                        .build())
                .build());


    }

    @Test
    public void should_increase_latest_price() throws Exception {

        final String screenOne = "screen-" + RandomStringUtils.randomNumeric(10);
        final String lineItemOne = "line-item-" + RandomStringUtils.randomNumeric(10);
        final String sspId = "sspId-" + RandomStringUtils.randomNumeric(10);

        final String requestIdZero = "request-zero-" + RandomStringUtils.randomNumeric(10);

        // bid 0
        pricerService.onBidResponseMessage(
                BidResponseMessage.of(
                        Instant.now(),
                        BidResponseMessage.Meta.builder().ssp(sspId)
                                .bidId("bid-zero")
                                .requestId(requestIdZero)
                                .currency("JOD")
                                .price(3.00)
                                .impBidFloor(3.00)
                                .impBidFloorCur("JOD")
                                .lineItemPrice(3.50)
                                .lineItemCurrency("JOD")
                                .lineItemId(lineItemOne)
                                .screenId(screenOne)
                                .build())
                        .build());

        final UUID directiveIdZero = uuidSupplierQueued.putRandom();

        // loss 0
        pricerService.onLossNoticeMessage(LossNoticeMessage.of(
                Instant.now(),
                LossNoticeMessage.Meta.builder().ssp(sspId)
                        .screenId(screenOne)
                        .lineItemId(lineItemOne)
                        .requestId(requestIdZero)
                        .lossReason("looser!")
                        .build())
                .build());


        final String requestIdZeroZero = "request-zero-zero-" + RandomStringUtils.randomNumeric(10);

        // bid 00
        pricerService.onBidResponseMessage(
                BidResponseMessage.of(
                        Instant.now(),
                        BidResponseMessage.Meta.builder().ssp(sspId)
                                .bidId("bid-zero")
                                .requestId(requestIdZeroZero)
                                .currency("JOD")
                                .price(3.07)
                                .impBidFloor(3.00)
                                .impBidFloorCur("JOD")
                                .lineItemPrice(3.50)
                                .lineItemCurrency("JOD")
                                .lineItemId(lineItemOne)
                                .screenId(screenOne)
                                .directiveId(directiveIdZero)
                                .build())
                        .build());

        final UUID directiveIdZeroZero = uuidSupplierQueued.putRandom();

        pricerService.onWinNoticeMessage(WinNoticeMessage.of(
                Instant.now(),
                WinNoticeMessage.Meta.builder().ssp(sspId)
                        .screenId(screenOne)
                        .lineItemId(lineItemOne)
                        .requestId(requestIdZeroZero)
                        .winPrice(3.07)
                        .build())
                .build());

        ////////////////////

        final String requestIdOne = "request-one-" + RandomStringUtils.randomNumeric(10);

        // bid 1
        pricerService.onBidResponseMessage(
                BidResponseMessage.of(
                        Instant.now(),
                        BidResponseMessage.Meta.builder().ssp(sspId)
                                .bidId("bid-one")
                                .requestId(requestIdOne)
                                .currency("JOD")
                                .price(3.07)
                                .impBidFloor(3.00)
                                .impBidFloorCur("JOD")
                                .lineItemPrice(3.50)
                                .lineItemCurrency("JOD")
                                .lineItemId(lineItemOne)
                                .screenId(screenOne)
                                .directiveId(directiveIdZeroZero)
                                .build())
                        .build());

        final UUID directiveIdOne = uuidSupplierQueued.putRandom();

        // loss 1
        pricerService.onLossNoticeMessage(LossNoticeMessage.of(
                Instant.now(),
                LossNoticeMessage.Meta.builder().ssp(sspId)
                        .screenId(screenOne)
                        .lineItemId(lineItemOne)
                        .requestId(requestIdOne)
                        .lossReason("looser!")
                        .build())
                .build());


        // bid 2
        final String requestIdTwo = "request-two-" + RandomStringUtils.randomNumeric(10);
        pricerService.onBidResponseMessage(
                BidResponseMessage.of(
                        Instant.now(),
                        BidResponseMessage.Meta.builder().ssp(sspId)
                                .bidId("bid-two")
                                .requestId(requestIdTwo)
                                .currency("JOD")
                                .price(3.07)
                                .impBidFloor(3.00)
                                .impBidFloorCur("JOD")
                                .lineItemPrice(3.50)
                                .lineItemCurrency("JOD")
                                .lineItemId(lineItemOne)
                                .screenId(screenOne)
                                .directiveId(directiveIdZeroZero)
                                .build())
                        .build());

        // bid 3
        final String requestIdThree = "request-three-" + RandomStringUtils.randomNumeric(10);
        pricerService.onBidResponseMessage(
                BidResponseMessage.of(
                        Instant.now(),
                        BidResponseMessage.Meta.builder().ssp(sspId)
                                .bidId("bid-three")
                                .requestId(requestIdThree)
                                .currency("JOD")
                                .price(3.14)
                                .impBidFloor(3.00)
                                .impBidFloorCur("JOD")
                                .lineItemPrice(3.50)
                                .lineItemCurrency("JOD")
                                .lineItemId(lineItemOne)
                                .screenId(screenOne)
                                .directiveId(directiveIdOne)
                                .build())
                        .build());

        final UUID directiveIdTwo = uuidSupplierQueued.putRandom();
        final UUID directiveIdThree = uuidSupplierQueued.putRandom();

        // loss 2
        pricerService.onLossNoticeMessage(LossNoticeMessage.of(
                Instant.now(),
                LossNoticeMessage.Meta.builder().ssp(sspId)
                        .screenId(screenOne)
                        .lineItemId(lineItemOne)
                        .requestId(requestIdTwo)
                        .lossReason("looser!")
                        .build())
                .build());

        // loss 3
        pricerService.onLossNoticeMessage(LossNoticeMessage.of(
                Instant.now(),
                LossNoticeMessage.Meta.builder().ssp(sspId)
                        .screenId(screenOne)
                        .lineItemId(lineItemOne)
                        .requestId(requestIdThree)
                        .lossReason("looser!")
                        .build())
                .build());

        Optional<Guideline> guideline = guidelineReader.read(lineItemOne, screenOne, sspId);

        log.info("guideline: {}", guideline);
    }

}