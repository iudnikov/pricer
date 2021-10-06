package com.theneuron.pricer.services;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.theneuron.pricer.model.Directive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Slf4j
@Component
public class DirectivePublisherSNSImpl implements DirectivePublisher {

    private final ObjectMapper objectMapper;
    private final String topicArn;
    private final SnsClient sns;

    public DirectivePublisherSNSImpl(
            ObjectMapper objectMapper,
            SnsClient sns,
            @Value("${sns.pricer-directives-topic-arn}") String topicArn
    ) {
        this.objectMapper = objectMapper;
        this.topicArn = topicArn;
        this.sns = sns;
    }

    @Override
    public void publish(Directive directive) throws Exception {
        String message = objectMapper.writeValueAsString(directive);
        log.debug("publishing message {} to topic {}", message, topicArn);
        sns.publish(PublishRequest.builder().topicArn(topicArn).message(message).build());
    }
}
