package com.theneuron.dynamicbidder.sqs;

import com.amazon.sqs.javamessaging.AmazonSQSMessagingClientWrapper;
import com.amazon.sqs.javamessaging.ProviderConfiguration;
import com.amazon.sqs.javamessaging.SQSConnection;
import com.amazon.sqs.javamessaging.SQSConnectionFactory;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.CreateQueueRequest;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;

public class SqsExample {

    public static void main(String[] args) throws Exception {

        final String queueName = "MyQueue1627805120323";
        final String accessKey = "AKIA4MVMF2E7UO5UQCMS";
        final String secretKey = "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";
        final String region = "us-east-1";
        SQSConnection connection = getSqsConnection(accessKey, secretKey, region);

        AmazonSQSMessagingClientWrapper client = connection.getWrappedAmazonSQSClient();

        // Create an Amazon SQS FIFO queue named MyQueue.fifo, if it doesn't already exist
        if (!client.queueExists(queueName)) {
            Map<String, String> attributes = new HashMap<>();
            //attributes.put("FifoQueue", "true");
            //attributes.put("ContentBasedDeduplication", "true");
            client.createQueue(new CreateQueueRequest().withQueueName(queueName).withAttributes(attributes));
        }

        // Create the nontransacted session with AUTO_ACKNOWLEDGE mode
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        // Create a queue identity and specify the queue name to the session
        Queue queue = session.createQueue(queueName);

        // Create a producer for the 'MyQueue'
        MessageProducer producer = session.createProducer(queue);

        for (int i = 0; i < 10; i++) {

            // Create the text message
            TextMessage message = session.createTextMessage("Hello World @ " + i);

            // Send the message
            producer.send(message);
            System.out.println("JMS Message " + message.getJMSMessageID());

            Thread.sleep(3000);

        }

    }

    public static SQSConnection getSqsConnection(String accessKey, String secretKey, String region) throws JMSException {
        SQSConnectionFactory connectionFactory = new SQSConnectionFactory(
                new ProviderConfiguration(),
                AmazonSQSClientBuilder.standard()
                        .withCredentials(
                                new AWSStaticCredentialsProvider(
                                        new BasicAWSCredentials(accessKey, secretKey)))
                        .withRegion(region)
                        .build()
        );

        SQSConnection connection = connectionFactory.createConnection();
        return connection;
    }
}
