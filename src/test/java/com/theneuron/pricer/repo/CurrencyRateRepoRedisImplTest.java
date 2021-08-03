package com.theneuron.pricer.repo;

import com.theneuron.pricer.model.messages.CurrencyRatesMessage;
import org.joda.time.DateTime;
import org.junit.Ignore;
import org.junit.Test;
import redis.clients.jedis.Jedis;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.Assert.*;

@Ignore
public class CurrencyRateRepoRedisImplTest {

    @Test
    public void testInverse() {
        String in = "FOO/BAR";
        assertEquals(CurrencyRateRepoRedisImpl.inverse(in), "BAR/FOO");
    }

    Jedis jedis = new Jedis("localhost", 6379);

    @Test
    public void testFlow() {

        CurrencyRateRepoRedisImpl repo = new CurrencyRateRepoRedisImpl(jedis);
        DateTime dateTime = new DateTime();

        repo.write(dateTime, CurrencyRatesMessage.CurrencyRate.builder()
                .currencyPair("FOO/BAR")
                .rate(BigDecimal.valueOf(2))
                .build());

        Optional<BigDecimal> result = repo.read(dateTime, "FOO/BAR");
        assertTrue(result.isPresent());
        assertEquals(BigDecimal.valueOf(2.0), result.get());

        result = repo.read(dateTime, "BAR/FOO");
        assertTrue(result.isPresent());
        assertEquals(BigDecimal.valueOf(0.5).setScale(6), result.get());

    }

}