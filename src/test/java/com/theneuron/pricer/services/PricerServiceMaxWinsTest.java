package com.theneuron.pricer.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theneuron.pricer.config.AppConfig;
import com.theneuron.pricer.model.*;
import com.theneuron.pricer.model.messages.BidResponseMessage;
import com.theneuron.pricer.model.messages.LossNoticeMessage;
import com.theneuron.pricer.model.messages.WinNoticeMessage;
import com.theneuron.pricer.repo.*;
import lombok.NonNull;
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

        BidEvidence bidEvidenceOne = BidEvidence.builder()
                .requestId(requestId)
                .lineItemId(lineItemId)
                .screenId(screenId)
                .minPrice(BigDecimal.valueOf(3))
                .actualPrice(BigDecimal.valueOf(10))
                .maxPrice(BigDecimal.valueOf(10))
                .currencyCode("USD")
                .directiveId(Optional.of(directiveId))
                .build();

        Guideline existingGuideline = Guideline.builder()
                .status(GuidelineStatus.ACTIVE)
                .guidelineType(GuidelineType.MAXIMISE_WINS)
                .directive(Directive.builder()
                        .directiveId(directiveId)
                        .type(DirectiveType.EXPLORATION)
                        .lineItemId(lineItemId)
                        .screenId(screenId)
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
        DirectivePublisher directivePublisher = mock(DirectivePublisher.class);
        CacheReader cacheReader = mock(CacheReader.class);
        CacheWriter cacheWriter = mock(CacheWriter.class);
        GuidelineReader guidelineReader = mock(GuidelineReader.class);
        UUID directiveId = UUID.randomUUID();

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
                        .lineItemId(lineItemId)
                        .screenId(screenId)
                        .build())
                .directive(Directive.builder()
                        .type(DirectiveType.EXPLOITATION)
                        .lineItemId(lineItemId)
                        .screenId(screenId)
                        .build())
                .directive(Directive.builder()
                        .type(DirectiveType.EXPLORATION)
                        .directiveId(directiveId)
                        .lineItemId(lineItemId)
                        .screenId(screenId)
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
                .directiveId(Optional.of(directiveId))
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
                .lineItemId(lineItemId)
                .screenId(screenId)
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

        assertEquals(GuidelineStatus.COMPLETE, guideline.status);
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

        Guideline existingGuideline = Guideline.builder()
                .status(GuidelineStatus.ACTIVE)
                .guidelineType(GuidelineType.MAXIMISE_WINS)
                .directive(Directive.builder()
                        .directiveId(directiveId)
                        .type(EXPLORATION)
                        .screenId(screenId)
                        .lineItemId(lineItemId)
                        .build())
                .directive(Directive.builder()
                        .directiveId(UUID.randomUUID())
                        .type(EXPLOITATION)
                        .screenId(screenId)
                        .lineItemId(lineItemId)
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
                        .directiveId(directiveId)
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
                        .directiveId(null)
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

    @Test
    public void test_min_max_price_zero_check() throws Exception {

        GuidelineWriter guidelineWriter = mock(GuidelineWriter.class);
        DirectivePublisher directivePublisher = mock(DirectivePublisher.class);
        CacheReader cacheReader = mock(CacheReader.class);
        CacheWriter cacheWriter = mock(CacheWriter.class);
        GuidelineReader guidelineReader = mock(GuidelineReader.class);

        PricerService pricerService = new PricerService(
                AppConfig.objectMapper(), cacheReader, UUID::randomUUID, maxWinsPercentage, minCostsPercentage, guidelineWriter, priceIncreaseStep, moneyExchanger, directivePublisher, cacheWriter, guidelineReader);

        BidResponseMessage bidResponseMessage = objectMapper.readValue(stagingBidEvidenceOne(), BidResponseMessage.class);

        pricerService.onBidResponseMessage(bidResponseMessage);

        verifyNoInteractions(guidelineReader, cacheWriter);

    }

    @NonNull
    private String stagingBidEvidenceOne() {
        return "{\n" +
                "    \"type\": \"bid_response\",\n" +
                "    \"time\": \"2021-09-16T12:54:29.304Z\",\n" +
                "    \"content\": {\n" +
                "      \"id\": \"64ebd379-2f6e-4a7c-be4f-fcc3fe6505bf\",\n" +
                "      \"seatbid\": [\n" +
                "        {\n" +
                "          \"bid\": [\n" +
                "            {\n" +
                "              \"id\": \"1631796869302\",\n" +
                "              \"impid\": \"1\",\n" +
                "              \"price\": 0.0,\n" +
                "              \"nurl\": \"https://bidder.dooh-api.staging.theneuron.com/fssp/winnotice?bid_cost=0.0&impression_id=1&line_item_id=e31ffda8-2a8b-4f45-ace8-f1dbac07d92f&agency_id=a9178ede-274c-4709-93f5-0631e3e75b8d&imp_multiply=1.8333334289491177&bid_id=1631796869302&bid_cost_with_markup=0.0&creative_id=94854a28-1ab7-45d4-826e-23322d45e2a1&advertiser_id=f1b64017-7a45-48ed-9539-7e26dadb1ad9&win_price=${WIN_PRICE}&instance_id=68402eca-62c4-4766-80c4-372a1a6ac167&instance_markup=25.0&screen_id=2399955&bid_currency=JOD&advertiser_markup=0.0&request_id=64ebd379-2f6e-4a7c-be4f-fcc3fe6505bf&campaign_id=c4612111-47d6-4a1b-9056-fb0d3e43796b\",\n" +
                "              \"lurl\": \"https://bidder.dooh-api.staging.theneuron.com/fssp/lossnotice?bid_cost=0.0&impression_id=1&line_item_id=e31ffda8-2a8b-4f45-ace8-f1dbac07d92f&agency_id=a9178ede-274c-4709-93f5-0631e3e75b8d&imp_multiply=1.8333334289491177&bid_id=1631796869302&bid_cost_with_markup=0.0&creative_id=94854a28-1ab7-45d4-826e-23322d45e2a1&loss_reason=${AUCTION_LOSS}&advertiser_id=f1b64017-7a45-48ed-9539-7e26dadb1ad9&instance_id=68402eca-62c4-4766-80c4-372a1a6ac167&instance_markup=25.0&screen_id=2399955&bid_currency=JOD&advertiser_markup=0.0&request_id=64ebd379-2f6e-4a7c-be4f-fcc3fe6505bf&campaign_id=c4612111-47d6-4a1b-9056-fb0d3e43796b\",\n" +
                "              \"adid\": \"94854a28-1ab7-45d4-826e-23322d45e2a1\",\n" +
                "              \"adomain\": [\n" +
                "                \"theneuron.com\"\n" +
                "              ],\n" +
                "              \"w\": 1920,\n" +
                "              \"h\": 1080,\n" +
                "              \"ext\": {\n" +
                "                \"mime_type\": \"image/jpeg\",\n" +
                "                \"creative_type\": \"IMAGE\",\n" +
                "                \"creative_url\": \"https://dooh-dsp-media.staging.theneuron.com/68402eca-62c4-4766-80c4-372a1a6ac167/f1b64017-7a45-48ed-9539-7e26dadb1ad9/creatives/a391cfff-eae9-441d-94a4-6e6154b9c1dc.jpg\"\n" +
                "              }\n" +
                "            }\n" +
                "          ],\n" +
                "          \"seat\": \"theneuron\",\n" +
                "          \"group\": 0\n" +
                "        }\n" +
                "      ],\n" +
                "      \"cur\": \"JOD\"\n" +
                "    },\n" +
                "    \"meta\": {\n" +
                "      \"ssp\": \"fssp\",\n" +
                "      \"request_id\": \"64ebd379-2f6e-4a7c-be4f-fcc3fe6505bf\",\n" +
                "      \"screen_id\": \"2399955\",\n" +
                "      \"publisher_id\": \"a0ba4950-9b3d-4898-b406-f80013452beb\",\n" +
                "      \"campaign_ids\": [\n" +
                "        \"c4612111-47d6-4a1b-9056-fb0d3e43796b\"\n" +
                "      ],\n" +
                "      \"line_item_ids\": [\n" +
                "        \"e31ffda8-2a8b-4f45-ace8-f1dbac07d92f\"\n" +
                "      ],\n" +
                "      \"creative_ids\": [\n" +
                "        \"94854a28-1ab7-45d4-826e-23322d45e2a1\"\n" +
                "      ],\n" +
                "      \"impression_ids\": [\n" +
                "        \"1\"\n" +
                "      ],\n" +
                "      \"bid_ids\": [\n" +
                "        \"1631796869302\"\n" +
                "      ],\n" +
                "      \"prices\": [\n" +
                "        0.0\n" +
                "      ],\n" +
                "      \"costs\": [\n" +
                "        0.0\n" +
                "      ],\n" +
                "      \"costs_with_markup\": [\n" +
                "        0.0\n" +
                "      ],\n" +
                "      \"deal_ids\": [\n" +
                "        null\n" +
                "      ],\n" +
                "      \"currencies\": [\n" +
                "        \"JOD\"\n" +
                "      ],\n" +
                "      \"imp_bid_floors\": [\n" +
                "        0.0\n" +
                "      ],\n" +
                "      \"imp_bid_floor_curs\": [\n" +
                "        \"JOD\"\n" +
                "      ],\n" +
                "      \"imp_multiplies\": [\n" +
                "        1.8333334289491177,\n" +
                "        1.8333334289491177\n" +
                "      ],\n" +
                "      \"line_item_prices\": [\n" +
                "        0.0\n" +
                "      ],\n" +
                "      \"line_item_currencies\": [\n" +
                "        \"JOD\"\n" +
                "      ],\n" +
                "      \"instance_markups\": [\n" +
                "        25.0\n" +
                "      ],\n" +
                "      \"advertiser_markups\": [\n" +
                "        0.0\n" +
                "      ],\n" +
                "      \"directive_ids\": [\n" +
                "        null\n" +
                "      ]\n" +
                "    }\n" +
                "  }";
    }

}