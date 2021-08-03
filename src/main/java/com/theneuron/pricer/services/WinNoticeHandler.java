package com.theneuron.pricer.services;

import com.theneuron.pricer.model.messages.WinNoticeMessage;

public interface WinNoticeHandler {
    void onWinNoticeMessage(WinNoticeMessage winNoticeMessage) throws Exception;
}
