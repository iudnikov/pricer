package com.theneuron.pricer.services;

import com.theneuron.pricer.model.messages.BidResponseMessage;

public interface BidResponseHandler {
    void onBidResponseMessage(BidResponseMessage bidResponseMessage) throws Exception;
}
