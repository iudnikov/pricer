package com.theneuron.pricer.repo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.theneuron.pricer.model.Directive;
import com.theneuron.pricer.model.Guideline;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    public List<Guideline> readAll(@Nullable Function<Guideline, Boolean> filter) throws Exception {
        ScanResult<String> result = jedis.scan("0", new ScanParams().count(1000000).match(Guideline.class.getName() + "*"));
        if (!result.getCursor().equals("0")) {
            throw new Exception("can't read from jedis");
        }
        if (result.getResult().isEmpty()) {
            return ImmutableList.of();
        }
        List<String> values = jedis.mget(result.getResult().toArray(new String[result.getResult().size()]));
        List<Guideline> list = new ArrayList<>();
        values.forEach(data -> {
            try {
                list.add(objectMapper.readValue(data, Guideline.class));
            } catch (Exception e) {
                log.error("can't read guideline from: {}", data, e);
            }
        });
        return ImmutableList.copyOf(list);
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
