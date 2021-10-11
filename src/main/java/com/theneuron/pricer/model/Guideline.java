package com.theneuron.pricer.model;

import lombok.*;
import org.javamoney.moneta.Money;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * group of several price directives,
 * should contain all conditions how to issue new price directives
 */
@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class Guideline {

    @Singular
    public List<Directive> directives;
    @NonNull
    public GuidelineType guidelineType;
    @NonNull
    public GuidelineStatus status;

    public boolean isAfter(Instant t) {
        return !directives.isEmpty() && directives.get(directives.size() - 1).timestamp.isAfter(t);
    }

    public Optional<Directive> getDirectiveById(UUID uuid) {
        return directives.stream().filter(d -> d.getDirectiveId().equals(uuid)).findAny();
    }

    public Directive getLatestDirective() {
        return directives.get(directives.size() - 1);
    }

    public final Boolean isActiveMaxWins() {
        return status.equals(GuidelineStatus.ACTIVE) && guidelineType.equals(GuidelineType.MAXIMISE_WINS);
    }

    public final Boolean isActive() {
        return status.equals(GuidelineStatus.ACTIVE);
    }

    public final Boolean isCancelled() {
        return status.equals(GuidelineStatus.CANCELLED);
    }

    public final Boolean isCompleted() {
        return status.equals(GuidelineStatus.COMPLETE);
    }

    public final Boolean isMaxWins() {
        return guidelineType.equals(GuidelineType.MAXIMISE_WINS);
    }

    public final Boolean isActiveMinCosts() {
        return status.equals(GuidelineStatus.ACTIVE) && guidelineType.equals(GuidelineType.MINIMISE_COSTS);
    }

    public boolean has(DirectiveType directiveType) {
        return directives.stream().anyMatch(d -> d.type.equals(directiveType));
    }

}

