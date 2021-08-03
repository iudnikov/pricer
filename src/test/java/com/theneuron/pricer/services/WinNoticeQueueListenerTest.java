package com.theneuron.pricer.services;

import com.amazon.sqs.javamessaging.message.SQSTextMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theneuron.pricer.config.AppConfig;
import com.theneuron.pricer.services.queue.WinNoticeQueueListener;
import com.theneuron.pricer.utils.DataLoader;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class WinNoticeQueueListenerTest {

    final ObjectMapper objectMapper = AppConfig.objectMapper();

    @Test
    public void testWinNotice() throws Exception {

        SQSTextMessage sqsTextMessage = new SQSTextMessage(DataLoader.readWinNoticesSqsString());

        WinNoticeHandler handler = mock(WinNoticeHandler.class);
        WinNoticeQueueListener listener = new WinNoticeQueueListener(objectMapper, handler, true);

        listener.onMessage(sqsTextMessage);

        verify(handler, times(1)).onWinNoticeMessage(any());

    }

}