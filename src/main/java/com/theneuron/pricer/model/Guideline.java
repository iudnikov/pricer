package com.theneuron.pricer.model;

import lombok.*;
import org.javamoney.moneta.Money;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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


    public Optional<Directive> getLatestLoi() {
        final List<Directive> lois = directives.stream().
                filter(Directive::isLoi).
                sorted(Comparator.comparing(directive -> directive.timestamp)).
                collect(Collectors.toList());

        if (lois.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(lois.get(lois.size() - 1));
    }

    public Directive getLatestLor() {
        final List<Directive> lors = directives.stream().
                filter(Directive::isLor).
                sorted(Comparator.comparing(directive -> directive.timestamp)).
                collect(Collectors.toList());
        return lors.get(lors.size() - 1);
    }

    public final Boolean isActiveMaxWins() {
        return status.equals(GuidelineStatus.ACTIVE) && guidelineType.equals(GuidelineType.MAXIMISE_WINS);
    }

    public final Boolean isActive() {
        return status.equals(GuidelineStatus.ACTIVE);
    }

    public final Boolean isValid() {
        return !directives.isEmpty() && directives.get(0).isLor();
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

