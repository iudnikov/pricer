package com.theneuron.pricer.repo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theneuron.pricer.jedis.JedisStatefulClient;
import com.theneuron.pricer.model.CacheData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
@Repository
public class CacheRepoRedisImpl implements CacheWriter, CacheReader {

    private final JedisStatefulClient jedisSupplier;
    private final ObjectMapper objectMapper;

    public CacheRepoRedisImpl(
            JedisStatefulClient jedisSupplier,
            ObjectMapper objectMapper
    ) {
        this.jedisSupplier = jedisSupplier;
        this.objectMapper = objectMapper;
    }

    @Override
    public void write(CacheData cacheData) throws Exception {
        String key = getKey(Objects.requireNonNull(cacheData.getRequestId()));
        String value = objectMapper.writeValueAsString(cacheData);
        int secondsToExpire = 24 * 60 * 60;
        jedisSupplier.get().set(key, value, new SetParams().ex(secondsToExpire));
    }

    private String getKey(String s) {
        return String.join("#", CacheData.class.getName(), s);
    }

    @Override
    public void delete(CacheData cacheData) throws Exception {
        String key = getKey(Objects.requireNonNull(cacheData.getRequestId()));
        jedisSupplier.get().del(key);
    }

    @Override
    public void delete(String... key) throws Exception {
        jedisSupplier.get().del(key);
    }

    @Override
    public Optional<CacheData> read(String requestId) {
        return Optional.ofNullable(jedisSupplier.get().get(getKey(requestId)))
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
