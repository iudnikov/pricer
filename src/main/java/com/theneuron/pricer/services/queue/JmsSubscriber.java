package com.theneuron.pricer.services.queue;

import javax.jms.*;

public interface JmsSubscriber {
    void sub(Session session) throws JMSException;
    static void subscribe(Session session, String queueName, MessageListener listener) throws JMSException {
        Queue queue = session.createQueue(queueName);
        MessageConsumer consumer = session.createConsumer(queue);
        consumer.setMessageListener(listener);
    }
}
