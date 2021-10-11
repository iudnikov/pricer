package com.theneuron.pricer.services.queue;

import com.amazon.sqs.javamessaging.message.SQSTextMessage;
import com.theneuron.pricer.config.AppConfigLocal;
import com.theneuron.pricer.repo.CurrencyRateWriter;
import com.theneuron.pricer.utils.DataLoader;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.mockito.Mockito;

import javax.jms.JMSException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class CurrencyRatesQueueListenerTest {

    @Test
    public void testHandler() throws JMSException {

        CurrencyRateWriter writer = Mockito.mock(CurrencyRateWriter.class);
        CurrencyRatesQueueListener listener = new CurrencyRatesQueueListener(AppConfigLocal.objectMapper(), writer, false);

        String messages = DataLoader.readCurrencyRatesSqsString();
        SQSTextMessage message = new SQSTextMessage(messages);

        listener.onMessage(message);

        DateTime expectedDateTime = new DateTime(2021, 8, 24, 0, 0).withZoneRetainFields(DateTimeZone.UTC);

        verify(writer, times(56)).write(eq(expectedDateTime), any());

    }

}