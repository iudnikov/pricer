package com.theneuron.pricer.model.messages;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import org.joda.time.DateTime;

import java.math.BigDecimal;
import java.util.List;

@Value
@Builder
public class CurrencyRatesMessage {

    DateTime date;
    List<CurrencyRate> rates;

    @Value
    @Builder
    public static class CurrencyRate {
        @JsonProperty("currency")
        String currencyPair;
        BigDecimal rate;
    }

}
