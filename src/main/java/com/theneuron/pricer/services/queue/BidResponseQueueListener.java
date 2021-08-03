package com.theneuron.pricer.services.queue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theneuron.pricer.model.messages.BidResponseMessage;
import com.theneuron.pricer.model.messages.SQSMessageWrapper;
import com.theneuron.pricer.services.BidResponseHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import java.util.List;

@Slf4j
@Component
public final class BidResponseQueueListener extends GenericQueueListener<List<BidResponseMessage>> {

    public BidResponseQueueListener(
            ObjectMapper objectMapper,
            BidResponseHandler bidResponseHandler,
            @Value("${app.is-acknowledge-enabled}") Boolean isAcknowledgeEnabled
    ) {
        super(
                new TypeReference<List<BidResponseMessage>>() {},
                objectMapper,
                bidResponseMessages ->
                        bidResponseMessages.forEach(bidResponseMessage -> {
                            try {
                                bidResponseHandler.onBidResponseMessage(bidResponseMessage);
                            } catch (Exception e) {
                                log.error("can't handle: {}", bidResponseMessage, e);
                            }
                        }),
                isAcknowledgeEnabled);
    }
}
