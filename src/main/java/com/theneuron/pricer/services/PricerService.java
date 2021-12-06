package com.theneuron.pricer.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.theneuron.pricer.model.*;
import com.theneuron.pricer.model.messages.BidResponseMessage;
import com.theneuron.pricer.model.messages.LossNoticeMessage;
import com.theneuron.pricer.model.messages.WinNoticeMessage;
import com.theneuron.pricer.repo.CacheReader;
import com.theneuron.pricer.repo.CacheWriter;
import com.theneuron.pricer.repo.GuidelineReader;
import com.theneuron.pricer.repo.GuidelineWriter;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.javamoney.moneta.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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

    private final Supplier<Instant> nowSupplier;

    @Builder
    public PricerService(ObjectMapper objectMapper, CacheReader cacheReader, Supplier<UUID> uuidSupplier, Integer maxWinsPercentage, Integer minCostsPercentage, GuidelineWriter guidelineWriter, Money priceChangeStep, MoneyExchanger moneyExchanger, DirectivePublisher directivePublisher, CacheWriter cacheWriter, GuidelineReader guidelineReader, Supplier<Instant> nowSupplier) {
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
        this.nowSupplier = nowSupplier;
    }

    public static BidEvidence bidEvidence(BidResponseMessage bidResponseMessage, Integer i) throws Exception {

        final Double minPrice = Optional.ofNullable(bidResponseMessage.meta.getDealBidFloors())
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(i))
                .orElse(Optional.ofNullable(bidResponseMessage.meta.getImpBidFloors())
                        .map(list -> list.get(i))
                        .orElseThrow(() -> new Exception("can't define minimum price")));

        final String sspId = bidResponseMessage.meta.getSsp();
        final BigDecimal lineItemPrice = BigDecimal.valueOf(bidResponseMessage.meta.getLineItemPrices().get(i));
        final BigDecimal impMultiply = BigDecimal.valueOf(bidResponseMessage.meta.getImpMultiplies().get(i));
        final BigDecimal maxPrice = getMaxPrice(sspId, lineItemPrice, impMultiply);

        return BidEvidence.builder()
                .requestId(bidResponseMessage.meta.getRequestId())
                .minPrice(BigDecimal.valueOf(minPrice))
                .actualPrice(BigDecimal.valueOf(bidResponseMessage.meta.getPrices().get(i)))
                .maxPrice(maxPrice)
                .lineItemId(bidResponseMessage.meta.getLineItemIds().get(i))
                .screenId(bidResponseMessage.meta.getScreenId())
                .currencyCode(bidResponseMessage.meta.getLineItemCurrencies().get(i))
                .directiveId(Optional.ofNullable(bidResponseMessage.meta.getDirectiveIds())
                        .filter(list -> !list.isEmpty())
                        .map(list -> list.get(i)))
                .timestamp(bidResponseMessage.time)
                .sspId(sspId)
                .build();
    }

    static BigDecimal getMaxPrice(String sspId, BigDecimal lineItemPrice, BigDecimal impMultiply) {
        sspId = sspId.toUpperCase();
        switch (sspId) {
            case "VISTAR":
            case "VIOOH":
                return lineItemPrice.multiply(impMultiply);
            default:
                return lineItemPrice;
        }
    }

    public void onBidResponseMessage(BidResponseMessage bidResponse) throws Exception {

        log.debug("handling bid response: {}", objectMapper.writeValueAsString(bidResponse));

        if (bidResponse.meta.getSsp().trim().equalsIgnoreCase("FSSP") ||
                bidResponse.meta.getSsp().trim().equalsIgnoreCase("VISTAR")) {
            //log.debug("dynamic bidding for FSSP is disabled");
            return;
        }

        BidEvidence bidEvidence = bidEvidence(bidResponse, 0);

        if (bidEvidence.minPrice.doubleValue() == 0d || bidEvidence.maxPrice.doubleValue() == 0d) {
            log.warn("min or max price equals zero, correct behaviour cannot be guaranteed, skipping");
            if (bidEvidence.directiveId.isPresent()) {
                cancelGuidelineAndAllDirectives(bidEvidence, guidelineReader.read(bidEvidence.lineItemId, bidEvidence.screenId, bidEvidence.sspId));
            }
            return;
        }

        if (bidEvidence.actualPrice.compareTo(bidEvidence.maxPrice) > 0 && bidEvidence.directiveId.isPresent()) {
            log.debug("received bid evidence with actual price greater or equal than maxPrice and defined directive, guideline would be cancelled");
            cancelGuidelineAndAllDirectives(bidEvidence, guidelineReader.read(bidEvidence.lineItemId, bidEvidence.screenId, bidEvidence.sspId));
            return;
        } else if (bidEvidence.actualPrice.compareTo(bidEvidence.maxPrice) > 0) {
            log.warn("actual price {} is grater than max {}, case would be ignored", bidEvidence.actualPrice, bidEvidence.maxPrice);
            return;
        }

        Optional<CacheData> optionalCacheData = cacheReader.read(bidEvidence.requestId);

        if (optionalCacheData.isPresent() && optionalCacheData.get().getBidEvidence().equals(bidEvidence)) {
            log.debug("message duplicate, ignoring");
            return;
        } else if (optionalCacheData.isPresent()) {
            String message = "existing cache message differs from received one with the same request id";
            log.error(message + " existing: {} received: {}", objectMapper.writeValueAsString(optionalCacheData.get()), objectMapper.writeValueAsString(bidEvidence));
            throw new Exception(message);
        }

        writeCache(bidEvidence);

    }

    public boolean isLossReason(LossNoticeMessage lossNoticeMessage) {
        String lossReason = lossNoticeMessage.getMeta().getLossReason();
        return lossReason == null || Lists.newArrayList("102", "101", "another bid won").stream().anyMatch(str -> lossNoticeMessage.getMeta().getLossReason().trim().equals(str));
    }

    public void onLossNoticeMessage(LossNoticeMessage lossNoticeMessage) throws Exception {

        log.debug("handling loss notice: {}", objectMapper.writeValueAsString(lossNoticeMessage));

        String requestId = lossNoticeMessage.getMeta().getRequestId();

        log.debug("requestId: {}", requestId);

        Optional<CacheData> optionalCacheData = cacheReader.read(requestId);

        if (!optionalCacheData.isPresent()) {
            log.debug("CacheData not found for requestId: {}", requestId);
            return;
        }
        CacheData cacheData = optionalCacheData.get();
        BidEvidence bidEvidence = Objects.requireNonNull(cacheData.getBidEvidence());

        Optional<Guideline> optionalGuideline = guidelineReader.read(bidEvidence.lineItemId, bidEvidence.screenId, bidEvidence.sspId);

        // TODO REMOVE THIS?!
        if (!optionalGuideline.isPresent()) {
            optionalGuideline = guidelineReader.read(lossNoticeMessage.getMeta().getLineItemId(), lossNoticeMessage.getMeta().getScreenId(), lossNoticeMessage.getMeta().getSsp());
        }

        if (shouldBeSkipped(cacheData, bidEvidence, optionalGuideline)) return;

        Money priceIncreaseCapacity = bidEvidence.priceIncreaseCapacity();

        boolean isLossReason = isLossReason(lossNoticeMessage);

        if (optionalGuideline.filter(g -> (g.isActive() | g.isCompleted()) && g.isMaxWins() && priceIncreaseCapacity.isPositive()).isPresent() && isLossReason) {
            log.debug("existing ACTIVE MAXIMISE_WINS guideline: {}, price may be increased by {}", optionalGuideline.get(), priceIncreaseCapacity);
            increasePrice(bidEvidence, priceIncreaseCapacity, cacheData, optionalGuideline);
        } else if (optionalGuideline.filter(g -> g.isActiveMaxWins() && priceIncreaseCapacity.isNegativeOrZero()).isPresent()) {
            log.debug("existing ACTIVE MAXIMISE_WINS guideline found and would be cancelled because of no price increase capacity");
            cancelGuideline(bidEvidence, optionalGuideline.get());
        } else if (optionalGuideline.filter(g -> g.isActiveMinCosts()).isPresent()) {
            log.debug("existing ACTIVE MINIMISE_COSTS guideline found");
            cancelGuideline(bidEvidence, optionalGuideline.get());
        } else if (optionalGuideline.filter(g -> !g.isActive()).isPresent() && isLossReason) {
            log.debug("existing non ACTIVE guideline found, would be overwritten");
            increasePrice(bidEvidence, priceIncreaseCapacity, cacheData, Optional.empty());
        } else if (priceIncreaseCapacity.isPositive() && isLossReason) {
            log.debug("no guideline found, trying to maximise wins by increasing price");
            increasePrice(bidEvidence, priceIncreaseCapacity, cacheData, Optional.empty());
        } else if (!isLossReason) {
            log.debug("not a proper loss reason received: {}", lossNoticeMessage.getMeta().getLossReason());
        } else {
            log.debug("nothing to do on loss notice message");
        }

        cacheWriter.delete(cacheData);

    }

    public void onWinNoticeMessage(WinNoticeMessage winNoticeMessage) throws Exception {

        log.debug("handling win notice: {}", objectMapper.writeValueAsString(winNoticeMessage));

        String requestId = winNoticeMessage.getMeta().getRequestId();

        Optional<CacheData> optionalCacheData = cacheReader.read(requestId);
        if (!optionalCacheData.isPresent()) {
            log.debug("cache data not found for requestId: {}", requestId);
            return;
        }

        CacheData cacheData = optionalCacheData.get();
        BidEvidence bidEvidence = Objects.requireNonNull(cacheData.getBidEvidence());

        Optional<Guideline> optionalGuideline = guidelineReader.read(bidEvidence.lineItemId, bidEvidence.screenId, bidEvidence.sspId);

        if (shouldBeSkipped(cacheData, bidEvidence, optionalGuideline)) return;

        if (isMaximiseWinsGuidelineExistsAndActive(optionalGuideline)) {
            log.debug("ACTIVE MAXIMIZE_WINS guideline found");
            completeGuideline(bidEvidence, optionalGuideline.get());
            cacheWriter.delete(cacheData);
        } else if (isRecentlyCompletedMaxWinsGuideline(optionalGuideline)) {
            log.debug("MAXIMISE_WINS recently completed, would not try reduce price");
        } else if (isMinimiseCostsGuidelineExistsAndActive(optionalGuideline)) {

            Guideline guideline = optionalGuideline.get();

            Optional<Directive> previous = guideline.getDirectiveById(bidEvidence.directiveId.get());
            if (!previous.isPresent()) {
                String msg = String.format("no previous directive found by id: %s}", bidEvidence.directiveId.get());
                log.error(msg);
                throw new Exception(msg);
            }
            if (!previous.filter(d -> d.type.equals(DirectiveType.EXPLORATION)).isPresent()) {
                String msg = "previous directive should be exploration";
                log.error(msg);
                throw new Exception("previous directive should be exploration");
            }
            Directive loi = Directive.builder()
                    .directiveId(uuidSupplier.get())
                    .priceChange(null)
                    .newPrice(bidEvidence.actualPriceMoney().getNumberStripped())
                    .currencyCode(bidEvidence.currencyCode)
                    .type(DirectiveType.EXPLOITATION)
                    .percentage(100)
                    .screenId(bidEvidence.screenId)
                    .sspId(bidEvidence.sspId)
                    .lineItemId(bidEvidence.lineItemId)
                    .requestId(bidEvidence.requestId)
                    .timestamp(nowSupplier.get())
                    .build();
            guideline = guideline.toBuilder().directive(loi).build();
            log.debug("exploitation directive would be published: {}", loi);
            directivePublisher.publish(loi);
            createGuideline(cacheData, bidEvidence, guideline);
        } else if (bidEvidence.priceReduceCapacity().isPositive()) {
            reducePrice(bidEvidence, bidEvidence.priceReduceCapacity(), cacheData, Optional.empty());
        } else {
            log.debug("nothing to do");
        }

        cacheWriter.delete(cacheData);

    }

    private boolean shouldBeSkipped(CacheData cacheData, BidEvidence bidEvidence, Optional<Guideline> optionalGuideline) throws Exception {
        if (optionalGuideline.isPresent() && !optionalGuideline.get().isCancelled() && (!bidEvidence.directiveId.isPresent() || !optionalGuideline.get().getDirectiveById(bidEvidence.directiveId.get()).isPresent())) {
            log.warn("guideline exists, but without directiveId would be ignored: {}", bidEvidence);
            cacheWriter.delete(cacheData);
            return true;
        }
        return false;
    }


    private void createGuideline(CacheData cacheData, BidEvidence bidEvidence, Guideline guideline) throws Exception {

        Money priceReduceCapacity = bidEvidence.priceReduceCapacity();

        if (priceReduceCapacity.isNegativeOrZero()) {
            log.debug("price has no reduce capacity and still wins, guideline should be completed");
            completeGuideline(bidEvidence, guideline);
            return;
        }

        Money priceIncreaseStepConverted = getOrExchange(bidEvidence.actualPriceMoney(), priceChangeStep);

        log.debug("price may be reduced by: {} step: {}", priceReduceCapacity, priceIncreaseStepConverted);

        Money priceReduce = ObjectUtils.min(priceIncreaseStepConverted, priceReduceCapacity);

        log.debug("price would be reduced by: {}", priceReduce);

        // create factory method
        Directive lor = Directive.builder()
                .directiveId(uuidSupplier.get())
                .priceChange(priceReduce.negate().getNumberStripped())
                .newPrice(bidEvidence.actualPriceMoney().subtract(priceReduce).getNumberStripped())
                .currencyCode(bidEvidence.currencyCode)
                .type(DirectiveType.EXPLORATION)
                .percentage(minCostsPercentage)
                .screenId(bidEvidence.screenId)
                .sspId(bidEvidence.sspId)
                .lineItemId(bidEvidence.lineItemId)
                .requestId(bidEvidence.requestId)
                .timestamp(Instant.now())
                .build();

        guideline = guideline.toBuilder().directive(lor).build();

        log.debug("\nguideline would be written: {}\nexploration directive would be published: {}\n", guideline, lor);

        cacheWriter.delete(cacheData);
        guidelineWriter.write(guideline);
        directivePublisher.publish(lor);
    }

    private boolean isRecentlyCompletedMaxWinsGuideline(Optional<Guideline> optionalGuideline) {
        return optionalGuideline
                .filter(g -> g.guidelineType.equals(GuidelineType.MAXIMISE_WINS) && g.status.equals(GuidelineStatus.COMPLETE) && g.isAfter(nowSupplier.get().minus(1, ChronoUnit.HOURS)))
                .isPresent();
    }

    private Money getOrExchange(Money fromPrice, Money step) throws Exception {
        if (fromPrice.getCurrency().equals(step.getCurrency())) {
            return step;
        }
        return moneyExchanger.exchange(step, fromPrice.getCurrency());
    }

    private void writeCache(BidEvidence bidEvidence) throws Exception {
        CacheData cacheData = CacheData.builder()
                .bidEvidence(bidEvidence)
                .requestId(bidEvidence.getRequestId())
                .build();
        log.debug("CacheData would be written: {}", cacheData);
        cacheWriter.write(cacheData);
    }

    private void cancelGuidelineAndAllDirectives(BidEvidence bidEvidence, Optional<Guideline> optionalGuideline) throws Exception {
        if (optionalGuideline.isPresent() && optionalGuideline.get().isActive()) {
            log.debug("guideline would be cancelled: {}", optionalGuideline.get());
            Directive exploitationCancel = Directive.builder()
                    .screenId(bidEvidence.screenId)
                    .sspId(bidEvidence.sspId)
                    .lineItemId(bidEvidence.lineItemId)
                    .type(DirectiveType.EXPLOITATION_CANCEL)
                    .directiveId(uuidSupplier.get())
                    .requestId(bidEvidence.requestId)
                    .timestamp(nowSupplier.get())
                    .build();
            Directive explorationCancel = exploitationCancel.toBuilder()
                    .type(DirectiveType.EXPLORATION_CANCEL)
                    .directiveId(uuidSupplier.get())
                    .build();

            Guideline guideline = optionalGuideline.get().toBuilder()
                    .status(GuidelineStatus.CANCELLED)
                    .directive(exploitationCancel)
                    .directive(explorationCancel)
                    .build();

            deleteCache(guideline);
            guidelineWriter.write(guideline);
            directivePublisher.publish(exploitationCancel);
            directivePublisher.publish(explorationCancel);
        } else {
            log.warn("what if optionalGuideline is empty?");
        }
    }

    private void completeGuideline(BidEvidence bidEvidence, Guideline guideline) throws Exception {

        log.debug("guideline would be completed: {}", guideline);

        if (guideline.directives.isEmpty()) {
            throw new Exception("guideline cannot have zero directives");
        }

        Directive latestDirective = guideline.getLatestDirective();
        Guideline.GuidelineBuilder builder = guideline.toBuilder();

        if (latestDirective.type.equals(DirectiveType.EXPLORATION)) {
            Directive exploitation = latestDirective.toBuilder()
                    .directiveId(uuidSupplier.get())
                    .requestId(bidEvidence.requestId)
                    .timestamp(nowSupplier.get())
                    .type(DirectiveType.EXPLOITATION)
                    .percentage(100)
                    .build();

            builder.directive(exploitation);

            directivePublisher.publish(exploitation);
        } else {
            log.error("latest directive type = {}", latestDirective.type);
        }
        deleteCache(guideline);
        guidelineWriter.write(builder
                .status(GuidelineStatus.COMPLETE)
                .build());
    }

    private void deleteCache(Guideline guideline) throws Exception {
        List<String> requestIds = guideline.directives.stream()
                .map(Directive::getRequestId)
                .collect(Collectors.toList());
        log.debug("CacheData would be deleted for requestIds: {}", requestIds);
        cacheWriter.delete(requestIds.toArray(new String[0]));
    }

    public static Money getPriceChangeStep(Money fromPrice, Money step, MoneyExchanger moneyExchanger) throws Exception {
        // TODO implement s = 10*2^(n/2)
        if (fromPrice.getCurrency().equals(step.getCurrency())) {
            return step;
        }
        return moneyExchanger.exchange(step, fromPrice.getCurrency());
    }

    private void increasePrice(BidEvidence bidEvidence, Money priceIncreaseCapacity, CacheData cacheData, Optional<Guideline> optionalGuideline) throws Exception {

        Money priceIncreaseStepConverted = getPriceChangeStep(bidEvidence.actualPriceMoney(), priceChangeStep, moneyExchanger);

        log.debug("price may be increased by: {}, priceIncreaseStep: {}", priceIncreaseCapacity, priceIncreaseStepConverted);

        Money priceIncrease = ObjectUtils.min(priceIncreaseStepConverted, priceIncreaseCapacity);

        log.debug("price would be increase by: {}", priceIncrease);

        Money increasedPrice = bidEvidence.actualPriceMoney().add(priceIncrease);

        if (optionalGuideline.isPresent()) {
            Guideline existingGuideline = optionalGuideline.get();
            Directive latestDirective = existingGuideline.getLatestDirective();
            if (latestDirective.isExploration() && latestDirective.newPriceMoney().isGreaterThanOrEqualTo(increasedPrice)) {
                log.debug("price increase to: {} would not have effect, current exploration amount: {}", increasedPrice, latestDirective.newPriceMoney());
                return;
            }
        }

        Directive lor = Directive.builder()
                .directiveId(uuidSupplier.get())
                .priceChange(priceIncrease.getNumberStripped())
                .newPrice(increasedPrice.getNumberStripped())
                .currencyCode(bidEvidence.currencyCode)
                .type(DirectiveType.EXPLORATION)
                .percentage(maxWinsPercentage)
                .screenId(bidEvidence.screenId)
                .sspId(bidEvidence.sspId)
                .lineItemId(bidEvidence.lineItemId)
                .requestId(bidEvidence.requestId)
                .timestamp(nowSupplier.get())
                .build();

        Guideline guideline = optionalGuideline
                .map(g -> g.toBuilder()
                        .directive(lor)
                        .status(GuidelineStatus.ACTIVE)
                        .build())
                .orElse(Guideline.builder()
                        .status(GuidelineStatus.ACTIVE)
                        .guidelineType(GuidelineType.MAXIMISE_WINS)
                        .directive(lor)
                        .build());

        guidelineWriter.write(guideline);
        directivePublisher.publish(lor);

    }

    private void reducePrice(BidEvidence bidEvidence, Money capacity, CacheData cacheData, Optional<Guideline> optionalGuideline) throws Exception {

        Money step = getPriceChangeStep(bidEvidence.actualPriceMoney(), priceChangeStep, moneyExchanger);

        log.debug("price may be reduced by: {}, priceIncreaseStep: {}", capacity, step);

        Money priceIncrease = ObjectUtils.min(step.abs(), capacity.abs());

        log.debug("price would be increase by: {}", priceIncrease);

        final Money reducedPrice = bidEvidence.actualPriceMoney().subtract(priceIncrease);

        if (optionalGuideline.isPresent()) {
            Guideline existingGuideline = optionalGuideline.get();
            Directive latestDirective = existingGuideline.getLatestDirective();
            if (latestDirective.isExploration() && latestDirective.newPriceMoney().isLessThanOrEqualTo(reducedPrice)) {
                log.debug("price reduce to: {} would not have effect, current exploration amount: {}", reducedPrice, latestDirective.newPriceMoney());
                return;
            }
        }

        Directive lor = Directive.builder()
                .directiveId(uuidSupplier.get())
                .priceChange(priceIncrease.negate().getNumberStripped())
                .newPrice(reducedPrice.getNumberStripped())
                .currencyCode(bidEvidence.currencyCode)
                .type(DirectiveType.EXPLORATION)
                .percentage(minCostsPercentage)
                .screenId(bidEvidence.screenId)
                .sspId(bidEvidence.sspId)
                .lineItemId(bidEvidence.lineItemId)
                .requestId(bidEvidence.requestId)
                .timestamp(nowSupplier.get())
                .build();

        Guideline guideline = optionalGuideline
                .map(g -> g.toBuilder().directive(lor).build())
                .orElse(Guideline.builder()
                        .status(GuidelineStatus.ACTIVE)
                        .guidelineType(GuidelineType.MINIMISE_COSTS)
                        .directive(lor)
                        .build());

        cacheWriter.delete(cacheData);
        guidelineWriter.write(guideline);
        directivePublisher.publish(lor);

    }

    private void cancelGuideline(BidEvidence bidEvidence, final Guideline guideline) throws Exception {

        log.debug("guideline would be cancelled: {}", guideline);

        Directive latestDirective = guideline.getLatestDirective();

        Guideline.GuidelineBuilder builder = guideline.toBuilder();

        Optional<Directive> optionalDirective = guideline.getDirectiveById(bidEvidence.directiveId.get());

        if (optionalDirective.filter(d -> d.isExploitation() && guideline.isActiveMinCosts()).isPresent() || !guideline.has(DirectiveType.EXPLOITATION)) {
            builder.status(GuidelineStatus.CANCELLED);
        }

        if (latestDirective.type.equals(DirectiveType.EXPLORATION)) {
            Directive directive = Directive.builder()
                    .screenId(bidEvidence.getScreenId())
                    .sspId(bidEvidence.sspId)
                    .lineItemId(bidEvidence.getLineItemId())
                    .type(DirectiveType.EXPLORATION_CANCEL)
                    .directiveId(uuidSupplier.get())
                    .timestamp(nowSupplier.get())
                    .requestId(bidEvidence.requestId)
                    .build();
            builder.directive(directive);
            directivePublisher.publish(directive);
        }

        final Guideline newGuideline = builder.build();

        deleteCache(guideline);

        log.debug("\nwriting guideline: {}", guideline);
        guidelineWriter.write(newGuideline);

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
