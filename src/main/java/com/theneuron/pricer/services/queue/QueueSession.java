package com.theneuron.pricer.services.queue;

import com.amazon.sqs.javamessaging.SQSConnection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.jms.*;

@Slf4j
@Component
public class QueueSession {

    private final String bidResponseQueueName;
    private final String winNoticeQueueName;
    private final String lossNoticeQueueName;
    private final String currencyRatesQueueName;
    private final SQSConnection sqsConnection;
    private final BidResponseQueueListener bidResponseQueueListener;
    private final WinNoticeQueueListener winNoticeQueueListener;
    private final LossNoticeQueueListener lossNoticeQueueListener;
    private final CurrencyRatesQueueListener currencyRatesQueueListener;

    public QueueSession(
            @Value("${queue.bid-response}") String bidResponseQueueName,
            @Value("${queue.win}") String winNoticeQueueName,
            @Value("${queue.loss}") String lossNoticeQueueName,
            @Value("${queue.currency-rate}") String currencyRatesQueueName,
            SQSConnection sqsConnection,
            BidResponseQueueListener bidResponseQueueListener,
            WinNoticeQueueListener winNoticeQueueListener,
            LossNoticeQueueListener lossNoticeQueueListener,
            CurrencyRatesQueueListener currencyRatesQueueListener) {
        this.bidResponseQueueName = bidResponseQueueName;
        this.winNoticeQueueName = winNoticeQueueName;
        this.lossNoticeQueueName = lossNoticeQueueName;
        this.sqsConnection = sqsConnection;
        this.bidResponseQueueListener = bidResponseQueueListener;
        this.winNoticeQueueListener = winNoticeQueueListener;
        this.lossNoticeQueueListener = lossNoticeQueueListener;
        this.currencyRatesQueueName = currencyRatesQueueName;
        this.currencyRatesQueueListener = currencyRatesQueueListener;
    }

    @PostConstruct
    public void postConstruct() throws Exception {

        log.info("starting SQS session");

        Session session = sqsConnection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

        subscribe(session, bidResponseQueueName, bidResponseQueueListener);
        subscribe(session, winNoticeQueueName, winNoticeQueueListener);
        subscribe(session, lossNoticeQueueName, lossNoticeQueueListener);

        subscribe(session, currencyRatesQueueName, currencyRatesQueueListener);

        sqsConnection.start();

    }

    public static void subscribe(Session session, String queueName, MessageListener listener) throws JMSException {
        Queue queue = session.createQueue(queueName);
        MessageConsumer consumer = session.createConsumer(queue);
        consumer.setMessageListener(listener);
    }

    @PreDestroy
    public void preDestroy() throws JMSException {
        log.info("SQS connection would be closed");
        sqsConnection.close();
    }

}
