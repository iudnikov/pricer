package com.theneuron.pricer.repo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theneuron.pricer.model.CacheData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;

import java.util.Objects;
import java.util.Optional;

@Slf4j
@Repository
public class CacheRepoRedisImpl implements CacheWriter, CacheReader {

    private final Jedis jedis;
    private final ObjectMapper objectMapper;

    public CacheRepoRedisImpl(Jedis jedis, ObjectMapper objectMapper) {
        this.jedis = jedis;
        this.objectMapper = objectMapper;
    }

    @Override
    public void write(CacheData cacheData) throws Exception {
        String key = String.join("#", CacheData.class.getName(), Objects.requireNonNull(cacheData.getRequestId()));
        String value = objectMapper.writeValueAsString(cacheData);
        jedis.set(key, value);
    }

    @Override
    public Optional<CacheData> read(String requestId) {
        return Optional.ofNullable(jedis.get(String.join("#", CacheData.class.getName(), requestId)))
                .map(data -> {
                    try {
                        return objectMapper.readValue(data, CacheData.class);
                    } catch (JsonProcessingException e) {
                        log.error("can't unmarshal data: {}", data, e);
                        return null;
                    }
                });
    }
}
