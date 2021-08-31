package com.theneuron.pricer.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theneuron.pricer.config.AppConfig;
import com.theneuron.pricer.services.queue.LossNoticeQueueListener;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class LossNoticeQueueListenerTest {

    final ObjectMapper objectMapper = AppConfig.objectMapper();

    @Test
    void onMessage() {

        LossNoticeHandler handler = mock(LossNoticeHandler.class);
        LossNoticeQueueListener listener = new LossNoticeQueueListener(objectMapper, handler, true);

        //listener.onMessage();
    }
}