package com.theneuron.pricer.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theneuron.pricer.model.*;
import com.theneuron.pricer.model.messages.BidResponseMessage;
import com.theneuron.pricer.model.messages.LossNoticeMessage;
import com.theneuron.pricer.model.messages.WinNoticeMessage;
import com.theneuron.pricer.repo.CacheReader;
import com.theneuron.pricer.repo.CacheWriter;
import com.theneuron.pricer.repo.GuidelineReader;
import com.theneuron.pricer.repo.GuidelineWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.javamoney.moneta.Money;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
public class PricerService implements BidResponseHandler, WinNoticeHandler, LossNoticeHandler {

    private final ObjectMapper objectMapper;
    private final CacheReader cacheReader;
    private final Supplier<UUID> uuidSupplier;
    private final Integer maxWinsPercentage;
    private final Integer minCostsPercentage;
    private final GuidelineWriter guidelineWriter;
    private final Money priceChangeStep;
    private final MoneyExchanger moneyExchanger;

    private final DirectivePublisher directivePublisher;
    private final CacheWriter cacheWriter;
    private final GuidelineReader guidelineReader;

    public PricerService(ObjectMapper objectMapper, CacheReader cacheReader, Supplier<UUID> uuidSupplier, Integer maxWinsPercentage, Integer minCostsPercentage, GuidelineWriter guidelineWriter, Money priceChangeStep, MoneyExchanger moneyExchanger, DirectivePublisher directivePublisher, CacheWriter cacheWriter, GuidelineReader guidelineReader) {
        this.objectMapper = objectMapper;
        this.cacheReader = cacheReader;
        this.uuidSupplier = uuidSupplier;
        this.maxWinsPercentage = maxWinsPercentage;
        this.minCostsPercentage = minCostsPercentage;
        this.guidelineWriter = guidelineWriter;
        this.priceChangeStep = priceChangeStep;
        this.moneyExchanger = moneyExchanger;
        this.directivePublisher = directivePublisher;
        this.cacheWriter = cacheWriter;
        this.guidelineReader = guidelineReader;
    }

    public static BidEvidence bidEvidence(BidResponseMessage bidResponseMessage, Integer i) throws Exception {

        final Double minPrice = Optional.ofNullable(bidResponseMessage.meta.getDealBidFloors())
                .map(list -> list.get(i))
                .orElse(Optional.ofNullable(bidResponseMessage.meta.getImpBidFloors())
                        .map(list -> list.get(i))
                        .orElseThrow(() -> new Exception("can't define minimum price")));

        return BidEvidence.builder()
                .requestId(bidResponseMessage.meta.getRequestId())
                .minPrice(BigDecimal.valueOf(minPrice))
                .actualPrice(BigDecimal.valueOf(bidResponseMessage.meta.getPrices().get(i)))
                .maxPrice(BigDecimal.valueOf(bidResponseMessage.meta.getLineItemPrices().get(i)))
                .lineItemId(bidResponseMessage.meta.getLineItemIds().get(i))
                .screenId(bidResponseMessage.meta.getScreenId())
                .currencyCode(bidResponseMessage.meta.getLineItemCurrencies().get(i))
                .directiveId(Optional.ofNullable(bidResponseMessage.meta.getDirectiveIds().get(i)))
                .build();
    }

    public void onBidResponseMessage(BidResponseMessage bidResponse) throws Exception {

        log.info("handling bid response: {}", objectMapper.writeValueAsString(bidResponse));

        // TODO implement support for multiple bids inside single bid response
        if (bidResponse.meta.getBidIds().size() != 1) {
            log.warn("bid responses with bids count != 1 are currently not supported");
            return;
        }

        BidEvidence bidEvidence = bidEvidence(bidResponse, 0);

        if (bidEvidence.actualPrice.compareTo(bidEvidence.maxPrice) > 0) {
            log.info("received bid evidence with actual price greater than maxPrice and defined directive, guideline would be cancelled");
            cancelGuidelineAndAllDirectives(bidEvidence);
            return;
        }

        Optional<CacheData> optionalCacheData = cacheReader.read(bidEvidence.requestId);

        if (optionalCacheData.isPresent() && optionalCacheData.get().getBidEvidence().equals(bidEvidence)) {
            log.info("message duplicate, ignoring");
            return;
        }
        else if (optionalCacheData.isPresent()) {
            String message = "existing cache message differs from received one with the same request id";
            log.error(message + " existing: {} received: {}", objectMapper.writeValueAsString(optionalCacheData.get()), objectMapper.writeValueAsString(bidEvidence));
            throw new Exception(message);
        }

        writeCache(bidEvidence);

    }

