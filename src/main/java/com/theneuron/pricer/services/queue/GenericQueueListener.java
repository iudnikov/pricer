package com.theneuron.pricer.services.queue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theneuron.pricer.model.messages.SQSMessageWrapper;
import lombok.extern.slf4j.Slf4j;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import java.util.function.Consumer;

@Slf4j
public class GenericQueueListener<T> implements MessageListener {

    final TypeReference<T> typeReference;
    final ObjectMapper objectMapper;
    final Consumer<T> handler;
    final boolean isAcknowledgeEnabled;

    public GenericQueueListener(TypeReference<T> typeReference, ObjectMapper objectMapper, Consumer<T> handler, boolean isAcknowledgeEnabled) {
        this.typeReference = typeReference;
        this.objectMapper = objectMapper;
        this.handler = handler;
        this.isAcknowledgeEnabled = isAcknowledgeEnabled;
    }

    @Override
    public final void onMessage(Message message) {
        try {
            final String text = ((TextMessage) message).getText();
            log.info("handling message: {} ", text);
            final SQSMessageWrapper messageWrapper = objectMapper.readValue(text, SQSMessageWrapper.class);
            final String payload = messageWrapper.Message;
            T model = objectMapper.readValue(payload, typeReference);
            handler.accept(model);
            if (isAcknowledgeEnabled) {
                message.acknowledge();
            }
        } catch (Exception e) {
            log.error("can't handle message {}", message, e);
        }
    }
}
