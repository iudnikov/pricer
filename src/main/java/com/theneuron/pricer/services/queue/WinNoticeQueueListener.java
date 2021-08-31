package com.theneuron.pricer.services.queue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theneuron.pricer.model.messages.WinNoticeMessage;
import com.theneuron.pricer.services.WinNoticeHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class WinNoticeQueueListener extends GenericQueueListener<List<WinNoticeMessage>> {

    public WinNoticeQueueListener(
            ObjectMapper objectMapper,
            WinNoticeHandler winNoticeHandler,
            @Value("${app.is-acknowledge-enabled}") Boolean isAcknowledgeEnabled
    ) {
        super(
                new TypeReference<List<WinNoticeMessage>>() {},
                objectMapper,
                winNoticeMessages -> winNoticeMessages.forEach(winNoticeMessage -> {
                    try {
                        winNoticeHandler.onWinNoticeMessage(winNoticeMessage);
                    } catch (Exception e) {
                        log.error("can't handle: {}", winNoticeMessage, e);
                    }
                }),
                isAcknowledgeEnabled
        );
    }

}