    public void onLossNoticeMessage(LossNoticeMessage lossNoticeMessage) throws Exception {

        log.info("handling loss notice: {}", objectMapper.writeValueAsString(lossNoticeMessage));

        String requestId = lossNoticeMessage.getMeta().getRequestId();

        log.info("requestId: {}", requestId);

        Optional<CacheData> optionalCacheData = cacheReader.read(requestId);
        if (!optionalCacheData.isPresent()) {
            log.info("cache data not found for requestId: {}", requestId);
            return;
        }
        CacheData cacheData = optionalCacheData.get();
        BidEvidence bidEvidence = Objects.requireNonNull(cacheData.getBidEvidence());
        Optional<Guideline> optionalGuideline = Optional.ofNullable(cacheData.getGuideline());

        if (!optionalGuideline.isPresent()) {
            optionalGuideline = guidelineReader.read(lossNoticeMessage.getMeta().getLineItemId(), lossNoticeMessage.getMeta().getScreenId());
        }
        else {
            log.info("guideline read from cache");
        }

        if (optionalGuideline.isPresent() && !bidEvidence.directiveId.isPresent()) {
            log.warn("guideline exists, bid evidence without directiveId would be ignored");
            return;
        }

        Money priceIncreaseCapacity = bidEvidence.getPriceIncreaseCapacity();

        if (priceIncreaseCapacity.isNegativeOrZero() && isMaximiseWinsGuidelineExistsAndActive(optionalGuideline)) {
            log.info("price has no increase capacity and still loses, guideline should be cancelled");
            cancelGuideline(bidEvidence, optionalGuideline.get());
            return;
        }

        increasePrice(bidEvidence, optionalGuideline, priceIncreaseCapacity, cacheData);

    }

