package com.theneuron.pricer.model.messages;

public enum MessageType {

    LOSS_NOTICE("loss_notice"),
    WIN_NOTICE("win_notice"),
    BID_RESPONSE("bid_response");

    public final String name;

    private MessageType(String name) {
        this.name = name;
    }
}
