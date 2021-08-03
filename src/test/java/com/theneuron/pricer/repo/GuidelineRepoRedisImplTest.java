package com.theneuron.pricer.repo;

import com.theneuron.pricer.config.AppConfig;
import com.theneuron.pricer.model.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Ignore
class GuidelineRepoRedisImplTest {

    Jedis jedis = new Jedis("localhost", 6379);

    GuidelineRepoRedisImpl guidelineRepoRedis = new GuidelineRepoRedisImpl(AppConfig.objectMapper(), jedis);

    @Test
    void test() throws Exception {

        String lineItemId = RandomStringUtils.randomAlphanumeric(10);
        String screenId = RandomStringUtils.randomNumeric(10);

        Optional<Guideline> result = guidelineRepoRedis.read(lineItemId, screenId);

        assertFalse(result.isPresent());

        Guideline guideline = Guideline.builder()
                .guidelineType(GuidelineType.MAXIMISE_WINS)
                .status(GuidelineStatus.ACTIVE)
                .directive(Directive.builder()
                        .type(DirectiveType.EXPLOITATION)
                        .lineItemId(lineItemId)
                        .screenId(screenId)
                        .build())
                .build();

        guidelineRepoRedis.write(guideline);

        result = guidelineRepoRedis.read(lineItemId, screenId);
        assertTrue(result.isPresent());
        assertEquals(guideline, result.get());

    }

}