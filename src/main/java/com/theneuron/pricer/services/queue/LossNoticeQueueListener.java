package com.theneuron.pricer.services.queue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theneuron.pricer.model.messages.LossNoticeMessage;
import com.theneuron.pricer.services.LossNoticeHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class LossNoticeQueueListener extends GenericQueueListener<List<LossNoticeMessage>> {

    public LossNoticeQueueListener(
            ObjectMapper objectMapper,
            LossNoticeHandler lossNoticeHandler,
            @Value("${app.is-acknowledge-enabled}") Boolean isAcknowledgeEnabled
    ) {
        super(
                new TypeReference<List<LossNoticeMessage>>() {},
                objectMapper,
                lossNoticeMessages ->
                        lossNoticeMessages.forEach(lossNoticeMessage -> {
                            try {
                                lossNoticeHandler.onLossNoticeMessage(lossNoticeMessage);
                            } catch (Exception e) {
                                log.error("can't handle: {}", lossNoticeMessage, e);
                            }
                        })
                ,
                isAcknowledgeEnabled
        );
    }

}
