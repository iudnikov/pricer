package com.theneuron.pricer.services;

import com.theneuron.pricer.model.messages.LossNoticeMessage;

public interface LossNoticeHandler {
    void onLossNoticeMessage(LossNoticeMessage lossNoticeMessage) throws Exception;
}
