package com.theneuron.pricer.services;

import com.amazon.sqs.javamessaging.message.SQSTextMessage;
import com.theneuron.pricer.config.AppConfig;
import com.theneuron.pricer.services.queue.BidResponseQueueListener;
import com.theneuron.pricer.utils.DataLoader;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class BidResponseQueueListenerTest {

    BidResponseHandler handler = mock(BidResponseHandler.class);
    BidResponseQueueListener listener = new BidResponseQueueListener(AppConfig.objectMapper(), handler, false);

    @Test
    void onMessage() throws Exception {

        String messages = DataLoader.readBidResponsesSqsString();
        SQSTextMessage message = new SQSTextMessage(messages);

        listener.onMessage(message);

        verify(handler, times(2)).onBidResponseMessage(any());

    }

}