    public void onWinNoticeMessage(WinNoticeMessage winNoticeMessage) throws Exception {
        log.info("handling win notice: {}", objectMapper.writeValueAsString(winNoticeMessage));

        String requestId = winNoticeMessage.getMeta().getRequestId();
        Optional<CacheData> optionalCacheData = cacheReader.read(requestId);
        if (!optionalCacheData.isPresent()) {
            log.info("cache data not found for requestId: {}", requestId);
            return;
        }

        CacheData cacheData = optionalCacheData.get();
        BidEvidence bidEvidence = Objects.requireNonNull(cacheData.getBidEvidence());
        Optional<Guideline> optionalGuideline = Optional.ofNullable(cacheData.getGuideline());

        if (!optionalGuideline.isPresent()) {
            optionalGuideline = guidelineReader.read(bidEvidence.lineItemId, bidEvidence.screenId);
        }

        if (isMaximiseWinsGuidelineExistsAndActive(optionalGuideline)) {
            log.info("ACTIVE MAXIMIZE_WINS guideline found");
            confirmMaximizeWinsGuideline(bidEvidence, optionalGuideline.get());
        }
        else {

            Money priceReduceCapacity = bidEvidence.getPriceReduceCapacity();

            if (priceReduceCapacity.isNegativeOrZero() && isMinimiseCostsGuidelineExistsAndActive(optionalGuideline)) {
                log.info("price has no reduce capacity and still wins, guideline should be cancelled");
                cancelGuideline(bidEvidence, optionalGuideline.get());
                return;
            }

            Money priceIncreaseStepConverted = getOrExchange(bidEvidence.getActualPriceMoney(), priceChangeStep);

            log.info("price may be reduced by: {} step: {}", priceReduceCapacity, priceIncreaseStepConverted);

            Money priceReduce = ObjectUtils.min(priceIncreaseStepConverted, priceReduceCapacity);

            log.info("price would be reduced by: {}", priceReduce);

            // create factory method
            Directive directiveLor = Directive.builder()
                    .directiveId(uuidSupplier.get())
                    .priceChange(priceReduce.negate().getNumberStripped())
                    .newPrice(bidEvidence.getActualPriceMoney().subtract(priceReduce).getNumberStripped())
                    .currencyCode(bidEvidence.currencyCode)
                    .type(DirectiveType.EXPLORATION)
                    .percentage(minCostsPercentage)
                    .screenId(bidEvidence.screenId)
                    .lineItemId(bidEvidence.lineItemId)
                    .requestId(bidEvidence.requestId)
                    .build();

            Guideline guideline = optionalGuideline.orElse(
                    Guideline.builder()
                            .status(GuidelineStatus.ACTIVE)
                            .guidelineType(GuidelineType.MINIMISE_COSTS)
                            .directive(directiveLor)
                            .build());

            if (bidEvidence.directiveId.isPresent()) {
                Directive directiveLoi = Directive.builder()
                        .directiveId(uuidSupplier.get())
                        .priceChange(null)
                        .newPrice(bidEvidence.getActualPriceMoney().getNumberStripped())
                        .currencyCode(bidEvidence.currencyCode)
                        .type(DirectiveType.EXPLOITATION)
                        .percentage(100)
                        .screenId(bidEvidence.screenId)
                        .lineItemId(bidEvidence.lineItemId)
                        .requestId(bidEvidence.requestId)
                        .build();
                guideline = guideline.toBuilder().directive(directiveLoi).build();
                directivePublisher.publish(directiveLoi);
            }

            cacheWriter.write(cacheData.withGuideline(guideline));
            guidelineWriter.write(guideline);
            directivePublisher.publish(directiveLor);

        }

    }

    private Money getOrExchange(Money fromPrice, Money step) throws Exception {
        if (fromPrice.getCurrency().equals(step.getCurrency())) {
            return step;
        }
        return moneyExchanger.exchange(step, fromPrice.getCurrency());
    }

    private void writeCache(BidEvidence bidEvidence) throws Exception {
        CacheData.CacheDataBuilder cacheDataBuilder = CacheData.builder()
                .bidEvidence(bidEvidence)
                .requestId(bidEvidence.getRequestId());
        Optional<Guideline> optionalGuideline = guidelineReader.read(bidEvidence.lineItemId, bidEvidence.screenId);
        optionalGuideline.ifPresent(cacheDataBuilder::guideline);
        cacheWriter.write(cacheDataBuilder.build());
    }

    private void cancelGuidelineAndAllDirectives(BidEvidence bidEvidence) throws Exception {
        Optional<Guideline> optionalGuideline = guidelineReader.read(bidEvidence.lineItemId, bidEvidence.screenId);
        if (isMaximiseWinsGuidelineExistsAndActive(optionalGuideline)) {
            log.info("guideline would be cancelled: {}", objectMapper.writeValueAsString(optionalGuideline.get()));
            Directive exploitationCancel = Directive.builder()
                    .screenId(bidEvidence.screenId)
                    .lineItemId(bidEvidence.lineItemId)
                    .type(DirectiveType.EXPLOITATION_CANCEL)
                    .build();
            Directive explorationCancel = exploitationCancel.toBuilder()
                    .type(DirectiveType.EXPLORATION_CANCEL)
                    .build();
            guidelineWriter.write(optionalGuideline.get().toBuilder()
                    .status(GuidelineStatus.CANCELLED)
                    .directive(exploitationCancel)
                    .directive(explorationCancel)
                    .build());
            directivePublisher.publish(exploitationCancel);
            directivePublisher.publish(explorationCancel);
        }
    }

