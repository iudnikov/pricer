package com.theneuron.pricer.jedis;

import lombok.extern.slf4j.Slf4j;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.jupiter.api.Assertions.*;

@RunWith(SpringRunner.class)
@SpringBootTest()
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles("local")
@Slf4j
public class JedisStatefulClientTest {

    @Autowired
    JedisStatefulClient jedisStatefulClient;

    @Test
    public void getJedis() {
        jedisStatefulClient.get().set("foo", "bar");
        assertEquals("bar", jedisStatefulClient.get().get("foo"));
    }
}