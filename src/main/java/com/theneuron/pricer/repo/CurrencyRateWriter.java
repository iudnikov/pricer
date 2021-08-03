package com.theneuron.pricer.repo;

import com.theneuron.pricer.model.messages.CurrencyRatesMessage;
import org.joda.time.DateTime;

import java.math.BigDecimal;

public interface CurrencyRateWriter {
    void write(DateTime dateTime, CurrencyRatesMessage.CurrencyRate currencyRate);
}