    private void confirmMaximizeWinsGuideline(BidEvidence bidEvidence, Guideline guideline) throws Exception {

        if (guideline.directives.isEmpty()) {
            throw new Exception("guideline cannot have zero directives");
        }

        Directive latestDirective = guideline.directives.get(guideline.directives.size() - 1);

        if (latestDirective.type.equals(DirectiveType.EXPLORATION)) {
            Directive exploitation = Directive.builder()
                    .directiveId(uuidSupplier.get())
                    .type(DirectiveType.EXPLOITATION)
                    .lineItemId(bidEvidence.lineItemId)
                    .screenId(bidEvidence.screenId)
                    .percentage(100)
                    .build();

            guidelineWriter.write(guideline.toBuilder()
                    .directive(exploitation)
                    .status(GuidelineStatus.COMPLETE)
                    .build());

            directivePublisher.publish(exploitation);
        } else {
            log.error("latest directive type is {} but EXPLORATION required", latestDirective.type);
        }
    }

    public static Money getPriceIncreaseStep(Money fromPrice, Money step, MoneyExchanger moneyExchanger) throws Exception {
        // TODO implement s = 10*2^(n/2)
        if (fromPrice.getCurrency().equals(step.getCurrency())) {
            return step;
        }
        return moneyExchanger.exchange(step, fromPrice.getCurrency());
    }

    private void increasePrice(BidEvidence bidEvidence, Optional<Guideline> optionalGuideline, Money priceIncreaseCapacity, CacheData cacheData) throws Exception {

        Money priceIncreaseStepConverted = getPriceIncreaseStep(bidEvidence.getActualPriceMoney(), priceChangeStep, moneyExchanger);

        log.info("price may be increased by: {}, priceIncreaseStep: {}", priceIncreaseCapacity, priceIncreaseStepConverted);

        Money priceIncrease = ObjectUtils.min(priceIncreaseStepConverted, priceIncreaseCapacity);

        log.info("price would be increased by: {}", priceIncrease);

        Directive directive = Directive.builder()
                .directiveId(uuidSupplier.get())
                .priceChange(priceIncrease.getNumberStripped())
                .newPrice(bidEvidence.getActualPriceMoney().add(priceIncrease).getNumberStripped())
                .currencyCode(bidEvidence.currencyCode)
                .type(DirectiveType.EXPLORATION)
                .percentage(maxWinsPercentage)
                .screenId(bidEvidence.screenId)
                .lineItemId(bidEvidence.lineItemId)
                .requestId(bidEvidence.requestId)
                .build();

        Guideline guideline = optionalGuideline.orElse(
                Guideline.builder()
                        .status(GuidelineStatus.ACTIVE)
                        .guidelineType(GuidelineType.MAXIMISE_WINS)
                        .directive(directive)
                        .build());

        cacheWriter.write(cacheData.withGuideline(guideline));
        guidelineWriter.write(guideline);
        directivePublisher.publish(directive);

    }

    private void cancelGuideline(BidEvidence bidEvidence, Guideline guideline) throws Exception {
        Directive directive = Directive.builder()
                .screenId(bidEvidence.getScreenId())
                .lineItemId(bidEvidence.getLineItemId())
                .type(DirectiveType.EXPLORATION_CANCEL)
                .build();

        Guideline.GuidelineBuilder guidelineBuilder = guideline.toBuilder()
                .directive(directive);

        if (guideline.directives.stream().noneMatch(d -> d.type.equals(DirectiveType.EXPLOITATION))) {
            guidelineBuilder.status(GuidelineStatus.CANCELLED);
        }

        guideline = guidelineBuilder.build();

        guidelineWriter.write(guideline);
        directivePublisher.publish(directive);
    }

    private static Boolean isMaximiseWinsGuidelineExistsAndActive(Optional<Guideline> optionalGuideline) {
        return optionalGuideline
                .map(guideline -> guideline.guidelineType.equals(GuidelineType.MAXIMISE_WINS) && guideline.status.equals(GuidelineStatus.ACTIVE))
                .orElse(false);
    }

    private static Boolean isMinimiseCostsGuidelineExistsAndActive(Optional<Guideline> optionalGuideline) {
        return optionalGuideline
                .map(guideline -> guideline.guidelineType.equals(GuidelineType.MINIMISE_COSTS) && guideline.status.equals(GuidelineStatus.ACTIVE))
                .orElse(false);
    }

}
