package com.theneuron.pricer.develop;

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

public class SQSPublisher {

    public static void main(String[] args) throws Exception {

        final String queueName = "dooh-ssp-logging-bid-response-staging";
        final String accessKey = "AKIA4MVMF2E7UO5UQCMS";
        final String secretKey = "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";
        final String region = "us-east-1";

        SQSConnection connection = getSqsConnection(accessKey, secretKey, region);

        AmazonSQSMessagingClientWrapper client = connection.getWrappedAmazonSQSClient();

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

        /*for (int i = 0; i < 10; i++) {

            // Create the text message
            TextMessage message = session.createTextMessage("Hello World @ " + i);

            // Send the message
            producer.send(message);
            System.out.println("JMS Message " + message.getJMSMessageID());

            Thread.sleep(3000);

        }*/

        String message = "{\n" +
                "  \"Type\" : \"Notification\",\n" +
                "  \"MessageId\" : \"40675986-2b1c-5b23-973a-000352aad0be\",\n" +
                "  \"TopicArn\" : \"arn:aws:sns:us-east-1:851838357823:dooh-bid-request\",\n" +
                "  \"Message\" : \"[\\n  {\\\"type\\\":\\\"bid_response\\\",\\\"time\\\":1628511083.694000000,\\\"content\\\":{\\\"id\\\":\\\"63731fae-15cf-48bb-b221-bbf0f1a83240\\\",\\\"seatbid\\\":[{\\\"bid\\\":[{\\\"id\\\":\\\"3a5d4d98-e97d-462b-8f8b-370c0b348303\\\",\\\"impid\\\":\\\"1\\\",\\\"price\\\":0.1,\\\"nurl\\\":\\\"/endpoint/SSP/winnotice?bid_cost=1.0E-4&impression_id=1&line_item_id=id2_with_day_schedule_video&agency_id&auction_id=${AUCTION_ID}&imp_multiply=1.0&bid_id=3a5d4d98-e97d-462b-8f8b-370c0b348303&bid_cost_with_markup=1.0E-4&creative_id=id2_video_creative&advertiser_id&win_price=${AUCTION_PRICE}&instance_id&instance_markup=0.0&screen_id=MULTIPLE IMP SCREEN ID&bid_currency=USD&advertiser_markup=0.0&request_id=63731fae-15cf-48bb-b221-bbf0f1a83240&campaign_id=camp id\\\",\\\"burl\\\":\\\"/endpoint/SSP/billnotice?bid_cost=1.0E-4&impression_id=1&line_item_id=id2_with_day_schedule_video&agency_id&auction_id=${AUCTION_ID}&imp_multiply=1.0&bid_id=3a5d4d98-e97d-462b-8f8b-370c0b348303&bid_cost_with_markup=1.0E-4&creative_id=id2_video_creative&advertiser_id&win_price=${AUCTION_PRICE}&instance_id&instance_markup=0.0&screen_id=MULTIPLE IMP SCREEN ID&bid_currency=USD&advertiser_markup=0.0&request_id=63731fae-15cf-48bb-b221-bbf0f1a83240&campaign_id=camp id\\\",\\\"lurl\\\":\\\"/endpoint/SSP/lossnotice?bid_cost=1.0E-4&impression_id=1&line_item_id=id2_with_day_schedule_video&agency_id&auction_id=${AUCTION_ID}&imp_multiply=1.0&bid_id=3a5d4d98-e97d-462b-8f8b-370c0b348303&bid_cost_with_markup=1.0E-4&creative_id=id2_video_creative&loss_reason=${AUCTION_LOSS}&advertiser_id&instance_id&instance_markup=0.0&screen_id=MULTIPLE IMP SCREEN ID&bid_currency=USD&advertiser_markup=0.0&request_id=63731fae-15cf-48bb-b221-bbf0f1a83240&campaign_id=camp id\\\",\\\"adm\\\":null,\\\"adid\\\":null,\\\"adomain\\\":null,\\\"bundle\\\":null,\\\"iurl\\\":null,\\\"cid\\\":null,\\\"crid\\\":null,\\\"tactic\\\":null,\\\"cat\\\":null,\\\"attr\\\":null,\\\"api\\\":0,\\\"protocol\\\":0,\\\"qagmediarating\\\":0,\\\"language\\\":null,\\\"dealid\\\":\\\"DEAL ID\\\",\\\"w\\\":0,\\\"h\\\":0,\\\"wratio\\\":0,\\\"hratio\\\":0,\\\"exp\\\":0,\\\"ext\\\":{\\\"vast_url\\\":\\\"/endpoint/SSP/vast?bid_cost=1.0E-4&impression_id=1&line_item_id=id2_with_day_schedule_video&agency_id&imp_multiply=1.0&bid_id=3a5d4d98-e97d-462b-8f8b-370c0b348303&bid_cost_with_markup=1.0E-4&creative_id=id2_video_creative&advertiser_id&instance_id&instance_markup=0.0&screen_id=MULTIPLE IMP SCREEN ID&bid_currency=USD&advertiser_markup=0.0&request_id=63731fae-15cf-48bb-b221-bbf0f1a83240&campaign_id=camp id\\\"}},{\\\"id\\\":\\\"a73c4f85-4688-440a-8e19-8da3a16fbbba\\\",\\\"impid\\\":\\\"2\\\",\\\"price\\\":0.1,\\\"nurl\\\":\\\"/endpoint/SSP/winnotice?bid_cost=1.0E-4&impression_id=2&line_item_id=id1_image&agency_id&auction_id=${AUCTION_ID}&imp_multiply=1.0&bid_id=a73c4f85-4688-440a-8e19-8da3a16fbbba&bid_cost_with_markup=1.0E-4&creative_id=id1_image_creative&advertiser_id&win_price=${AUCTION_PRICE}&instance_id&instance_markup=0.0&screen_id=MULTIPLE IMP SCREEN ID&bid_currency=USD&advertiser_markup=0.0&request_id=63731fae-15cf-48bb-b221-bbf0f1a83240&campaign_id=camp id\\\",\\\"burl\\\":\\\"/endpoint/SSP/billnotice?bid_cost=1.0E-4&impression_id=2&line_item_id=id1_image&agency_id&auction_id=${AUCTION_ID}&imp_multiply=1.0&bid_id=a73c4f85-4688-440a-8e19-8da3a16fbbba&bid_cost_with_markup=1.0E-4&creative_id=id1_image_creative&advertiser_id&win_price=${AUCTION_PRICE}&instance_id&instance_markup=0.0&screen_id=MULTIPLE IMP SCREEN ID&bid_currency=USD&advertiser_markup=0.0&request_id=63731fae-15cf-48bb-b221-bbf0f1a83240&campaign_id=camp id\\\",\\\"lurl\\\":\\\"/endpoint/SSP/lossnotice?bid_cost=1.0E-4&impression_id=2&line_item_id=id1_image&agency_id&auction_id=${AUCTION_ID}&imp_multiply=1.0&bid_id=a73c4f85-4688-440a-8e19-8da3a16fbbba&bid_cost_with_markup=1.0E-4&creative_id=id1_image_creative&loss_reason=${AUCTION_LOSS}&advertiser_id&instance_id&instance_markup=0.0&screen_id=MULTIPLE IMP SCREEN ID&bid_currency=USD&advertiser_markup=0.0&request_id=63731fae-15cf-48bb-b221-bbf0f1a83240&campaign_id=camp id\\\",\\\"adm\\\":null,\\\"adid\\\":null,\\\"adomain\\\":null,\\\"bundle\\\":null,\\\"iurl\\\":\\\"/endpoint/SSP/imagenotice?bid_cost=1.0E-4&impression_id=2&line_item_id=id1_image&agency_id&imp_multiply=1.0&bid_id=a73c4f85-4688-440a-8e19-8da3a16fbbba&bid_cost_with_markup=1.0E-4&creative_id=id1_image_creative&advertiser_id&instance_id&instance_markup=0.0&screen_id=MULTIPLE IMP SCREEN ID&bid_currency=USD&advertiser_markup=0.0&request_id=63731fae-15cf-48bb-b221-bbf0f1a83240&campaign_id=camp id\\\",\\\"cid\\\":null,\\\"crid\\\":null,\\\"tactic\\\":null,\\\"cat\\\":null,\\\"attr\\\":null,\\\"api\\\":0,\\\"protocol\\\":0,\\\"qagmediarating\\\":0,\\\"language\\\":null,\\\"dealid\\\":\\\"DEAL ID\\\",\\\"w\\\":0,\\\"h\\\":0,\\\"wratio\\\":0,\\\"hratio\\\":0,\\\"exp\\\":0,\\\"ext\\\":null}],\\\"seat\\\":\\\"THE_NEURON\\\",\\\"group\\\":0,\\\"ext\\\":null}],\\\"bidid\\\":null,\\\"cur\\\":\\\"USD\\\",\\\"customdata\\\":null,\\\"nbr\\\":0,\\\"ext\\\":null},\\\"meta\\\":{\\\"ssp\\\":\\\"broadsign\\\",\\\"request_id\\\":\\\"63731fae-15cf-48bb-b221-bbf0f1a83240\\\",\\\"screen_id\\\":\\\"MULTIPLE IMP SCREEN ID\\\",\\\"campaign_ids\\\":[\\\"camp id\\\",\\\"camp id\\\"],\\\"line_item_ids\\\":[\\\"id2_with_day_schedule_video\\\",\\\"id1_image\\\"],\\\"creative_ids\\\":[\\\"id2_video_creative\\\",\\\"id1_image_creative\\\"],\\\"impression_ids\\\":[\\\"1\\\",\\\"2\\\"],\\\"bid_ids\\\":[\\\"3a5d4d98-e97d-462b-8f8b-370c0b348303\\\",\\\"a73c4f85-4688-440a-8e19-8da3a16fbbba\\\"],\\\"prices\\\":[0.1,0.1],\\\"deal_ids\\\":[\\\"DEAL ID\\\",\\\"DEAL ID\\\"],\\\"currencies\\\":[\\\"USD\\\",\\\"USD\\\"],\\\"deal_bid_floor\\\":[0.1,0.1],\\\"deal_bid_floor_cur\\\":[\\\"USD\\\",\\\"USD\\\"],\\\"imp_bid_floor\\\":[0.1,0.1],\\\"imp_bid_floor_cur\\\":[\\\"USD\\\",\\\"USD\\\"],\\\"imp_multiply\\\":[1.0,1.0]}},\\n  {\\\"type\\\":\\\"bid_response\\\",\\\"time\\\":1628511101.424000000,\\\"content\\\":{\\\"id\\\":\\\"63731fae-15cf-48bb-b221-bbf0f1a83240\\\",\\\"seatbid\\\":[{\\\"bid\\\":[{\\\"id\\\":\\\"1b849f13-bf7f-47b7-9bf5-added552ea47\\\",\\\"impid\\\":\\\"1\\\",\\\"price\\\":0.1,\\\"nurl\\\":\\\"/endpoint/SSP/winnotice?bid_cost=1.0E-4&impression_id=1&line_item_id=id2_with_day_schedule_video&agency_id&auction_id=${AUCTION_ID}&imp_multiply=1.0&bid_id=1b849f13-bf7f-47b7-9bf5-added552ea47&bid_cost_with_markup=1.0E-4&creative_id=id2_video_creative&advertiser_id&win_price=${AUCTION_PRICE}&instance_id&instance_markup=0.0&screen_id=MULTIPLE IMP SCREEN ID&bid_currency=USD&advertiser_markup=0.0&request_id=63731fae-15cf-48bb-b221-bbf0f1a83240&campaign_id=camp id\\\",\\\"burl\\\":\\\"/endpoint/SSP/billnotice?bid_cost=1.0E-4&impression_id=1&line_item_id=id2_with_day_schedule_video&agency_id&auction_id=${AUCTION_ID}&imp_multiply=1.0&bid_id=1b849f13-bf7f-47b7-9bf5-added552ea47&bid_cost_with_markup=1.0E-4&creative_id=id2_video_creative&advertiser_id&win_price=${AUCTION_PRICE}&instance_id&instance_markup=0.0&screen_id=MULTIPLE IMP SCREEN ID&bid_currency=USD&advertiser_markup=0.0&request_id=63731fae-15cf-48bb-b221-bbf0f1a83240&campaign_id=camp id\\\",\\\"lurl\\\":\\\"/endpoint/SSP/lossnotice?bid_cost=1.0E-4&impression_id=1&line_item_id=id2_with_day_schedule_video&agency_id&auction_id=${AUCTION_ID}&imp_multiply=1.0&bid_id=1b849f13-bf7f-47b7-9bf5-added552ea47&bid_cost_with_markup=1.0E-4&creative_id=id2_video_creative&loss_reason=${AUCTION_LOSS}&advertiser_id&instance_id&instance_markup=0.0&screen_id=MULTIPLE IMP SCREEN ID&bid_currency=USD&advertiser_markup=0.0&request_id=63731fae-15cf-48bb-b221-bbf0f1a83240&campaign_id=camp id\\\",\\\"adm\\\":null,\\\"adid\\\":null,\\\"adomain\\\":null,\\\"bundle\\\":null,\\\"iurl\\\":null,\\\"cid\\\":null,\\\"crid\\\":null,\\\"tactic\\\":null,\\\"cat\\\":null,\\\"attr\\\":null,\\\"api\\\":0,\\\"protocol\\\":0,\\\"qagmediarating\\\":0,\\\"language\\\":null,\\\"dealid\\\":\\\"DEAL ID\\\",\\\"w\\\":0,\\\"h\\\":0,\\\"wratio\\\":0,\\\"hratio\\\":0,\\\"exp\\\":0,\\\"ext\\\":{\\\"vast_url\\\":\\\"/endpoint/SSP/vast?bid_cost=1.0E-4&impression_id=1&line_item_id=id2_with_day_schedule_video&agency_id&imp_multiply=1.0&bid_id=1b849f13-bf7f-47b7-9bf5-added552ea47&bid_cost_with_markup=1.0E-4&creative_id=id2_video_creative&advertiser_id&instance_id&instance_markup=0.0&screen_id=MULTIPLE IMP SCREEN ID&bid_currency=USD&advertiser_markup=0.0&request_id=63731fae-15cf-48bb-b221-bbf0f1a83240&campaign_id=camp id\\\"}},{\\\"id\\\":\\\"74929a71-da70-48e9-a868-b8bfef6ecea4\\\",\\\"impid\\\":\\\"2\\\",\\\"price\\\":0.1,\\\"nurl\\\":\\\"/endpoint/SSP/winnotice?bid_cost=1.0E-4&impression_id=2&line_item_id=id1_image&agency_id&auction_id=${AUCTION_ID}&imp_multiply=1.0&bid_id=74929a71-da70-48e9-a868-b8bfef6ecea4&bid_cost_with_markup=1.0E-4&creative_id=id1_image_creative&advertiser_id&win_price=${AUCTION_PRICE}&instance_id&instance_markup=0.0&screen_id=MULTIPLE IMP SCREEN ID&bid_currency=USD&advertiser_markup=0.0&request_id=63731fae-15cf-48bb-b221-bbf0f1a83240&campaign_id=camp id\\\",\\\"burl\\\":\\\"/endpoint/SSP/billnotice?bid_cost=1.0E-4&impression_id=2&line_item_id=id1_image&agency_id&auction_id=${AUCTION_ID}&imp_multiply=1.0&bid_id=74929a71-da70-48e9-a868-b8bfef6ecea4&bid_cost_with_markup=1.0E-4&creative_id=id1_image_creative&advertiser_id&win_price=${AUCTION_PRICE}&instance_id&instance_markup=0.0&screen_id=MULTIPLE IMP SCREEN ID&bid_currency=USD&advertiser_markup=0.0&request_id=63731fae-15cf-48bb-b221-bbf0f1a83240&campaign_id=camp id\\\",\\\"lurl\\\":\\\"/endpoint/SSP/lossnotice?bid_cost=1.0E-4&impression_id=2&line_item_id=id1_image&agency_id&auction_id=${AUCTION_ID}&imp_multiply=1.0&bid_id=74929a71-da70-48e9-a868-b8bfef6ecea4&bid_cost_with_markup=1.0E-4&creative_id=id1_image_creative&loss_reason=${AUCTION_LOSS}&advertiser_id&instance_id&instance_markup=0.0&screen_id=MULTIPLE IMP SCREEN ID&bid_currency=USD&advertiser_markup=0.0&request_id=63731fae-15cf-48bb-b221-bbf0f1a83240&campaign_id=camp id\\\",\\\"adm\\\":null,\\\"adid\\\":null,\\\"adomain\\\":null,\\\"bundle\\\":null,\\\"iurl\\\":\\\"/endpoint/SSP/imagenotice?bid_cost=1.0E-4&impression_id=2&line_item_id=id1_image&agency_id&imp_multiply=1.0&bid_id=74929a71-da70-48e9-a868-b8bfef6ecea4&bid_cost_with_markup=1.0E-4&creative_id=id1_image_creative&advertiser_id&instance_id&instance_markup=0.0&screen_id=MULTIPLE IMP SCREEN ID&bid_currency=USD&advertiser_markup=0.0&request_id=63731fae-15cf-48bb-b221-bbf0f1a83240&campaign_id=camp id\\\",\\\"cid\\\":null,\\\"crid\\\":null,\\\"tactic\\\":null,\\\"cat\\\":null,\\\"attr\\\":null,\\\"api\\\":0,\\\"protocol\\\":0,\\\"qagmediarating\\\":0,\\\"language\\\":null,\\\"dealid\\\":\\\"DEAL ID\\\",\\\"w\\\":0,\\\"h\\\":0,\\\"wratio\\\":0,\\\"hratio\\\":0,\\\"exp\\\":0,\\\"ext\\\":null}],\\\"seat\\\":\\\"THE_NEURON\\\",\\\"group\\\":0,\\\"ext\\\":null}],\\\"bidid\\\":null,\\\"cur\\\":\\\"USD\\\",\\\"customdata\\\":null,\\\"nbr\\\":0,\\\"ext\\\":null},\\\"meta\\\":{\\\"ssp\\\":\\\"broadsign\\\",\\\"request_id\\\":\\\"63731fae-15cf-48bb-b221-bbf0f1a83240\\\",\\\"screen_id\\\":\\\"MULTIPLE IMP SCREEN ID\\\",\\\"campaign_ids\\\":[\\\"camp id\\\",\\\"camp id\\\"],\\\"line_item_ids\\\":[\\\"id2_with_day_schedule_video\\\",\\\"id1_image\\\"],\\\"creative_ids\\\":[\\\"id2_video_creative\\\",\\\"id1_image_creative\\\"],\\\"impression_ids\\\":[\\\"1\\\",\\\"2\\\"],\\\"bid_ids\\\":[\\\"1b849f13-bf7f-47b7-9bf5-added552ea47\\\",\\\"74929a71-da70-48e9-a868-b8bfef6ecea4\\\"],\\\"prices\\\":[0.1,0.1],\\\"deal_ids\\\":[\\\"DEAL ID\\\",\\\"DEAL ID\\\"],\\\"currencies\\\":[\\\"USD\\\",\\\"USD\\\"],\\\"deal_bid_floor\\\":[0.1,0.1],\\\"deal_bid_floor_cur\\\":[\\\"USD\\\",\\\"USD\\\"],\\\"imp_bid_floor\\\":[0.1,0.1],\\\"imp_bid_floor_cur\\\":[\\\"USD\\\",\\\"USD\\\"],\\\"imp_multiply\\\":[1.0,1.0]}}\\n]\",\n" +
                "  \"Timestamp\" : \"2021-08-05T05:30:30.475Z\",\n" +
                "  \"SignatureVersion\" : \"1\",\n" +
                "  \"Signature\" : \"jS8fXwAj67WfZfcVt+Upx6FEJMftHety5bRDtf3U9vcLfUQfW7Dbl3Efb987WOW+LL5JQ3OA5xB//vIQWa0FMgPOTHkPyKMraZxzzPgZuURyQCg1FfuFRHOw/Ror40jOgtVnv+3TadkCHlOf3VPVuLkxhmnjc/N4H8R5c3ce55tkFLac4/5uU49jQ6T5R/5gxhi0LrGxGQSlzjFGJD+PrG6PgB4peoIiqOrHDyWPrsivQNTcSLp+SJabU5xbWNd8HL7qEm5/c19nTspH/2XAiObZPhoTx//AB4b5dARomMhSAXIZXTa8XSAwvC6oEo2UxHVZFiBek4Y9mij+gKmlhA==\",\n" +
                "  \"SigningCertURL\" : \"https://sns.us-east-1.amazonaws.com/SimpleNotificationService-010a507c1833636cd94bdb98bd93083a.pem\",\n" +
                "  \"UnsubscribeURL\" : \"https://sns.us-east-1.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:us-east-1:851838357823:dooh-bid-request:2eb30c5f-456f-4af6-b930-7aa2ba6c6804\"\n" +
                "}";

        TextMessage textMessage = session.createTextMessage(message);

        // Send the message
        producer.send(textMessage);

        System.out.println("JMS Message " + textMessage.getJMSMessageID());

        Thread.sleep(3000);


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

        return connectionFactory.createConnection();
    }
}
