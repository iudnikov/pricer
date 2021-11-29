package com.theneuron.pricer.services;

import com.google.common.collect.ImmutableList;
import com.theneuron.pricer.config.AppConfig;
import com.theneuron.pricer.model.*;
import org.junit.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;

public class DirectivePublisherSNSImplTest {

    SnsClient snsClient = Mockito.mock(SnsClient.class);
    DirectivePublisher publisher = new DirectivePublisherSNSImpl(AppConfig.objectMapper(), snsClient, "test");
    Instant now = Instant.now();

    @Test
    public void publish_once_when_latest_is_loi() throws Exception {
        List<Guideline> guidelines = ImmutableList.of(
                Guideline.builder()
                        .directive(
                                Directive.builder()
                                        .type(DirectiveType.EXPLORATION)
                                        .timestamp(now)
                                        .directiveId(UUID.randomUUID())
                                        .requestId("one")
                                        .screenId("one")
                                        .lineItemId("one")
                                        .build())
                        .directive(
                                Directive.builder()
                                        .type(DirectiveType.EXPLOITATION)
                                        .timestamp(now.plus(1, ChronoUnit.SECONDS))
                                        .directiveId(UUID.randomUUID())
                                        .requestId("two")
                                        .screenId("two")
                                        .lineItemId("two")
                                        .build())
                        .status(GuidelineStatus.ACTIVE)
                        .guidelineType(GuidelineType.MAXIMISE_WINS)
                        .build()
        );

        publisher.publish(guidelines);
        Mockito.verify(snsClient).publish((PublishRequest) any());

    }

    @Test
    public void publish_twice_when_latest_is_lor() throws Exception {
        List<Guideline> guidelines = ImmutableList.of(
                Guideline.builder()
                        .directive(
                                Directive.builder()
                                        .type(DirectiveType.EXPLORATION)
                                        .timestamp(now)
                                        .directiveId(UUID.randomUUID())
                                        .requestId("one")
                                        .screenId("one")
                                        .lineItemId("one")
                                        .build())
                        .directive(
                                Directive.builder()
                                        .type(DirectiveType.EXPLOITATION)
                                        .timestamp(now.plus(1, ChronoUnit.SECONDS))
                                        .directiveId(UUID.randomUUID())
                                        .requestId("two")
                                        .screenId("two")
                                        .lineItemId("two")
                                        .build())
                        .directive(
                                Directive.builder()
                                        .type(DirectiveType.EXPLORATION)
                                        .timestamp(now.plus(2, ChronoUnit.SECONDS))
                                        .directiveId(UUID.randomUUID())
                                        .requestId("three")
                                        .screenId("three")
                                        .lineItemId("three")
                                        .build())
                        .status(GuidelineStatus.ACTIVE)
                        .guidelineType(GuidelineType.MAXIMISE_WINS)
                        .build()
        );

        publisher.publish(guidelines);
        Mockito.verify(snsClient, Mockito.times(2)).publish((PublishRequest) any());

    }
}