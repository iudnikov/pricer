package com.theneuron.pricer.repo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.theneuron.pricer.jedis.JedisStatefulClient;
import com.theneuron.pricer.model.Directive;
import com.theneuron.pricer.model.Guideline;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class GuidelineRepoRedisImpl implements GuidelineReader, GuidelineWriter {

    private final ObjectMapper objectMapper;
    private final JedisStatefulClient jedis;

    public GuidelineRepoRedisImpl(
            ObjectMapper objectMapper,
            JedisStatefulClient jedis
    ) {
        this.objectMapper = objectMapper;
        this.jedis = jedis;
    }

    @Override
    public List<Guideline> readAll(@Nullable Function<Guideline, Boolean> filter) throws Exception {
        ScanResult<String> result = jedis.get().scan("0", new ScanParams().count(1000000).match(Guideline.class.getName() + "*"));
        if (!result.getCursor().equals("0")) {
            throw new Exception("can't read from jedis");
        }
        if (result.getResult().isEmpty()) {
            return ImmutableList.of();
        }
        List<String> values = jedis.get().mget(result.getResult().toArray(new String[result.getResult().size()]));
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
    public Optional<Guideline> read(String lineItemId, String screenId, String sspId) {
        String key = getKey(lineItemId, screenId, sspId);
        return Optional.ofNullable(jedis.get().get(key)).map(data -> {
            try {
                return objectMapper.readValue(data, Guideline.class);
            } catch (JsonProcessingException e) {
                log.error("can't read Guideline from: {}", data, e);
                return null;
            }
        });
    }

    @Override
    public void write(Guideline guideline) throws Exception {
        log.debug("guideline would be written: {}", guideline);
        if (guideline.directives.isEmpty()) {
            throw new Exception("can't write guideline without directives");
        }

        List<Directive> invalidDirectives = guideline.directives.stream().filter(directive -> !directive.isValid()).collect(Collectors.toList());

        if (!invalidDirectives.isEmpty()) {
            log.error("invalid directives: {}", invalidDirectives);
            throw new Exception("can't write invalid directives");
        }

        Directive directive = guideline.directives.get(0);
        String key = getKey(directive.lineItemId, directive.screenId, directive.sspId);
        String value = objectMapper.writeValueAsString(guideline);
        jedis.get().set(key, value);
    }

    private static String getKey(String lineItemId, String screenId, String sspId) {
        return String.join("#", Guideline.class.getName(), lineItemId, screenId, sspId);
    }
}
