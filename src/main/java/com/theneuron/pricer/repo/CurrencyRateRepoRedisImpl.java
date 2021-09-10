package com.theneuron.pricer.repo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theneuron.pricer.model.messages.CurrencyRatesMessage;
import org.joda.time.DateTime;
import redis.clients.jedis.Jedis;

import javax.money.CurrencyUnit;
import java.math.BigDecimal;
import java.util.Optional;

public class CurrencyRateRepoRedisImpl implements CurrencyRateReader, CurrencyRateWriter {

    private final Jedis jedis;

    public CurrencyRateRepoRedisImpl(Jedis jedis) {
        this.jedis = jedis;
    }

    @Override
    public Optional<BigDecimal> read(DateTime dateTime, String currencyPair) {
        Optional<BigDecimal> result = Optional.ofNullable(jedis.get(getKey(dateTime, currencyPair))).map(str -> BigDecimal.valueOf(Double.parseDouble(str)));
        if (result.isPresent()) {
            return result;
        }
        return Optional.ofNullable(jedis.get(getKey(dateTime, inverse(currencyPair))))
                .map(str -> BigDecimal.valueOf(Double.parseDouble(str)))
                .map(rate -> BigDecimal.valueOf(1.000000).setScale(6, BigDecimal.ROUND_CEILING).divide(rate, BigDecimal.ROUND_CEILING));
    }

    @Override
    public Optional<BigDecimal> read(DateTime dateTime, CurrencyUnit from, CurrencyUnit to) {
        return read(dateTime, String.join("/", from.getCurrencyCode(), to.getCurrencyCode()));
    }

    @Override
    public void write(DateTime dateTime, CurrencyRatesMessage.CurrencyRate currencyRate) {
        String pair = currencyRate.getCurrencyPair();
        BigDecimal rate = currencyRate.getRate();
        write(dateTime, pair, rate);
    }

    private void write(DateTime dateTime, String pair, BigDecimal rate) {
        String key = getKey(dateTime, pair);
        jedis.set(key, rate.toString());
    }

    private String getKey(DateTime dateTime, String pair) {
        return String.join("#", dateTime.toString("yyyy-MM-dd"), pair);
    }

    public static String inverse(String currency) {
        String[] split = currency.split("/");
        return String.join("/", split[1], split[0]);
    }
}