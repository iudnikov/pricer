package com.theneuron.pricer.repo;

import com.theneuron.pricer.config.AppConfig;
import com.theneuron.pricer.model.CacheData;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Ignore
class CacheReaderRedisImplTest {

    Jedis jedis = new Jedis("localhost", 6379);

    CacheRepoRedisImpl cacheRepoRedis = new CacheRepoRedisImpl(jedis, AppConfig.objectMapper());

    @Test
    void test() throws Exception {

        final String requestId = RandomStringUtils.randomAlphanumeric(10);

        Optional<CacheData> result = cacheRepoRedis.read(requestId);
        assertFalse(result.isPresent());

        CacheData cacheData = CacheData.builder()
                .requestId(requestId)
                .build();

        cacheRepoRedis.write(cacheData);

        result = cacheRepoRedis.read(requestId);
        assertTrue(result.isPresent());
        assertEquals(cacheData, result.get());

    }

}