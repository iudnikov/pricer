package com.theneuron.pricer.repo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theneuron.pricer.model.Directive;
import com.theneuron.pricer.model.Guideline;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Repository
public class GuidelineRepoRedisImpl implements GuidelineReader, GuidelineWriter {

    private final ObjectMapper objectMapper;
    private final Jedis jedis;

    public GuidelineRepoRedisImpl(ObjectMapper objectMapper, Jedis jedis) {
        this.objectMapper = objectMapper;
        this.jedis = jedis;
    }

    @Override
    public Optional<Guideline> read(UUID guidelineId) {
        return Optional.empty();
    }

    @Override
    public Optional<Guideline> read(String lineItemId, String screenId) {
        String key = getKey(lineItemId, screenId);
        return Optional.ofNullable(jedis.get(key)).map(data -> {
            try {
                return objectMapper.readValue(data, Guideline.class);
            } catch (JsonProcessingException e) {
                log.error("can't read Guideline from: {}", data);
                return null;
            }
        });
    }

    @Override
    public void write(Guideline guideline) throws Exception {
        if (guideline.directives.isEmpty()) {
            throw new Exception("can't write guideline without directives");
        }
        Directive directive = guideline.directives.get(0);
        String key = getKey(directive.lineItemId, directive.screenId);
        String value = objectMapper.writeValueAsString(guideline);
        jedis.set(key, value);
    }

    private static String getKey(String lineItemId, String screenId) {
        return String.join("#", Guideline.class.getName(), lineItemId, screenId);
    }
}
