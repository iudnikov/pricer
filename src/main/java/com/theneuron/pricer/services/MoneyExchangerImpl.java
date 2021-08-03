package com.theneuron.pricer.services;

import com.theneuron.pricer.repo.CurrencyRateReader;
import lombok.extern.slf4j.Slf4j;
import org.javamoney.moneta.Money;
import org.joda.time.DateTime;

import javax.money.CurrencyUnit;
import javax.money.MonetaryOperator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
public class MoneyExchangerImpl implements MoneyExchanger {

    private final CurrencyRateReader currencyRateReader;
    private final Supplier<DateTime> dateTimeSupplier;

    public MoneyExchangerImpl(CurrencyRateReader currencyRateReader, Supplier<DateTime> dateTimeSupplier) {
        this.currencyRateReader = currencyRateReader;
        this.dateTimeSupplier = dateTimeSupplier;
    }

    @Override
    public Money exchange(Money from, CurrencyUnit to) throws Exception {
        Optional<BigDecimal> rate = currencyRateReader.read(dateTimeSupplier.get(), from.getCurrency(), to);
        if (!rate.isPresent()) {
            throw new Exception("currency rate not found");
        }
        BigDecimal exchanged = rate.get().multiply(from.getNumber().numberValue(BigDecimal.class));
        BigDecimal rounded = exchanged.setScale(2, RoundingMode.HALF_DOWN);
        log.info("exchange rate: {} exchanged: {} rounded: {}", rate.get(), exchanged, rounded);
        return Money.of(rounded, to);
    }
}
