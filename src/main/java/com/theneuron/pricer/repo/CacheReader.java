package com.theneuron.pricer.repo;

import com.theneuron.pricer.model.CacheData;

import java.util.Optional;

public interface CacheReader {
    Optional<CacheData> read(String requestId);
}
