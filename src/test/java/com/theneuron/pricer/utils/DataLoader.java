package com.theneuron.pricer.utils;

import com.amazon.sqs.javamessaging.message.SQSTextMessage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theneuron.pricer.config.AppConfigLocal;
import com.theneuron.pricer.model.messages.BidResponseMessage;
import com.theneuron.pricer.model.messages.WinNoticeMessage;
import com.theneuron.pricer.model.messages.LossNoticeMessage;
import com.theneuron.pricer.model.messages.SQSMessageWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class DataLoader {

    @Nullable
    private static <T> T readAndUnmarshalSQS(String filename, TypeReference<T> typeReference) {
        try {
            File file = new File(filename);
            String actual = Files.lines(file.toPath()).collect(Collectors.joining());
            ObjectMapper objectMapper = AppConfigLocal.objectMapper();
            final SQSMessageWrapper messageWrapper = objectMapper.readValue(actual, SQSMessageWrapper.class);
            String message = messageWrapper.Message;
            if (message.contains("bid_response")) {
                return objectMapper.readValue(message, typeReference);
            }
            return null;
        } catch (Exception e) {
            log.error("can't read: ", e);
            return null;
        }
    }

    @Nullable
    private static <T> T readAndUnmarshal(String filename, TypeReference<T> typeReference) {
        try {
            File file = new File(filename);
            String message = Files.lines(file.toPath()).collect(Collectors.joining());
            ObjectMapper objectMapper = AppConfigLocal.objectMapper();
            if (message.contains("bid_response")) {
                return objectMapper.readValue(message, typeReference);
            }
            return null;
        } catch (Exception e) {
            log.error("can't read: ", e);
            return null;
        }
    }

    public static SQSTextMessage createSQSTextMessage(String payload) throws Exception {
        File file = new File("src/test/resources/sqs-message-template.json");
        String text = Files.lines(file.toPath()).collect(Collectors.joining());
        ObjectMapper objectMapper = AppConfigLocal.objectMapper();
        String replaced = text.replace("PAYLOAD", payload);
        return new SQSTextMessage(replaced);
    }

    @Nullable
    private static String read(String filename) {
        try {
            File file = new File(filename);
            return Files.lines(file.toPath()).collect(Collectors.joining());
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public static List<BidResponseMessage> readBidResponsesFromSQS() {
        return readAndUnmarshalSQS("src/test/resources/bid-response-sqs.json", new TypeReference<List<BidResponseMessage>>() {});
    }

    @Nullable
    public static BidResponseMessage readBidResponse() {
        return readAndUnmarshal("src/test/resources/bid-response.json", new TypeReference<BidResponseMessage>() {});
    }

    @Nullable
    public static String readBidResponsesSqsString() {
        return read("src/test/resources/bid-response-sqs.json");
    }

    @Nullable
    public static String readCurrencyRatesSqsString() {
        return read("src/test/resources/currency-rate-sqs.json");
    }

    @Nullable
    public static List<WinNoticeMessage> readWinNotices() {
        return readAndUnmarshal("src/test/resources/win-notice-sqs.json", new TypeReference<List<WinNoticeMessage>>() {});
    }

    @Nullable
    public static String readWinNoticesSqsString() {
        return read("src/test/resources/win-notice-sqs.json");
    }

    @Nullable
    public static List<LossNoticeMessage> readLossNotices() {
        return readAndUnmarshal("src/test/resources/win-notice-sqs.json", new TypeReference<List<LossNoticeMessage>>() {});
    }

    public static List<String> readMessages() throws Exception {
        File file = new File("src/test/resources/case_one.txt");
        return Files.lines(file.toPath()).collect(Collectors.toList());
    }

}
