package com.theneuron.pricer.services;

import com.theneuron.pricer.repo.CurrencyRateReader;
import org.javamoney.moneta.Money;
import org.joda.time.DateTime;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MoneyExchangerImplTest {

    @Test
    public void test_exchange() throws Exception {

        CurrencyRateReader reader = mock(CurrencyRateReader.class);
        DateTime now = DateTime.now();
        MoneyExchanger moneyExchanger = new MoneyExchangerImpl(reader, () -> now);
        Money from = Money.of(1, "USD");
        Money target = Money.of(2, "EUR");

        when(reader.read(now, from.getCurrency(), target.getCurrency())).thenReturn(Optional.of(BigDecimal.valueOf(2)));

        Money actual = moneyExchanger.exchange(from, target.getCurrency());
        assertEquals(target, actual);

    }

}