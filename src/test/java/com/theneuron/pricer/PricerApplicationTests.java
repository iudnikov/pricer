package com.theneuron.pricer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.theneuron.pricer.config.AppConfig;
import com.theneuron.pricer.model.Directive;
import com.theneuron.pricer.model.messages.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.fail;


class PricerApplicationTests {

    private final ObjectMapper objectMapper = AppConfig.objectMapper();

    @Configuration
    public static class Config {

        @Bean
        public static SqsClient sqsClient() {
            return SqsClient.builder()
                    .region(Region.US_EAST_1)
                    .endpointOverride(URI.create("http://localhost:4566"))
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("dummy", "dummy")))
                    .build();
        }

    }

    SqsClient client = Config.sqsClient();

    @Test
    void test_flow() throws Exception {

        // send currency rate
        CurrencyRatesMessage currencyRatesMessage = CurrencyRatesMessage.builder()
                .date(DateTime.now())
                .rates(ImmutableList.of(CurrencyRatesMessage.CurrencyRate
                        .builder()
                        .currencyPair("JOD/USD")
                        .rate(BigDecimal.valueOf(1.41))
                        .build()))
                .build();

        client.sendMessage(SendMessageRequest.builder()
                .messageBody(sqsMessage(currencyRatesMessage))
                .queueUrl("http://localhost:4566/000000000000/currency-rate")
                .build());

        // send first bid response
        String one = RandomStringUtils.randomAlphanumeric(10);
        String lineItemId = "lineItem" + RandomStringUtils.randomNumeric(10);
        String screenId = "screenId" + RandomStringUtils.randomNumeric(10);
        BidResponseMessage bidResponseMessageOne = BidResponseMessage.builder()
                .type(MessageType.BID_RESPONSE.name)
                .time(Instant.now())
                .meta(BidResponseMessage.Meta.builder()
                        .requestId(one)
                        .bidId("bid")
                        .dealBidFloor(1.0)
                        .dealBidFloorCur("JOD")
                        .price(1.3)
                        .currency("JOD")
                        .lineItemId(lineItemId)
                        .lineItemPrice(2.0)
                        .lineItemCurrency("JOD")
                        .screenId(screenId)
                        .build())
                .build();

        client.sendMessage(SendMessageRequest.builder()
                .messageBody(sqsMessageArr(bidResponseMessageOne))
                .queueUrl("http://localhost:4566/000000000000/bid-response")
                .build());

        // send first loss notice
        LossNoticeMessage lossNoticeMessageOne = LossNoticeMessage.builder()
                .type(MessageType.LOSS_NOTICE.name)
                .meta(LossNoticeMessage.Meta.builder()
                        .requestId(one)
                        .lineItemId(lineItemId)
                        .screenId(screenId)
                        .build())
                .time(Instant.now())
                .build();

        client.sendMessage(SendMessageRequest.builder()
                .messageBody(sqsMessageArr(lossNoticeMessageOne))
                .queueUrl("http://localhost:4566/000000000000/loss")
                .build());

        // should create guideline and publish first directive

        Thread.sleep(1000);

        // Receive messages from the queue
        Map<String, Directive> directives = readDirectivesFromSQS();

        Directive directive = directives.get(one);

        // send second bid response

        String two = RandomStringUtils.randomAlphanumeric(10);

        BidResponseMessage bidResponseMessageTwo = BidResponseMessage.builder()
                .type(MessageType.BID_RESPONSE.name)
                .time(Instant.now())
                .meta(BidResponseMessage.Meta.builder()
                        .requestId(two)
                        .bidId("bid")
                        .dealBidFloor(1.0)
                        .dealBidFloorCur("JOD")
                        .price(1.37)
                        .currency("JOD")
                        .lineItemId(lineItemId)
                        .lineItemPrice(2.0)
                        .lineItemCurrency("JOD")
                        .screenId(screenId)
                        .directiveId(directive.directiveId)
                        .build())
                .build();

        client.sendMessage(SendMessageRequest.builder()
                .messageBody(sqsMessageArr(bidResponseMessageTwo))
                .queueUrl("http://localhost:4566/000000000000/bid-response")
                .build());


        // send second loss notice

        LossNoticeMessage lossNoticeMessageTwo = LossNoticeMessage.builder()
                .type(MessageType.LOSS_NOTICE.name)
                .meta(LossNoticeMessage.Meta.builder()
                        .requestId(two)
                        .lineItemId(lineItemId)
                        .screenId(screenId)
                        .build())
                .time(Instant.now())
                .build();

        client.sendMessage(SendMessageRequest.builder()
                .messageBody(sqsMessageArr(lossNoticeMessageTwo))
                .queueUrl("http://localhost:4566/000000000000/loss")
                .build());


    }

    private Map<String, Directive> readDirectivesFromSQS() throws InterruptedException {
        String directivesQueue = "http://localhost:4566/000000000000/directives";
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(directivesQueue)
                .maxNumberOfMessages(10)
                .build();

        Thread.sleep(1000);

        List<Message> messages = client.receiveMessage(receiveRequest).messages();

        Map<String, Directive> directives = new HashMap<>();

        messages.forEach(message -> {
            try {
                SQSMessageWrapper wrapper = objectMapper.readValue(message.body(), SQSMessageWrapper.class);
                Directive directive = objectMapper.readValue(wrapper.Message, Directive.class);
                directives.put(directive.requestId, directive);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        });

        client.purgeQueue(PurgeQueueRequest.builder()
                .queueUrl(directivesQueue)
                .build());

        return directives;
    }

    @Test
    void receive() throws Exception {

        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl("http://localhost:4566/000000000000/directives")
                .build();

        List<Message> messages = client.receiveMessage(receiveRequest).messages();

        messages.forEach(System.out::println);

    }

    private String sqsMessageArr(Object payload) throws Exception {
        Object[] arr = new Object[1];
        arr[0] = payload;
        return objectMapper.writeValueAsString(SQSMessageWrapper.builder().Message(objectMapper.writeValueAsString(arr)).build());
    }

    private String sqsMessage(Object payload) throws Exception {
        return objectMapper.writeValueAsString(SQSMessageWrapper.builder().Message(objectMapper.writeValueAsString(payload)).build());
    }

}
