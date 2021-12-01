package com.theneuron.pricer.jedis;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Slf4j
public class JedisStatefulClient {

    private final JedisPool jedisPool;
    private final int databaseNumber;
    private Jedis usableJedis;

    public JedisStatefulClient(JedisPool jedisPool, int databaseNumber) {
        this.jedisPool = jedisPool;
        this.databaseNumber = databaseNumber;
    }

    public final Jedis getJedis() {
        try {
            usableJedis.get("test-key");
        } catch (Exception e) {
            log.warn("jedis would be attempted to connect", e);
            usableJedis = jedisPool.getResource();
            usableJedis.select(databaseNumber);
        }
        return usableJedis;
    }

}
