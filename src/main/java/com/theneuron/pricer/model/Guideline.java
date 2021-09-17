package com.theneuron.pricer.model;

import lombok.*;

import java.util.List;

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

    @AllArgsConstructor
    @Builder
    public static final class Decision {
        public final List<Directive> directives;
        public final Guideline guideline;
    }

    public final Boolean isActive() {
        return status.equals(GuidelineStatus.ACTIVE);
    }


}

