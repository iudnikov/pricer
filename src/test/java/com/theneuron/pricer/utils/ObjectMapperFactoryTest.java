package com.theneuron.pricer.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theneuron.pricer.config.AppConfigLocal;
import com.theneuron.pricer.model.messages.BidResponseMessage;
import com.theneuron.pricer.model.messages.SQSMessageWrapper;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ObjectMapperFactoryTest {

    String bidRequest = "{\n" +
            "  \"Type\" : \"Notification\",\n" +
            "  \"MessageId\" : \"40675986-2b1c-5b23-973a-000352aad0be\",\n" +
            "  \"TopicArn\" : \"arn:aws:sns:us-east-1:851838357823:dooh-bid-request\",\n" +
            "  \"Message\" : \"[{\\\"type\\\":\\\"bid_request\\\",\\\"time\\\":\\\"2021-08-05T05:30:29.939Z\\\",\\\"content\\\":{\\\"id\\\":\\\"74aba6fc-3476-41d3-8fd6-4baf9c4c1f21\\\",\\\"cur\\\":[\\\"EUR\\\"],\\\"imp\\\":[{\\\"id\\\":\\\"b593c992-b78a-42cb-9928-04a191224d75\\\",\\\"video\\\":{\\\"w\\\":1080,\\\"h\\\":1920,\\\"mimes\\\":[\\\"video/mp4\\\"],\\\"protocols\\\":[3,6]},\\\"pmp\\\":{\\\"deals\\\":[{\\\"id\\\":\\\"TEST_DEAL_NEURON\\\",\\\"bidfloor\\\":1.0,\\\"at\\\":1}],\\\"private_auction\\\":0},\\\"bidfloor\\\":1.0,\\\"bidfloorcur\\\":\\\"EUR\\\",\\\"ext\\\":{\\\"exchangepkey\\\":\\\"Sales-Center\\\"}},{\\\"id\\\":\\\"def5554c-1ef7-4c43-87a3-5663df311784\\\",\\\"banner\\\":{\\\"mimes\\\":[\\\"image/jpeg\\\",\\\"image/png\\\",\\\"text/html\\\"],\\\"w\\\":1080,\\\"h\\\":1920},\\\"pmp\\\":{\\\"deals\\\":[{\\\"id\\\":\\\"TEST_DEAL_NEURON\\\",\\\"bidfloor\\\":1.0,\\\"at\\\":1}],\\\"private_auction\\\":0},\\\"bidfloor\\\":1.0,\\\"bidfloorcur\\\":\\\"EUR\\\",\\\"ext\\\":{\\\"exchangepkey\\\":\\\"Sales-Center\\\"}}],\\\"app\\\":{\\\"publisher\\\":{\\\"id\\\":\\\"7e5d9d9e-3dd8-4293-add4-1057ee8b8e85\\\",\\\"name\\\":\\\"Dev_Room_testing\\\"}},\\\"site\\\":{\\\"id\\\":\\\"6b81447b-05b2-4182-ac46-68bc8207cf43\\\",\\\"name\\\":\\\"Sales-Center\\\",\\\"publisher\\\":{\\\"id\\\":\\\"1\\\",\\\"name\\\":\\\"CSDM\\\",\\\"ext\\\":{\\\"exchangepubid\\\":\\\"1\\\"}}},\\\"user\\\":{\\\"ext\\\":{\\\"impmultiplier\\\":1.0}},\\\"device\\\":{\\\"language\\\":\\\"NL\\\",\\\"ip\\\":\\\"10.20.0.2\\\",\\\"geo\\\":{\\\"country\\\":\\\"NLD\\\",\\\"region\\\":\\\"Amsterdam\\\",\\\"city\\\":\\\"Amsterdam\\\",\\\"type\\\":1,\\\"lat\\\":52.34024,\\\"lon\\\":4.84282,\\\"zip\\\":\\\"1059 CL\\\"},\\\"didsha1\\\":\\\"56feefdb33558aa7cbc6c4c86ac5e0f5f4a4e613\\\",\\\"ext\\\":{\\\"id\\\":\\\"6b81447b-05b2-4182-ac46-68bc8207cf43\\\",\\\"code\\\":\\\"Sales-Center\\\",\\\"name\\\":\\\"Sales-Center\\\",\\\"publisher\\\":{\\\"id\\\":\\\"1\\\",\\\"name\\\":\\\"CSDM\\\",\\\"ext\\\":{\\\"exchangepubid\\\":\\\"1\\\"}},\\\"network\\\":{\\\"id\\\":\\\"7e5d9d9e-3dd8-4293-add4-1057ee8b8e85\\\",\\\"name\\\":\\\"Dev_Room_testing\\\"}}},\\\"regs\\\":{\\\"ext\\\":{\\\"gdpr\\\":0}},\\\"ext\\\":{\\\"mabver\\\":\\\"1.4.0\\\",\\\"code_description\\\":\\\"CSDM Headquarters\\\"}},\\\"meta\\\":{\\\"ssp\\\":\\\"madx\\\",\\\"request_id\\\":\\\"74aba6fc-3476-41d3-8fd6-4baf9c4c1f21\\\",\\\"publisher_ids\\\":[\\\"1\\\",\\\"1\\\"],\\\"screen_ids\\\":[\\\"6b81447b-05b2-4182-ac46-68bc8207cf43\\\",\\\"6b81447b-05b2-4182-ac46-68bc8207cf43\\\"],\\\"img_imp_multipliers\\\":[1.0,1.0],\\\"video_imp_multipliers\\\":[1.0,1.0],\\\"video_imp_per_second_multipliers\\\":[0.0,0.0],\\\"impression_ids\\\":[\\\"b593c992-b78a-42cb-9928-04a191224d75\\\",\\\"def5554c-1ef7-4c43-87a3-5663df311784\\\"],\\\"impression_floors\\\":[1.0,1.0],\\\"impression_currencies\\\":[\\\"EUR\\\",\\\"EUR\\\"],\\\"display_times\\\":[\\\"2021-08-05T05:30:34Z\\\",\\\"2021-08-05T05:30:34Z\\\"],\\\"private_auctions\\\":[0,0],\\\"deal_ids\\\":[\\\"TEST_DEAL_NEURON\\\",\\\"TEST_DEAL_NEURON\\\"],\\\"deal_floors\\\":[1.0,1.0],\\\"deal_currencies\\\":[null,null],\\\"seat_ids\\\":[\\\"\\\",\\\"\\\"],\\\"deal_ats\\\":[1,1]}}]\",\n" +
            "  \"Timestamp\" : \"2021-08-05T05:30:30.475Z\",\n" +
            "  \"SignatureVersion\" : \"1\",\n" +
            "  \"Signature\" : \"jS8fXwAj67WfZfcVt+Upx6FEJMftHety5bRDtf3U9vcLfUQfW7Dbl3Efb987WOW+LL5JQ3OA5xB//vIQWa0FMgPOTHkPyKMraZxzzPgZuURyQCg1FfuFRHOw/Ror40jOgtVnv+3TadkCHlOf3VPVuLkxhmnjc/N4H8R5c3ce55tkFLac4/5uU49jQ6T5R/5gxhi0LrGxGQSlzjFGJD+PrG6PgB4peoIiqOrHDyWPrsivQNTcSLp+SJabU5xbWNd8HL7qEm5/c19nTspH/2XAiObZPhoTx//AB4b5dARomMhSAXIZXTa8XSAwvC6oEo2UxHVZFiBek4Y9mij+gKmlhA==\",\n" +
            "  \"SigningCertURL\" : \"https://sns.us-east-1.amazonaws.com/SimpleNotificationService-010a507c1833636cd94bdb98bd93083a.pem\",\n" +
            "  \"UnsubscribeURL\" : \"https://sns.us-east-1.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:us-east-1:851838357823:dooh-bid-request:2eb30c5f-456f-4af6-b930-7aa2ba6c6804\"\n" +
            "}";

    @Test
    public void testReadJson() throws Exception {

        File file = new File("src/test/resources/bid-response-sqs.json");

        String actual = Files.lines(file.toPath()).collect(Collectors.joining());

        ObjectMapper objectMapper = AppConfigLocal.objectMapper();

        final SQSMessageWrapper messageWrapper = objectMapper.readValue(actual, SQSMessageWrapper.class);
        String message = messageWrapper.Message;
        if (message.contains("bid_response")) {
            List<BidResponseMessage> messages = objectMapper.readValue(message, new TypeReference<List<BidResponseMessage>>() {});
            assertEquals(messages.size(), 2);
        }

    }

    @Test
    public void testDataLoader() throws Exception {
        assertEquals(Objects.requireNonNull(DataLoader.readBidResponsesFromSQS()).size(), 2);
    }

    @Test
    public void testBidResponseUnmarshal() {
        BidResponseMessage message = DataLoader.readBidResponse();
        assertNotNull(Objects.requireNonNull(message).meta.getRequestId());
    }

}