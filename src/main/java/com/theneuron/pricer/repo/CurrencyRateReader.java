package com.theneuron.pricer.repo;

import org.joda.time.DateTime;

import javax.money.CurrencyUnit;
import java.math.BigDecimal;
import java.util.Optional;

public interface CurrencyRateReader {
    Optional<BigDecimal> read(DateTime dateTime, String currencyPair);
    Optional<BigDecimal> read(DateTime dateTime, CurrencyUnit from, CurrencyUnit to);
}
