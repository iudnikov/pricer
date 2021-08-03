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

    private final SQSConnection sqsConnection;
    private final JmsSubscriber jmsSubscriber;

    public QueueSession(
            SQSConnection sqsConnection,
            JmsSubscriber jmsSubscriber) {
        this.sqsConnection = sqsConnection;
        this.jmsSubscriber = jmsSubscriber;
    }

    @PostConstruct
    public void postConstruct() throws Exception {

        log.info("starting SQS session");

        Session session = sqsConnection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

        jmsSubscriber.sub(session);
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
