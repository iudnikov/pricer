package com.theneuron.dynamicbidder.listener;

import com.amazon.sqs.javamessaging.SQSConnection;

import javax.jms.*;

import static com.theneuron.dynamicbidder.sqs.SqsExample.getSqsConnection;

class MyListener implements MessageListener {

    public static void main(String[] args) throws Exception {

        final String queueName = "MyQueue1627805120323";
        final String accessKey = "AKIA4MVMF2E7UO5UQCMS";
        final String secretKey = "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";
        final String region = "us-east-1";

        SQSConnection connection = getSqsConnection(accessKey, secretKey, region);

        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Queue queue = session.createQueue(queueName);

        // Create a consumer for the 'MyQueue'.
        MessageConsumer consumer = session.createConsumer(queue);

        // Instantiate and set the message listener for the consumer.
        consumer.setMessageListener(new MyListener());

        // Start receiving incoming messages.
        connection.start();

    }

    @Override
    public void onMessage(Message message) {
        try {
            // Cast the received message as TextMessage and print the text to screen.
            System.out.println("Received: " + ((TextMessage) message).getText());
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
