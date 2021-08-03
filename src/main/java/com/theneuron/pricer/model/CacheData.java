package com.theneuron.pricer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.With;
import org.springframework.lang.Nullable;

@AllArgsConstructor
@Builder
@Value
public class CacheData {
    String requestId;
    BidEvidence bidEvidence;
    @Nullable
    @With
    Guideline guideline;
}
