package com.theneuron.pricer.repo;

import com.theneuron.pricer.model.Guideline;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public interface GuidelineReader {
    Optional<Guideline> read(String lineItemId, String screenId) throws Exception;
    List<Guideline> readAll(Function<Guideline, Boolean> filter) throws Exception;
}
