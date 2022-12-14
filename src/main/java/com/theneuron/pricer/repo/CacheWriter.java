package com.theneuron.pricer.repo;

import com.theneuron.pricer.model.CacheData;

public interface CacheWriter {
    void write(CacheData cacheData) throws Exception;
    void delete(CacheData cacheData) throws Exception;
    void delete(String... key) throws Exception;
}
