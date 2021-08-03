package com.theneuron.pricer.services.queue;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Session;

@Component
public class JmsSubscriberImpl implements JmsSubscriber {

    private final String bidResponseQueueName;
    private final String winNoticeQueueName;
    private final String lossNoticeQueueName;
    private final String currencyRatesQueueName;
    private final BidResponseQueueListener bidResponseQueueListener;
    private final WinNoticeQueueListener winNoticeQueueListener;
    private final LossNoticeQueueListener lossNoticeQueueListener;
    private final CurrencyRatesQueueListener currencyRatesQueueListener;

    public JmsSubscriberImpl(
            @Value("${queue.bid-response}") String bidResponseQueueName,
            @Value("${queue.win}") String winNoticeQueueName,
            @Value("${queue.loss}") String lossNoticeQueueName,
            @Value("${queue.currency-rate}") String currencyRatesQueueName,
            BidResponseQueueListener bidResponseQueueListener,
            WinNoticeQueueListener winNoticeQueueListener,
            LossNoticeQueueListener lossNoticeQueueListener,
            CurrencyRatesQueueListener currencyRatesQueueListener
    ) {
        this.bidResponseQueueName = bidResponseQueueName;
        this.winNoticeQueueName = winNoticeQueueName;
        this.lossNoticeQueueName = lossNoticeQueueName;
        this.bidResponseQueueListener = bidResponseQueueListener;
        this.winNoticeQueueListener = winNoticeQueueListener;
        this.lossNoticeQueueListener = lossNoticeQueueListener;
        this.currencyRatesQueueName = currencyRatesQueueName;
        this.currencyRatesQueueListener = currencyRatesQueueListener;
    }
    @Override
    public void sub(Session session) throws JMSException {
        JmsSubscriber.subscribe(session, bidResponseQueueName, bidResponseQueueListener);
        JmsSubscriber.subscribe(session, winNoticeQueueName, winNoticeQueueListener);
        JmsSubscriber.subscribe(session, lossNoticeQueueName, lossNoticeQueueListener);
        JmsSubscriber.subscribe(session, currencyRatesQueueName, currencyRatesQueueListener);
    }
}
