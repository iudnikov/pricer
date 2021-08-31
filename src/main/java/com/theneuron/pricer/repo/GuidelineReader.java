package com.theneuron.pricer.repo;

import com.theneuron.pricer.model.Guideline;

import java.util.Optional;
import java.util.UUID;

public interface GuidelineReader {
    Optional<Guideline> read(UUID guidelineId);
    Optional<Guideline> read(String lineItemId, String screenId) throws Exception;
}
