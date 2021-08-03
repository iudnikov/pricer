package com.theneuron.pricer.services.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theneuron.pricer.config.AppConfig;
import com.theneuron.pricer.services.PricerService;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

@Slf4j
@Component
@NoArgsConstructor
public class TestQueueListener implements MessageListener {

    private final ObjectMapper objectMapper = AppConfig.objectMapper();

    @Override
    public void onMessage(Message message) {
        try {
            final String text = ((TextMessage) message).getText();

            log.info("handling message: {} ", text);
        } catch (Exception e) {
            log.error(String.format("can't handle message %s", message), e);
        }
    }
}
