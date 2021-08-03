package com.theneuron.pricer.services.queue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theneuron.pricer.model.messages.CurrencyRatesMessage;
import com.theneuron.pricer.repo.CurrencyRateWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public final class CurrencyRatesQueueListener extends GenericQueueListener<CurrencyRatesMessage> {
    public CurrencyRatesQueueListener(
            ObjectMapper objectMapper,
            CurrencyRateWriter currencyRateWriter,
            @Value("${app.is-acknowledge-enabled}") Boolean isAcknowledgeEnabled
    ) {
        super(
                new TypeReference<CurrencyRatesMessage>() {},
                objectMapper,
                currencyRatesMessage ->
                        currencyRatesMessage.getRates().forEach(currencyRate ->
                            currencyRateWriter.write(currencyRatesMessage.getDate(), currencyRate)),
                isAcknowledgeEnabled);
    }
}
